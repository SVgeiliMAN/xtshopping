package com.xtbd.serviceshoppingtrolley.service;

import com.xtbd.Entity.Goods;
import com.xtbd.service.GoodsService;
import com.xtbd.service.ShoppingTrolleyService;
import com.xtbd.serviceshoppingtrolley.util.RedisUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@DubboService
@Service
public class ShoppingTrolleyServiceImpl implements ShoppingTrolleyService {
    @Resource
    RedisUtil redisUtil;

    @DubboReference(interfaceClass = GoodsService.class,check = false)
    GoodsService goodsService;
    @Override
    public List queryShoppingTrolley(String userId) {
        ArrayList<Map> resultList = new ArrayList<>();
        try {
            Map<String, Integer> shoppingTrolleyMap = redisUtil.getFromShoppingTrolley(userId);
            Set<String> goodsIds = shoppingTrolleyMap.keySet();;
            for (String goodsId:goodsIds) {
                Goods goodsInfo =goodsService.getGoodInfo(goodsId);
                if (goodsInfo==null){
                    continue;
                }
                HashMap<String , Object> jsonMap = new HashMap<>();
                jsonMap.put("goodsId",goodsId);
                jsonMap.put("imgUrls",goodsInfo.getImgUrls());
                jsonMap.put("belongTo",goodsInfo.getBelongTo().toString());
                jsonMap.put("goodsName",goodsInfo.getGoodsName());
                jsonMap.put("goodsPrice",goodsInfo.getGoodsPrice().toString());
                jsonMap.put("num",shoppingTrolleyMap.get(goodsId).toString());
                resultList.add(jsonMap);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;
    }

    @Override
    public boolean deleteFromShoppingTrolley(String userId, String goodsId) {
        return redisUtil.deleteFromShoppingTrolley(userId,goodsId);
    }

    @Override
    public boolean setToShoppingTrolley(String userId,String goodsId, Integer num) {
        redisUtil.setToShoppingTrolley(userId,goodsId,num);
        return true;
    }
}
