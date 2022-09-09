package com.xtbd.provider.consumer;
import com.alibaba.dubbo.config.annotation.Reference;
import com.xtbd.Entity.Goods;

import com.xtbd.service.GoodsService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @DubboReference(check = false)
    GoodsService goodsService;

    @GetMapping("/goodsInfo")
    public HashMap getGoodsInfo(HttpServletRequest request){
        String goodsId = request.getParameter("goodsId");
        if (null==goodsId||"".equals(goodsId)){
            return null;
        }
        HashMap<String, Object> hashMap = new HashMap<>();
        List<String> imageUrlList = goodsService.getImageUrlList(Integer.valueOf(goodsId));
        Goods goodInfo = goodsService.getGoodInfo(goodsId);
        hashMap.put("goodsInfo",goodInfo);
        hashMap.put("imageUrlList",imageUrlList);
        return hashMap;
    }

    @GetMapping("/getGoodsList")
    public List<HashMap<String,Object>> getGoodsList(HttpServletRequest request){
        String orderBy = request.getParameter("orderBy");
        String sort = request.getParameter("sort");
        return goodsService.getGoodsList(orderBy,sort);
    }
    @GetMapping("/search")
    public List searchGoods(HttpServletRequest request){
        String keyword = request.getParameter("keyword");
        return goodsService.searchGoods(keyword);
    }
}
