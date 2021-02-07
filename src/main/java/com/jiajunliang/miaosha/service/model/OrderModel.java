package com.jiajunliang.miaosha.service.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @project: MiaoshaProject
 * @program: OrderModel
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-06 15:41
 **/
//用户下单的交易模型
@Data
public class OrderModel {

    private String id;
    //用户id
    private Integer userId;
    //购买商品id
    private Integer itemId;
    //秒杀id，若非空，则表示以秒杀商品方式下单
    private Integer promoId;
    //购买商品单价，若promoId非空，则表示秒杀商品价格
    private BigDecimal itemPrice;
    //购买数量
    private Integer amount;
    //购买金额，若promoId非空，则表示秒杀商品价格
    private BigDecimal orderPrice;

}
