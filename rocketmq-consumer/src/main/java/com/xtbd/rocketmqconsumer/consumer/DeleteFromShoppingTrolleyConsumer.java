package com.xtbd.rocketmqconsumer.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbd.rocketmqconsumer.util.RedisUtil;
import com.xtbd.service.ShoppingTrolleyService;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.ArrayList;


@Component
@RocketMQMessageListener(topic = "deleteFromShoppingTrolley",consumerGroup = "shoppingTrolleyGroup")
public class DeleteFromShoppingTrolleyConsumer implements RocketMQListener<Message> {
    @Resource
    RedisUtil redisUtil;

    ObjectMapper objectMapper =new ObjectMapper();

    @Override
    public void onMessage(Message message) {
        try {
            byte[] body = message.getBody();
            String s = new String(body);
            ArrayList goodsIds = objectMapper.readValue(s, ArrayList.class);
            String userId = message.getProperty("userId");
            redisUtil.deleteFromShoppingTrolley(userId,goodsIds);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }
}
