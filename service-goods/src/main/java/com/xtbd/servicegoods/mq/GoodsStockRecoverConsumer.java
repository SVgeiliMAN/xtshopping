package com.xtbd.servicegoods.mq;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbd.Entity.Order;
import com.xtbd.service.GoodsService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RocketMQMessageListener(topic = "goodsStockRecoverTopic",consumerGroup = "goodsStockConsumer")
public class GoodsStockRecoverConsumer implements RocketMQListener<Message> {
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private GoodsService goodsService;
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redissonClient;
    @Override
    public void onMessage(Message message) {

        RLock lock = null;
        try {
            String messageId = message.getProperty("messageId");

//        分布式锁，锁住messageId,要是不锁住，那么会出现两个线程同时判断messageId存在的情况。
            lock = redissonClient.getLock("goodsStockRecoverTopic:"+messageId);
            lock.lock(2, TimeUnit.MINUTES);

//            如果这个message不存在，则表示已经被消费
            if (!redisTemplate.opsForSet().isMember("messageIdSet",messageId)){
                return;
            }
            Order order = objectMapper.readValue(message.getBody(), Order.class);
            Integer goodsId = order.getGoodsId();
            Integer num = order.getNum();
            boolean success = goodsService.updateCountAndSales(goodsId.toString(), -num);
            if (!success){
                throw new RuntimeException("恢复库存失败！goodsId:"+goodsId+",num:"+num);
            }
//            处理完删除redis里的消息id。
            redisTemplate.opsForSet().remove("messageIdSet",messageId);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
}
