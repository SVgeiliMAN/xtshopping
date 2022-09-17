package com.xtbd.servicegoods.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbd.Entity.Goods;
import com.xtbd.Entity.Image;
import com.xtbd.service.GoodsService;
import com.xtbd.servicegoods.mapper.GoodsDao;
import com.xtbd.servicegoods.util.FastDFSClient;
import com.xtbd.servicegoods.util.RedisUtil;
import com.xtbd.servicegoods.util.ThreadFactory;
import org.apache.dubbo.config.annotation.DubboService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Service
@DubboService(timeout = 12000,protocol ={"hessian","dubbo"} )
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

    ObjectMapper objectMapper = new ObjectMapper();
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
            if (null==goods){
                goods = goodsDao.getGoodsInfo(goodsId);
                redisUtil.setGoods(goods);
            }
            return goods;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public List<String>  getImageUrlList(Integer goodsId){
        return goodsDao.getGoodsImages(goodsId);
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
                Object goodsId = goods.get("goodsId");
                List<String> goodsImages = goodsDao.getGoodsImages((int)goodsId);
                goods.put("imageUrlList",goodsImages);
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
                String goodsId = goods.get("goodsId").toString();
                List<String> goodsImages = goodsDao.getGoodsImages(Integer.valueOf(goodsId));
                goods.put("imageUrlList",goodsImages);
                result.add(goods);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }



    @Override
    public List<Goods> getSellGoodsList(String sellerId) {
        return goodsDao.getSellGoodsList(Integer.valueOf(sellerId));
    }

    @Override
    public boolean updateGoods(Goods goods,String deletedUrls,List<Image> imageList) {
        String goodsId = goods.getGoodsId().toString();

        try {
            Future future = threadFactory.submit(() -> {
                List<String> newList = new ArrayList<>();
                for (Image image : imageList) {
                    Long size = image.getSize();
                    String extName = image.getExtName();
                    byte[] bytes = image.getBytes();
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                    try {
                        String path = fastDFSClient.uploadFile(byteArrayInputStream,size,extName);
                        String imgUrl = imgAddress+ path;
                        newList.add(imgUrl);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                return newList;

            });
            final String[] deletedUrlArr;
            if (deletedUrls!=null&&!"".equals(deletedUrls)){
                deletedUrlArr = deletedUrls.split(",");
                ArrayList<String> list = new ArrayList<>(deletedUrlArr.length);
                for (String s : deletedUrlArr) {
                    list.add(s);
                }
                goodsDao.deleteImages(Integer.valueOf(goodsId),list );
                threadFactory.execute(()->{
                    for (String url:list) {
                        fastDFSClient.deleteFile(url);
                    }
                });
            }
            List<String> newList = (List)future.get();
            if (null!=newList&&newList.size()!=0){
                goodsDao.addGoodsImages(Integer.valueOf(goodsId),newList);
            }
            boolean success = goodsDao.updateGoods(goods);
            return success;
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }



    @Override
    @Transactional
    public boolean deleteGoods(String goodsId) {
        List<String> goodsImages = goodsDao.getGoodsImages(Integer.valueOf(goodsId));
        goodsDao.deleteGoodsImages(Integer.valueOf(goodsId));
        goodsDao.deleteGoods(Integer.valueOf(goodsId));
        threadFactory.execute(()->{
                for (String url : goodsImages) {
                    fastDFSClient.deleteFile(url);
                }
        });

        return true;

    }

    @Override
    public boolean addGoods(Goods goods,List<Image> list) {
        final ArrayList<String> imgUrlList;
        try {
            boolean success = goodsDao.addGoods(goods);
            if (!success){
                return false;
            }
            Future future = threadFactory.submit(() -> {
                ArrayList<String> imgUrlArr = new ArrayList<>();
                for (Image image : list) {
                    try {
                        byte[] bytes = image.getBytes();
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                        Long size = image.getSize();
                        String extName = image.getExtName();
                        String path = fastDFSClient.uploadFile(byteArrayInputStream,size,extName);
                        imgUrlArr.add(imgAddress+path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return imgUrlArr;

            });

            imgUrlList = (ArrayList<String>) future.get();

            success = goodsDao.addGoodsImages(goods.getGoodsId(),imgUrlList);
            if (!success) {
                threadFactory.execute(() -> {
                    for (String url : imgUrlList) {
                        fastDFSClient.deleteFile(url);
                    }
                });
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public boolean changeOnSale(String goodsId, String onSale) {
        return goodsDao.changeOnSale(Integer.valueOf(goodsId),onSale);
    }

    @Override
    public List<Goods> getGoodsInfoArr(List goodsIdArr) {
        return goodsDao.getGoodsInfoArr(goodsIdArr);
    }

}
