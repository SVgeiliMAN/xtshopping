package com.xtbd.serviceuser.mapper;

import com.xtbd.Entity.Goods;
import com.xtbd.Entity.Seller;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SellersDao {


    boolean findSeller(Seller seller);

    Seller getSellerInfoByNickName(Seller seller);


    Seller getSellerInfoById(String sellerId);

}
