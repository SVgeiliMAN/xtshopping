package com.xtbd.rocketmqconsumer.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbd.Entity.Order;
import com.xtbd.service.GoodsService;
import jdk.nashorn.internal.ir.annotations.Reference;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@RocketMQMessageListener(topic = "goodsStockRecoverTopic",consumerGroup = "goodsStockConsumer")
public class GoodsStockRecoverConsumer implements RocketMQListener<Message> {
    @Resource
    ObjectMapper objectMapper;
    @Reference
    GoodsService goodsService;

    @Override
    public void onMessage(Message message) {
        try {
            Order order = objectMapper.readValue(message.getBody(), Order.class);
            Integer goodsId = order.getGoodsId();
            Integer num = order.getNum();
            boolean success = goodsService.updateCountAndSales(goodsId.toString(), -num);
            if (!success){
                throw new RuntimeException("恢复库存失败！goodsId:"+goodsId+",num:"+num);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
