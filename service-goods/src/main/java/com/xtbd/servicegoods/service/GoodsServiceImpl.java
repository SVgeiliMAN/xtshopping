package com.xtbd.servicegoods.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xtbd.Entity.Goods;
import com.alibaba.dubbo.config.annotation.Service;
import com.xtbd.servicegoods.mapper.GoodsDao;
import com.xtbd.servicegoods.util.FastDFSClient;
import com.xtbd.servicegoods.util.RedisUtil;
import com.xtbd.servicegoods.util.ThreadFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import com.xtbd.service.GoodsService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;


import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;


@org.springframework.stereotype.Service
@Service(interfaceClass = GoodsService.class)
public class GoodsServiceImpl implements GoodsService {
    @Resource
    private GoodsDao goodsDao;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ElasticsearchRestTemplate esTemplate;
    @Resource
    private ThreadFactory threadFactory;
    @Resource
    private FastDFSClient fastDFSClient;
    @Value("${imgAddress}")
    private String imgAddress;
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
    public boolean updateGoods(Goods goods,String deletedUrls,Collection files) {
        String goodsId = goods.getGoodsId().toString();
        String imgUrlsString=getGoodInfo(goodsId).getImgUrls();
        try {
            if (deletedUrls!=null&&!"".equals(deletedUrls)){
                String[] deletedUrlArr = deletedUrls.split(",");
                threadFactory.execute(()->{
                    for (String url:deletedUrlArr) {
                        if ("".equals(url)||null==url){
                            continue;
                        }
                        fastDFSClient.deleteFile(url);
                    }
                });
                for (String url:deletedUrlArr) {
                    if ("".equals(url)||null==url){
                        continue;
                    }
                    imgUrlsString = imgUrlsString.replace(url+",","");
                }
            }
            if (imgUrlsString!=null&&!"".equals(imgUrlsString)){
                if (!imgUrlsString.endsWith(",")) {
                    imgUrlsString+=",";
                }
                if (imgUrlsString.startsWith(",")){
                    imgUrlsString="";
                }
            }
            Future future = threadFactory.submit(() -> {
                StringBuilder imgUrls = new StringBuilder();
                for (Object fileObject : files) {
                    MultipartFile file = (MultipartFile)fileObject;
                    try {
                        String path = fastDFSClient.uploadFile(file);
                        String imgUrl = imgAddress+ path;
                        imgUrls.append(imgUrl).append(",");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                return imgUrls;

            });
            String imgUrls2 = future.get().toString();
            if (imgUrlsString==null){
                goods.setImgUrls(imgUrls2);
            }else {
                goods.setImgUrls(imgUrlsString+imgUrls2);
            }

            boolean success = goodsDao.updateGoods(goods);
            if (!success){
                threadFactory.execute(()->{
                    String[] imgArr = imgUrls2.split(",");
                    for (String url:imgArr) {
                        fastDFSClient.deleteFile(url);
                    }
                });
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }



    @Override
    public boolean deleteGoods(String goodsId) {
        String imgUrls = getGoodInfo(goodsId).getImgUrls();
        if (imgUrls!=null&&!"".equals(imgUrls)){
            String[] imgArr = imgUrls.split(",");
            threadFactory.execute(()->{
                for (String url : imgArr) {
                    fastDFSClient.deleteFile(url);
                }
            });
        }
        return goodsDao.deleteGoods(goodsId);
    }

    @Override
    public boolean addGoods(Goods goods, Collection files) {
        StringBuffer imgUrls = new StringBuffer();
        try {

            Future future = threadFactory.submit(() -> {
                for (Object fileObject : files) {
                    try {
                        MultipartFile file = (MultipartFile) fileObject;
                        String path = fastDFSClient.uploadFile(file);
                        String imgUrl = imgAddress+ path;
                        imgUrls.append(imgUrl);
                        imgUrls.append(",");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                return imgUrls;

            });
            Object result= future.get();
            goods.setImgUrls(result.toString());
        }catch (Exception e){
            e.printStackTrace();
        }

        boolean success = goodsDao.addGoods(goods);
        if (!success){
            threadFactory.execute(()->{
                String[] imgArr = imgUrls.toString().split(",");
                for (String url:imgArr) {
                    fastDFSClient.deleteFile(url);
                }
            });
            return false;
        }
        return true;
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
