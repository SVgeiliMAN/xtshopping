package com.xtbd.servicegoods.service;

import com.xtbd.Entity.Goods;
import com.alibaba.dubbo.config.annotation.Service;
import com.xtbd.servicegoods.mapper.GoodsDao;
import com.xtbd.servicegoods.util.RedisUtil;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import com.xtbd.service.GoodsService;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


@org.springframework.stereotype.Service
@Service(interfaceClass = GoodsService.class)
public class GoodsServiceImpl implements GoodsService {
    @Resource
    private GoodsDao goodsDao;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ElasticsearchRestTemplate esTemplate;

    @Override
    public boolean updateCountAndSales(String goodsId, Integer num) {

        return goodsDao.updateCountAndSales(goodsId,num);
    }

    @Override
    public Goods getGoodInfo(String goodsId) {
        try {
            Goods goods = redisUtil.getGoods(goodsId);
            if (null!=goods){
                return goods;
            }
            goods = goodsDao.getGoodsInfo(goodsId);
            redisUtil.setGoods(goods);
            return goods;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<HashMap<String , Object>> searchGoods(String keyword) {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        try {
            if (null==keyword||"".equals(keyword.trim())){
                return null;
            }

            MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery("goodsName", keyword);
            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withQuery(queryBuilder)
                    .withHighlightBuilder(new HighlightBuilder().field("goodsName").preTags("<span style=\"color:red\">").postTags("</span>"))
                    .build();
            SearchHits<HashMap> hashMapSearchHits = esTemplate.search(query, HashMap.class, IndexCoordinates.of("goods"));
            hashMapSearchHits.get().parallel().forEach(hashMapSearchHit->{
                HashMap<String ,Object> goods = hashMapSearchHit.getContent();
                List<String> goodsNameArr= hashMapSearchHit.getHighlightField("goodsName");
                goods.put("goodsName",goodsNameArr.get(0));
                result.add(goods);
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;

    }


    @Override
    public List<HashMap<String,Object>> getGoodsList(String orderBy,String sort) {
        ArrayList<HashMap<String,Object>> result = new ArrayList<>();
        try {

            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("onSale", "1"));
            NativeSearchQuery query = new NativeSearchQuery(queryBuilder);
            if ("asc".equals(sort)){
                query.addSort(Sort.by(orderBy).ascending());
            }
            if ("desc".equals(sort)){
                query.addSort(Sort.by(orderBy).descending());
            }
            SearchHits<HashMap> goodsSearchHits = esTemplate.search(query, HashMap.class, IndexCoordinates.of("goods"));
            for (SearchHit<HashMap> goodsSearchHit : goodsSearchHits) {
                HashMap<String, Object> goods = goodsSearchHit.getContent();
                result.add(goods);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }



    @Override
    public List<Goods> getSellGoodsList(String sellerId) {
        return goodsDao.getSellGoodsList(sellerId);
    }

    @Override
    public boolean updateGoods(Goods goods) {
        try {
            return goodsDao.updateGoods(goods);

        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }



    @Override
    public boolean deleteGoods(String goodsId) {

        return goodsDao.deleteGoods(goodsId);
    }

    @Override
    public boolean addGoods(Goods goods) {

        return goodsDao.addGoods(goods);
    }

    @Override
    public boolean changeOnSale(String goodsId, String onSale) {
        return goodsDao.changeOnSale(goodsId,onSale);
    }

    @Override
    public List<Goods> getGoodsInfoArr(List goodsIdArr) {

        return goodsDao.getGoodsInfoArr(goodsIdArr);
    }

}
