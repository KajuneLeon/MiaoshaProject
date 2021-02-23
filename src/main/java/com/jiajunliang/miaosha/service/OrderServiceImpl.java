package com.jiajunliang.miaosha.service;

import com.jiajunliang.miaosha.dao.OrderDOMapper;
import com.jiajunliang.miaosha.dao.SequenceDOMapper;
import com.jiajunliang.miaosha.dataobject.OrderDO;
import com.jiajunliang.miaosha.dataobject.SequenceDO;
import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.error.EmBusinessError;
import com.jiajunliang.miaosha.service.model.ItemModel;
import com.jiajunliang.miaosha.service.model.OrderModel;
import com.jiajunliang.miaosha.service.model.UserModel;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @project: MiaoshaProject
 * @program: OrderServiceImpl
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-06 16:19
 **/
@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException {
        //校验下单状态：下单商品是否存在，用户是否合法，数量是否正确
//        ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(itemModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
//        UserModel userModel = userService.getUserById(userId);
        UserModel userModel =userService.getUserByIdInCache(userId);
        if(userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户信息不存在");
        }
        if(amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "数量信息不正确");
        }

        //校验秒杀活动信息
        if(promoId != null) {
            if(promoId.intValue() != itemModel.getPromoModel().getId()) {
                //校验对应活动是否存在该活动商品
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
            } else if(itemModel.getPromoModel().getStatus() != 2){
                //校验活动是否正在进行中
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动未开始");
            }
        }

        //落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if(!result) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }


        //订单入库
        OrderModel orderModel = new OrderModel();
        //生成交易流水号（订单号）

        //坑：在这里直接调用generateOrderNo不会使用新的事务
        //原因：https://blog.csdn.net/hepei120/article/details/78058468
        //事务首先调用的是AOP代理对象而不是目标对象，首先执行事务切面，事务切面内部通过
        //TransactionInterceptor环绕增强进行事务的增强，即进入目标对象的目标方法之前开启事务，退出目标方法时提交/回滚事务。
        //而目标对象内部的自我方法调用将无法实施切面中的增强
        //解决方法：不做内部调用，将两个方法分别写在不同的类里

        orderModel.setId(generateOrderNo());

        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if(promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
            orderModel.setPromoId(promoId);
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
            orderModel.setPromoId(0);
        }
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        OrderDO orderDO = this.convertFromOrderModel(orderModel);
        orderDOMapper.insert(orderDO);

        //商品销量增加
        itemService.increaseSales(itemId, amount);

//        //Spring Transaction提供方法：整个事务成功提交后，再执行某个方法
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//            //在最近的一个@Transactional注解事务被成功提交后执行afterCommit()
//            @Override
//            public void afterCommit() {
//                //异步更新库存
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
//                //但此时异步消息发送失败时，没有机会回滚redis库存 -> 事务型消息：See MqProducer
//                if(!mqResult) {
//                    itemService.increaseStock(itemId, amount);
//                   throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
//                }
//            }
//        });

        //返回前端
        return orderModel;
    }

    //不使用调用者的事务传播，而使用自己新的事务，确保序列号的全局唯一性
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected String generateOrderNo() {

        //订单号有16位
        StringBuilder stringBuilder = new StringBuilder();

        //前8位为时间信息：年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        //中间6位为自增序列
        //从数据库sequence_info表中获取（Note：SQL语句for update的使用）
        //获取当前sequence，除此之外需要考虑sequence最大值问题（可在表中提供init_value列，当超过最大值时，回复表中数据为初始值）
        Integer sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue()+sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for(int i = 0; i < 6 - sequenceStr.length(); i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        //最后2位为分库分表位，暂时写死
        stringBuilder.append("00");
        return stringBuilder.toString();
    }

    private OrderDO convertFromOrderModel(OrderModel orderModel) {
        if(orderModel == null) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        return orderDO;
    }
}
