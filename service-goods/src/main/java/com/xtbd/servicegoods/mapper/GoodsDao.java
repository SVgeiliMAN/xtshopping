package com.xtbd.servicegoods.mapper;

import com.xtbd.Entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodsDao {
    Goods getGoodsInfo(String goodsId);

    List<Goods> getGoodsList(String orderBy, String sort);

    boolean updateCountAndSales(String goodsId, Integer num);

    List<Goods> searchGoods(String keyword);

    List<Goods> getGoodsInfoArr(List<String> goodsIdArr);

    List<Goods> getSellGoodsList(Integer userId);

    boolean updateGoods(Goods goods);

    boolean deleteGoods(Integer goodsId);

    boolean addGoods(Goods goods);
    boolean addGoodsImages(Integer goodsId,@Param("imageUrlList") List<String> imgUrlList);

    boolean changeOnSale(Integer goodsId, String onSale);

    List<String> getGoodsImages(Integer goodsId);

    boolean deleteGoodsImages(Integer goodsId);
    boolean deleteImages(Integer goodsId,@Param("imageUrlList") List<String> imageUrlList);

}
