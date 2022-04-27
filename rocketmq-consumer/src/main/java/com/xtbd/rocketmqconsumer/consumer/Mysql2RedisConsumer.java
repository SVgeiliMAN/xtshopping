package com.xtbd.rocketmqconsumer.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbd.rocketmqconsumer.util.RedisUtil;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Component
@RocketMQMessageListener(topic = "example",consumerGroup = "Mysql2RedisConsumerGroup")
public class Mysql2RedisConsumer implements RocketMQListener<String> {

    @Resource
    private RedisUtil redisUtil;


    ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public void onMessage(String message) {
        try {
            HashMap<String,Object> map = objectMapper.readValue(message, HashMap.class);
            if(!"goods".equals(map.get("table").toString())){
                return;
            }
            String option = map.get("type").toString();
            //新增直接return
            if (option.equals(Option.INSERT)){
                return;
            }
            //更新和删除就去redis里面删除相对应的商品
            ArrayList<HashMap> dataArr = (ArrayList) map.get("data");
            if (dataArr==null){
                return;
            }
            for (HashMap hashMap : dataArr) {
                String goodsId = hashMap.get("goodsId").toString();
                if (redisUtil.hasGoods(goodsId)){
                    Boolean success = redisUtil.deleteGoods(goodsId);
                    if (!success){
                        throw new RuntimeException("删除redis商品失败！");
                    }
                }
            }


        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    static class Option{
        static final String UPDATE= "UPDATE";
        static final String DELETE= "DELETE";
        static final String INSERT= "INSERT";

    }
}
