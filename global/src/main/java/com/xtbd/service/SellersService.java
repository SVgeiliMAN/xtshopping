package com.xtbd.service;

import com.xtbd.Entity.Seller;

public interface SellersService {

    boolean addSeller(Seller seller);
    Seller getSellerInfoByNickName(Seller seller);

    Seller getSellerInfoById(String sellerId);



    boolean login(Seller seller);



}
