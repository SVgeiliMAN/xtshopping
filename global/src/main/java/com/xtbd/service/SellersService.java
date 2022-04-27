package com.xtbd.service;

import com.xtbd.Entity.Seller;

public interface SellersService {

    Seller getSellerInfoByNickName(Seller seller);

    Seller getSellerInfoById(String sellerId);

    boolean login(Seller seller);



}
