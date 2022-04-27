package com.xtbd.service;

import java.util.List;

public interface ShoppingTrolleyService {

    List queryShoppingTrolley(String userId);

    boolean deleteFromShoppingTrolley(String userId, String goodsId);

    boolean setToShoppingTrolley(String userId, String goodsId, Integer valueOf);
}
