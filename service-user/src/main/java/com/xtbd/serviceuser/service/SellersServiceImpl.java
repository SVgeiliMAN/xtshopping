package com.xtbd.serviceuser.service;
import com.alibaba.dubbo.config.annotation.Service;
import com.xtbd.Entity.Seller;
import com.xtbd.service.SellersService;
import com.xtbd.serviceuser.mapper.SellersDao;


import javax.annotation.Resource;


@org.springframework.stereotype.Service
@Service(interfaceClass = SellersService.class)
public class SellersServiceImpl implements SellersService {

    @Resource
    SellersDao sellersDao;

    @Override
    public boolean login(Seller seller) {
        return sellersDao.findSeller(seller);
    }

    @Override
    public Seller getSellerInfoById(String sellerId) {
        return sellersDao.getSellerInfoById(sellerId);
    }

    @Override
    public Seller getSellerInfoByNickName(Seller seller) {
        Seller sellerInfoByNickName = sellersDao.getSellerInfoByNickName(seller);
        System.out.println(sellerInfoByNickName);
        return sellerInfoByNickName;
    }

}
