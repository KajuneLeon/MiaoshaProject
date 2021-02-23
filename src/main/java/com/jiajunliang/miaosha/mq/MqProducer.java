package com.jiajunliang.miaosha.mq;

import com.alibaba.fastjson.JSON;
import com.jiajunliang.miaosha.dao.StockLogDOMapper;
import com.jiajunliang.miaosha.dataobject.StockLogDO;
import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.stream.Location;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @project: MiaoshaProject
 * @program: MqProducer
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-21 00:00
 **/
@Component
public class MqProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @PostConstruct
    public void init() throws MQClientException {
        //做mq producer的初始化
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            //
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
                //return COMMIT_MESSAGE:可消费 ROLLBACK_MESSAGE:撤回消息 UNKNOW:为止，稍后询问
                //真正要做的事 - 创建订单
                Integer userId = (Integer) ((Map)arg).get("userId");
                Integer itemId = (Integer) ((Map)arg).get("itemId");
                Integer promoId = (Integer) ((Map)arg).get("promoId");
                Integer amount = (Integer) ((Map)arg).get("amount");
                String stockLogId = (String) ((Map)arg).get("stockLogId");
                try {
                    //真正调用createOrder
                    orderService.createOrder(userId, itemId, promoId, amount, stockLogId);
                    //存在问题：createOrder调用可能成功或失败，但下面的代码执行失败 / createOrder未执行完而定期调用了checkLocalTransaction
                    //解决：在定期回调的checkLocalTransaction(MessageExt msg)方法中检库存流水状态，并据此发送LocalTransactionState
                } catch (BusinessException e) {
                    e.printStackTrace();
                    //设置stockLog为回滚状态
                    StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            //当事务型消息为UNKNOW状态时，定期回调该方法
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                //根据是否扣减库存成功，判断要返回COMMIT/ROLLBACK/UNKNOW
                String jsonString = new String(msg.getBody());
                Map<String, Object> map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                String stockLogId = (String) map.get("stockLogId");
                StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                if(stockLogDO == null) {
                    return LocalTransactionState.UNKNOW;
                }
                if(stockLogDO.getStatus().intValue() == 2) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else if(stockLogDO.getStatus().intValue() == 1) {
                    return LocalTransactionState.UNKNOW;
                }
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        });
    }

    //事务型同步库存扣减消息
    //事务型-数据库事务提交成功时，对应消息必定发送成功，数据库事务回滚，消息必定不发送，数据库事务状态为止，消息pending（处理中）等待最后结果
    public boolean transactionAsyncReduceStock(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("stockLogId", stockLogId);

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("itemId", itemId);
        argsMap.put("amount", amount);
        argsMap.put("userId", userId);
        argsMap.put("promoId", promoId);
        argsMap.put("stockLogId", stockLogId);

        Message message = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        TransactionSendResult transactionSendResult = null;
        try {
            //发送一个事务型消息
            //二阶段提交：broker可接收到消息，但状态为不可消费（prepare），此时该消息不会被消费者看到
            //在prepare状态下，本地（代码本地）执行上面的executeLocalTransaction方法
            //即：1. 向消息队列中投递一个prepare状态的消息（由broker维护）；2.本地执行executeLocalTransaction方法
            transactionSendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }

        if(transactionSendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
            return false;
        } else if (transactionSendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE) {
            return true;
        } else {
            return false;
        }
    }


    //同步库存扣减消息 - 非事务性
    public boolean asyncReduceStock(Integer itemId, Integer amount) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        Message message = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        try {
            //不论如何，直接发送消息
            producer.send(message);
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
