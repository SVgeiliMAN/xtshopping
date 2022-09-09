package com.xtbd.serviceuser.service;
import com.xtbd.Entity.Seller;
import com.xtbd.service.SellersService;
import com.xtbd.serviceuser.mapper.SellersDao;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;


@Service
@DubboService(timeout = 12000)
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
