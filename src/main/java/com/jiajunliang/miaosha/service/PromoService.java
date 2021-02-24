package com.jiajunliang.miaosha.service;

import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.service.model.PromoModel;

public interface PromoService {
    //根据itemId获取即将或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);

    //活动发布（添加缓存库存）
    void publishPromo(Integer promoId);

    //生成秒杀令牌
    String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId);

}
