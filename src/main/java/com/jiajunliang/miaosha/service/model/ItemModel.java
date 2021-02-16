package com.jiajunliang.miaosha.service.model;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @project: MiaoshaProject
 * @program: ItemModel
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-01-31 00:13
 **/
@Data
public class ItemModel implements Serializable {
    private Integer id;
    //商品名
    @NotBlank(message = "商品名称不能为空")
    private String title;
    //商品价格
    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格必须大于0")
    private BigDecimal price;
    //商品库存-分表
    @NotNull(message = "必须提供库存")
    private Integer stock;
    //商品描述
    @NotBlank(message = "商品描述信息不能为空")
    private String description;
    //商品销量-分表
    private Integer sales;
    //商品描述图片url
    @NotBlank(message = "图片信息不能为空")
    private String imgUrl;

    //使用聚合模型，如果promoModel不为空，则表明其具有还未结束的秒杀活动
    private PromoModel promoModel;
}
