package com.xtbd.serviceorder.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbd.Entity.Goods;
import com.xtbd.Entity.Order;
import com.xtbd.service.GoodsService;
import com.xtbd.service.OrderService;
import com.xtbd.serviceorder.mapper.OrderDao;
import com.xtbd.serviceorder.util.RedisUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@DubboService(timeout = 12000)
public class OrderServiceImpl implements OrderService {
    @Resource
    private OrderDao orderDao;

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RocketMQTemplate rocketmqTemplate;

    @Reference(interfaceClass = GoodsService.class,timeout = 12000,check = false)
    private GoodsService goodsService;


    ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public boolean deleteOrderFromSeller(String orderId) {
        Order order = orderDao.queryOrderInfo(orderId);
        if (order==null){

            return false;
        }
        //判断用户删除了订单没有，
        //如果用户已经删除了，那么userDeleted字段的值为true。这样的话，直接物理删除
        //如果用户没有删除，那么只修改sellerDeleted的字段的值为true
        if (null!=order.getUserDeleted()&&order.getUserDeleted()){
            return orderDao.deleteOrder(orderId);
        }

        return orderDao.deleteOrderOnlySeller(order);


    }

    @Override
    public boolean deleteOrderFromUser(String orderId) {
        Order order = orderDao.queryOrderInfo(orderId);
        if (order==null){

            return false;
        }

        if (null!=order.getSellerDeleted()&&order.getSellerDeleted()){
            return orderDao.deleteOrder(orderId);
        }

        return orderDao.deleteOrderOnlyUser(order);


    }

    @Override
    public List<Map<String, Object>> getOrderListFromSeller(String sellerId, String status) {
        if ("-1".equals(status)){
            status = null;
        }
        List<Map<String,Object>> orderList = orderDao.getOrderListFromSeller(sellerId,status);
        for (Map<String, Object> map : orderList) {
            String isPay = map.get("isPay").toString();
            status =(String) map.get("status");
            if ("1".equals(status)){
                map.put("status","进行中");
            }else if ("0".equals(status)){
                map.put("status","已取消");
            }else if ("2".equals(status)){
                map.put("status","已完成");
            }

            if ("1".equals(isPay)){
                map.put("isPay","已付款");

            }else if ("0".equals(isPay)){
                map.put("isPay","未付款");
                Long orderTTL = redisUtil.getOrderTTL(map.get("orderId").toString());
                if (orderTTL>0){
                    map.put("orderTTL",orderTTL.toString());
                    map.put("seconds",orderTTL.toString());
                }
            }

        }
        return orderList;
    }

    @Override
    public List<Map<String,Object>> getOrderListFromUser(String userId, String status) {
        if ("-1".equals(status)){
            status = null;
        }
        List<Map<String,Object>> orderList = orderDao.getOrderListFromUser(userId,status);
        for (Map<String,Object> map : orderList) {
            Boolean isPay =(Boolean) map.get("isPay");
            status =map.get("status").toString();
            if ("1".equals(status)){
                map.put("status","进行中");
            }else if ("0".equals(status)){
                map.put("status","已取消");
            }else if ("2".equals(status)){
                map.put("status","已完成");
            }

            if (isPay){
                map.put("isPay","已付款");
            }else {
                map.put("isPay","未付款");
                if (!"进行中".equals(map.get("status"))){
                    continue;
                }
                String orderId =String.valueOf(map.get("orderId"));
                Long orderTTL = redisUtil.getOrderTTL(orderId);
                if (orderTTL>0){
                    map.put("orderTTL",orderTTL.toString());
                }

            }
            map.put("seconds","0");
        }
        return orderList;
    }

    @Override
    public Map<String,Object> queryOrderInfo(String orderId) {
        try {
            Order orderInfo = orderDao.queryOrderInfo(orderId);
            Boolean isPay = orderInfo.getIsPay();
            String status = orderInfo.getStatus();

            String orderJson = objectMapper.writeValueAsString(orderId);
            Map<String,Object> map = objectMapper.readValue(orderJson, Map.class);
            //商品未付款，而且状态为进行中，添加订单过期时间（orderTTL）
            if (!isPay&&"1".equals(status)){
                Long orderTTL = redisUtil.getOrderTTL(orderId);
                map.put("orderTTL",orderTTL.toString());
            }

            map.put("seconds","0");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean cancelOrder(String orderId) {
        //这个地方必须用到分布式锁，锁订单编号
        RLock lock = redissonClient.getLock("orderLock:" + orderId);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            Order order = orderDao.queryOrderInfo(orderId);
            if (order==null){
                return false;
            }
            boolean cancelOrderSuccess= orderDao.cancelOrder(orderId);
            if (!cancelOrderSuccess){
                throw new RuntimeException("订单取消失败！");
            }
            //消息队列更改库存
            Message message = new Message();
            message.setBody(objectMapper.writeValueAsBytes(order));
            SendResult result = rocketmqTemplate.syncSend("goodsStockRecoverTopic", MessageBuilder.withPayload(message).build());
            if (SendStatus.SEND_OK!=result.getSendStatus()){
                throw new RuntimeException("库存更改消息发送失败！");
            }
            redisUtil.removeOrderTTL(orderId);
            lock.unlock();
        }catch (Exception e){
            lock.unlock();
            e.printStackTrace();
        }
        return true;
    }



    @Override
    public boolean submitOrder(Order order) {
        RLock lock =redissonClient.getLock(order.getUserId().toString());
        try {
            lock.lock(5,TimeUnit.SECONDS);
            Integer goodsId = order.getGoodsId();
            Integer num = order.getNum();

            //操作库存---------------------------------------
            boolean success = goodsService.updateCountAndSales(goodsId.toString(), num);
            if (!success){
                return false;
            }
            //生成订单--------------------------------
            //生成订单的时候就已经给卖家和买家绑定上了订单
            boolean generateSuccess = orderDao.generateOrder(order);
            //主动回滚---消息队列操作库存
            if (!generateSuccess){
                Message message = new Message();
                message.setBody(objectMapper.writeValueAsBytes(order));
                SendResult result = rocketmqTemplate.syncSend("goodsStockRecoverTopic", MessageBuilder.withPayload(message).build());
                if (SendStatus.SEND_OK!=result.getSendStatus()){
                    throw new RuntimeException("库存更改消息发送失败！");
                }
                return false;
            }

            //成功之后调用消息队列，将订单时间设置为三十分钟，三十分钟后调用删除订单的consumer
            Message message = new Message();
            Integer orderId = order.getOrderId();
            message.setKeys(orderId.toString());
            //messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
            message.setBody(objectMapper.writeValueAsBytes(orderId));
            SendResult result = rocketmqTemplate.syncSend("OrderCancelTopic", MessageBuilder.withPayload(message).build(),2000,16);
            if (SendStatus.SEND_OK!=result.getSendStatus()){
                throw new RuntimeException("消息发送失败！");
            }
            //利用redis计时
            redisUtil.setOrderTTL(orderId.toString());
            lock.unlock();
            return true;
        }catch (Exception e){
            lock.unlock();
            e.printStackTrace();

        }
        return false;
    }
    //购物车结算
    @Override
    public boolean submitOrders(String userId, List goodsIdArr,String address) {
        ArrayList<String> goodsIds = new ArrayList<>();
        try {
            List<Integer> nums= redisUtil.shoppingTrolleyGoodsNum(userId, goodsIdArr);
            List<Goods> goodsInfo = redisUtil.getGoods(goodsIdArr);
            Iterator<Goods> iterator = goodsInfo.iterator();
            while (iterator.hasNext()){
                Goods goods = iterator.next();
                if (goods==null){
                    iterator.remove();
                    continue;
                }
                Integer goodsId = goods.getGoodsId();
                goodsIdArr.remove(goodsId.toString());
            }
            //合并
            if (goodsIdArr.size()>0){
                goodsInfo.addAll( goodsService.getGoodsInfoArr(goodsIdArr));
            }else {
                return false;
            }
            //生成订单ArrayList，一会一块提交
            ArrayList<Order> orderArr = new ArrayList<>();
            for (int i =0;i<goodsInfo.size();i++) {
                Goods goods = goodsInfo.get(i);
                Order order = new Order();
                order.setUserId(Integer.valueOf(userId));
                order.setPrice(goods.getGoodsPrice());
                order.setSellerId(goods.getBelongTo());
                order.setGoodsId(goods.getGoodsId());
                order.setIsPay(false);
                order.setNum(nums.get(i));
                order.setAddress(address);
                orderArr.add(order);

            }

            //扣减库存
            Iterator<Order> iteratorOrder= orderArr.iterator();
            while (iteratorOrder.hasNext()){
                Order order = iteratorOrder.next();
                Integer num = order.getNum();
                Integer goodsId = order.getGoodsId();
                boolean updateSuccess = goodsService.updateCountAndSales(goodsId.toString(), num);
                if (!updateSuccess){
                    iteratorOrder.remove();
                }
            }

            //订单生成
            if (orderArr.size()==0){
                return false;
            }
            boolean generateSuccess = orderDao.generateOrderArr(orderArr);
            if (!generateSuccess){
                //所有的订单回退库存
                for (Order order : orderArr) {
                    Message message = new Message();
                    message.setBody(objectMapper.writeValueAsBytes(order));
                    SendResult result = rocketmqTemplate.syncSend("goodsStockRecoverTopic", MessageBuilder.withPayload(message).build());
                    if (result.getSendStatus()!=SendStatus.SEND_OK){
                        throw new RuntimeException("回退库存失败！订单编号："+order.getOrderId()+",商品编号："+order.getGoodsId()+",数量："+order.getNum());
                    }
                }
                return false;
            }

            iteratorOrder=orderArr.iterator();
            while (iteratorOrder.hasNext()) {
                Order order = iteratorOrder.next();
                //成功之后调用消息队列，将订单时间设置为三十分钟，三十分钟后调用删除订单的consumer
                Message message = new Message();
                Integer orderId = order.getOrderId();
                message.setKeys(orderId.toString());
                //messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                message.setBody(objectMapper.writeValueAsBytes(orderId));
                SendResult result = rocketmqTemplate.syncSend("deleteOrderTopic",MessageBuilder.withPayload(message).build(),2000,16);
                if (result.getSendStatus()!=SendStatus.SEND_OK){
                    throw new RuntimeException("取消订单消息发送失败！");
                }
                //利用redis计时
                redisUtil.setOrderTTL(orderId.toString());

                //删除购物车中的商品
                goodsIds.add(order.getGoodsId().toString());
            }
            Message message = new Message();
            message.setBody(objectMapper.writeValueAsBytes(goodsIds));
            message.putUserProperty("userId",userId);
            message.putUserProperty("goodsIds",objectMapper.writeValueAsString(goodsIds));
            rocketmqTemplate.sendOneWay("deleteFromShoppingTrolley",message);
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }


        return false;
    }

}
