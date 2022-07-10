package com.xtbd.serviceshoppingtrolley.util;

import com.xtbd.Entity.Goods;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Resource
    RedisTemplate redisTemplate;


    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public Boolean lock(String id){
        return redisTemplate.opsForValue().setIfAbsent("lock:" + id, "lock", 5, TimeUnit.SECONDS);
}
    public void unLock(String id){
        redisTemplate.delete("lock:" + id);
    }

    //订单过期时间的操作，订单过期时间为三十分钟---------------------------------------------------
    public void setOrderTTL(String orderId){
        redisTemplate.opsForValue().set("orderTTL:"+orderId,orderId,30, TimeUnit.MINUTES);
    }
    public Long getOrderTTL(String orderId){
        return redisTemplate.getExpire("orderTTL:"+orderId);

    }
    public Boolean removeOrderTTL(String orderId){
        return redisTemplate.delete("orderTTL:" + orderId);
    }
    //-------------------------------------------------------------------


    //商品信息的缓存，商品缓存时间为一天，只要商品被查看就会被缓存---------------------------------------------------------
    public void setGoods(Goods goods){
        redisTemplate.opsForValue().set("goodsInfo:"+goods.getGoodsId(),goods,1,TimeUnit.DAYS);
    }

    public Goods getGoods(String goodsId){
        return (Goods)redisTemplate.opsForValue().get("goodsInfo:"+goodsId);
    }

    public List<Goods> getGoods(List<String> goodsIds){
        ArrayList<String> goodsIdArr = new ArrayList<>();
        for (String goodsId : goodsIds) {
            goodsIdArr.add("goodsInfo:"+goodsId);
        }
        List<Goods> list = redisTemplate.opsForValue().multiGet(goodsIdArr);
        return  list;
    }

    public Boolean hasGoods(String goodsId){
        return redisTemplate.hasKey("goodsInfo:"+goodsId);
    }


    public Boolean deleteGoods(String goodsId){
        return redisTemplate.delete("goodsInfo:" + goodsId);
    }
    //---------------------------------------------------------------------



    public void setToShoppingTrolley(String userId,String goodsId,Integer num){
        redisTemplate.opsForHash().put("shoppingTrolley:"+userId,goodsId,num);
    }
    public Map<String,Integer> getFromShoppingTrolley(String userId){
       Map<String,Integer> shoppingTrolley=redisTemplate.opsForHash().entries("shoppingTrolley:"+userId);
       return shoppingTrolley;

    }
    public List<Integer> shoppingTrolleyGoodsNum(String userId, List goodsIdArr){
        List<Integer> list = redisTemplate.opsForHash().multiGet("shoppingTrolley:" + userId, goodsIdArr);
        return list;

    }


    public boolean deleteFromShoppingTrolley(String userId,String goodsId){
        Long delete = redisTemplate.opsForHash().delete("shoppingTrolley:" + userId, goodsId);
        if (delete>0){
            return true;
        }
        return false;
    }
    public boolean deleteFromShoppingTrolley(String userId,List goodsId,Class clazz){

        return false;
    }
    public boolean deleteFromShoppingTrolley(String userId,List goodsIds){
        Long delete = redisTemplate.opsForHash().delete("shoppingTrolley:" + userId, goodsIds.toArray());
        if (delete>0){
            return true;
        }
        return false;
    }
}
