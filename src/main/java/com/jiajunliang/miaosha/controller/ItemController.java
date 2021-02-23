package com.jiajunliang.miaosha.controller;

import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.jiajunliang.miaosha.controller.viewobject.ItemVO;
import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.response.CommonReturnType;
import com.jiajunliang.miaosha.service.CacheService;
import com.jiajunliang.miaosha.service.ItemService;
import com.jiajunliang.miaosha.service.PromoService;
import com.jiajunliang.miaosha.service.model.ItemModel;
import org.apache.ibatis.annotations.Param;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.Cache;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @project: MiaoshaProject
 * @program: ItemController
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-05 21:07
 **/
@Controller
@RequestMapping("/item")
@CrossOrigin(allowCredentials="true", allowedHeaders="*", originPatterns="*")
public class ItemController extends BaseController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;

    //创建商品的controller
    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORM})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name="title") String title,
                                       @RequestParam(name="description") String description,
                                       @RequestParam(name="price") BigDecimal price,
                                       @RequestParam(name="stock") Integer stock,
                                       @RequestParam(name="imgUrl") String imgUrl) throws BusinessException {
        //封装Service请求用来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn = itemService.createItem(itemModel);
        ItemVO itemVO = this.convertVOFromModel(itemModelForReturn);
        return CommonReturnType.create(itemVO);
    }

    //商品详情页浏览
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id){

        ItemModel itemModel = null;

        //先取本地缓存
        itemModel = (ItemModel) cacheService.getFromCommonCache("item_"+id);

        if(itemModel == null){
            //redis缓存接入
            //根据商品的id到redis内获取
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_"+id);

            if(itemModel == null) {
                //若redis内不存在对应的itemModel，则访问下游数据库service
                itemModel = itemService.getItemById(id);
                //保存itemModel到redis内
                redisTemplate.opsForValue().set("item_"+id, itemModel);
                redisTemplate.expire("item_"+id,10, TimeUnit.MINUTES);
            }
            //填充本地缓存
            cacheService.setCommonCache("item_"+id, itemModel);
        }

        ItemVO itemVO = this.convertVOFromModel(itemModel);
        return CommonReturnType.create(itemVO);
    }

    //商品列表页面浏览
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem(){
        List<ItemModel> itemModelList = itemService.listItem();

        //使用stream api将list内的ItemModel转换为ItemVO
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = this.convertVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }

    //发布活动
    @RequestMapping(value = "/publishpromo", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam(name = "id") Integer id) {
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
    }


    private ItemVO convertVOFromModel(ItemModel itemModel) {
        if(itemModel == null){
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        if(itemModel.getPromoModel() != null){
            //有正在进行或即将进行的秒杀活动
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }

}
