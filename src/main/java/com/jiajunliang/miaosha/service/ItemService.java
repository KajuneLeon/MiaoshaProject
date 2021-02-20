package com.jiajunliang.miaosha.service;

import com.jiajunliang.miaosha.error.BusinessException;
import com.jiajunliang.miaosha.service.model.ItemModel;

import java.util.List;

/**
 * @project: MiaoshaProject
 * @program: ItemService
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-05 20:27
 **/
public interface ItemService {

    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //商品详情浏览
    ItemModel getItemById(Integer id);

    //库存扣减
    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;

    //商品销量增加
    void increaseSales(Integer itemId, Integer amount) throws BusinessException;

    //验证item及promo model缓存模型
    ItemModel getItemByIdInCache(Integer id);

}
