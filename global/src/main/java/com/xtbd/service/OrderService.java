package com.xtbd.service;

import com.xtbd.Entity.Order;

import java.util.List;
import java.util.Map;

public interface OrderService {
    boolean deleteOrderFromSeller(String orderId);

    boolean deleteOrderFromUser(String orderId);

    List<Map<String, Object>> getOrderListFromSeller(String userId, String status);
    List<Map<String ,Object>> getOrderListFromUser(String userId, String status);

    Map<String,Object> queryOrderInfo(String orderId);

    boolean cancelOrder(String orderId);


    boolean submitOrder(Order order);

    boolean submitOrders(String userId, List goodsIdArr, String address);


}
