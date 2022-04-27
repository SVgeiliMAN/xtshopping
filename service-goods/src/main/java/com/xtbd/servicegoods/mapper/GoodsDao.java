package com.xtbd.servicegoods.mapper;

import com.xtbd.Entity.Goods;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoodsDao {
    Goods getGoodsInfo(String goodsId);

    List<Goods> getGoodsList(String orderBy, String sort);

    boolean updateCountAndSales(String goodsId, Integer num);

    List<Goods> searchGoods(String keyword);

    List<Goods> getGoodsInfoArr(List<String> goodsIdArr);

    List<Goods> getSellGoodsList(String userId);

    boolean updateGoods(Goods goods);

    boolean deleteGoods(String goodsId);

    boolean addGoods(Goods goods);

    boolean changeOnSale(String goodsId, String onSale);
}
