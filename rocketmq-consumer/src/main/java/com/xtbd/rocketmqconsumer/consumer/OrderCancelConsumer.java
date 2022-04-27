package com.xtbd.rocketmqconsumer.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xtbd.service.OrderService;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RocketMQMessageListener(topic = "orderCancelTopic",consumerGroup = "DeleteOrderConsumerGroup")
public class OrderCancelConsumer implements RocketMQListener<Message> {

    @Reference(interfaceClass = OrderService.class,timeout = 12000)
    private OrderService orderService;


    @Override
    public void onMessage(Message message) {
        byte[] body = message.getBody();
        String orderId = new String(body);
        Map<String, Object> orderInfo = orderService.queryOrderInfo(orderId);
        if (orderInfo==null){
            return;

        }
        Boolean isPay =(Boolean)orderInfo.get("isPay");
        if (isPay){
            return;
        }
        orderService.cancelOrder(orderId);
    }
}
