package com.xtbd.serviceorder.mapper;

import com.xtbd.Entity.Order;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDao {
    boolean deleteOrder(String orderId);

    List<Map<String,Object>> getOrderListFromUser(String userId, String status);

    List<Map<String,Object>> getOrderListFromSeller(String sellerId, String status);

    boolean generateOrder(Order order);
    boolean generateOrderArr(List<Order> orderArr);

    Order queryOrderInfo(String orderId);

    boolean cancelOrder(String orderId);

    boolean deleteOrderOnlySeller(Order order);
    boolean deleteOrderOnlyUser(Order order);
}
