package com.jiajunliang.miaosha.controller.viewobject;

import lombok.Data;
import org.joda.time.DateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @project: MiaoshaProject
 * @program: ItemVO
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-05 21:17
 **/
@Data
public class ItemVO {
    private Integer id;
    //商品名
    private String title;
    //商品价格
    private BigDecimal price;
    //商品库存-分表
    private Integer stock;
    //商品描述
    private String description;
    //商品销量-分表
    private Integer sales;
    //商品描述图片url
    private String imgUrl;
    //商品是否在秒杀活动中，0-没有，1-未开始，2-进行中
    private Integer promoStatus;
    //秒杀活动价格
    private BigDecimal promoPrice;
    //秒杀活动id
    private Integer promoId;
    //秒杀活动开始时间
    private String startDate;

}
