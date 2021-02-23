package com.jiajunliang.miaosha.service;

import com.jiajunliang.miaosha.dao.PromoDOMapper;
import com.jiajunliang.miaosha.dataobject.PromoDO;
import com.jiajunliang.miaosha.service.model.ItemModel;
import com.jiajunliang.miaosha.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @project: MiaoshaProject
 * @program: PromoServiceImpl
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-06 20:46
 **/
@Service
public class PromoServiceImpl implements PromoService{

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = this.convertFromDataObject(promoDO);
        if(promoModel == null) {
            return null;
        }

        //判断当前时间是否秒杀活动即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(1);
        } else if(promoModel.getEndDate().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }

        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        //通过promoId获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if(promoDO.getItemId() == null || promoDO.getItemId().intValue() == 0) {
            return;
        }
        //注意：取出库存后存入缓存前这段时间，商品库存肯能减少，解决方案是：先下架商品，活动开始时再上架
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        //数据库库存同步到redis
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(), itemModel.getStock());
    }

    private PromoModel convertFromDataObject(PromoDO promoDO) {
        if(promoDO == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }


}
