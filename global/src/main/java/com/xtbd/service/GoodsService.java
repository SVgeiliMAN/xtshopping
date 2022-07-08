package com.xtbd.service;

import com.xtbd.Entity.Goods;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public interface GoodsService {


    boolean updateCountAndSales(String goodsId, Integer num);


    Goods getGoodInfo(String goodsId);

    List<HashMap<String,Object>> getGoodsList(String orderBy, String sort);

    List<HashMap<String,Object>> searchGoods(String keyword);

    boolean updateGoods(Goods goods,String deleteUrls,Collection files);

    boolean deleteGoods(String goodsId);

    boolean addGoods(Goods goods,Collection files);



    List<Goods> getSellGoodsList(String userId);

    boolean changeOnSale(String goodsId, String onSale);

    List<Goods> getGoodsInfoArr(List goodsIdArr);
}
