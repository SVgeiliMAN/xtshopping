package com.xtbd.servicegoods.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Component
@RocketMQMessageListener(topic = "example",consumerGroup = "Mysql2ESConsumerGroup",consumeMode = ConsumeMode.ORDERLY)
public class Mysql2ESConsumer implements RocketMQListener<String> {

    @Resource
    ElasticsearchRestTemplate esTemplate;
    ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public void onMessage(String message) {
        try {
            HashMap<String,Object> map = objectMapper.readValue(message, HashMap.class);
            if(!"goods".equals(map.get("table").toString())){
                return;
            }
            String option = map.get("type").toString();
            //新增添加到ES中
            if (option.equals(Option.INSERT)){
                ArrayList<HashMap> dataArr = (ArrayList) map.get("data");
                for (HashMap hashMap : dataArr) {
                    String goodsId = hashMap.get("goodsId").toString();
                    hashMap.put("sales",Integer.valueOf(hashMap.get("sales").toString()));
                    hashMap.put("goodsId",Integer.valueOf(goodsId));
                    IndexQuery indexQuery = new IndexQueryBuilder().withId(goodsId).withObject(hashMap).build();
                    String id = esTemplate.index(indexQuery, IndexCoordinates.of("goods"));
                    System.out.println("ES新增"+id);
                }
            }
            if (option.equals(Option.DELETE)){
                ArrayList<HashMap> dataArr = (ArrayList) map.get("data");
                for (HashMap hashMap : dataArr) {
                    String goodsId = hashMap.get("goodsId").toString();
                    goodsId = esTemplate.delete(goodsId, IndexCoordinates.of("goods"));
                    System.out.println("ES删除"+goodsId);
                }
            }
            if (option.equals(Option.UPDATE)){
                ArrayList<HashMap> dataArr = (ArrayList) map.get("data");
                //这里可以根据更新日期做幂等处理。
                for (HashMap hashMap : dataArr) {
                    String goodsId = hashMap.get("goodsId").toString();
                    Document document = Document.create().fromJson(objectMapper.writeValueAsString(hashMap));
                    UpdateResponse response= esTemplate.update(UpdateQuery.builder(goodsId).withDocument(document).withDocAsUpsert(true).build(), IndexCoordinates.of("goods"));
                    System.out.println(response.getResult());
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