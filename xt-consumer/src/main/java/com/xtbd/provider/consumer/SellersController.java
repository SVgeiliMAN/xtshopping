package com.xtbd.provider.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xtbd.Entity.Goods;
import com.xtbd.Entity.Seller;
import com.xtbd.provider.util.FastDFSClient;
import com.xtbd.provider.util.JwtUtil;
import com.xtbd.provider.util.ThreadFactory;
import com.xtbd.service.GoodsService;
import com.xtbd.service.OrderService;
import com.xtbd.service.SellersService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/seller")
public class SellersController {

    @Value("${imgAddress}")
    private String imgAddress;
    @Reference(interfaceClass = SellersService.class,check = false)
    private SellersService sellersService;
    @Reference(interfaceClass = OrderService.class,check = false)
    private OrderService orderService;
    @Reference(interfaceClass = GoodsService.class,check = false)
    private GoodsService goodsService;
    @Resource
    private FastDFSClient fastDFSClient;
    @Resource
    private ThreadFactory threadFactory;
    @Resource
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Seller login(HttpServletRequest request, HttpServletResponse response, @RequestBody Seller seller){
        boolean correct = sellersService.login(seller);
        if(correct){
            Seller sellerInfo = sellersService.getSellerInfoByNickName(seller);
            String token = jwtUtil.getAccessToken(null, sellerInfo.getSellerId().toString());
            System.out.println("sellerToken"+token);
            response.setHeader("token",token);
            return sellerInfo;
        }
        return null;
    }
    @GetMapping("/getSellerInfo")
    public Seller getSellerInfo(HttpServletRequest request){
        String sellerId = jwtUtil.getSellerId(request);
        Seller sellerInfo = sellersService.getSellerInfoById(sellerId);
        return sellerInfo;
    }

    @GetMapping("/getSellerGoodsList")
    public List<Goods> getSellerGoodsList(HttpServletRequest request){
        String sellerId = jwtUtil.getSellerId(request);
        System.out.println("sellerId"+sellerId);
        List<Goods> sellerGoodsList = goodsService.getSellGoodsList(sellerId);
        return  sellerGoodsList;
    }
    @PostMapping("/uploadGoods")
    public boolean uploadGoods(HttpServletRequest request, HttpServletResponse response,MultipartHttpServletRequest multipartHttpServletRequest){
        try {
            Map<String, MultipartFile> fileMap = multipartHttpServletRequest.getFileMap();
            Collection<MultipartFile> files = fileMap.values();
            String goodsName = multipartHttpServletRequest.getParameter("goodsName");
            String goodsPrice = multipartHttpServletRequest.getParameter("goodsPrice");
            String count = multipartHttpServletRequest.getParameter("count");
            Goods goods = new Goods();
            goods.setCount(Integer.valueOf(count));
            goods.setGoodsPrice(Double.valueOf(goodsPrice));
            goods.setGoodsName(goodsName);
            goods.setBelongTo(Integer.valueOf(jwtUtil.getSellerId(request)));
            goods.setOnSale(true);
            StringBuffer imgUrls= new StringBuffer();

            Future future = threadFactory.submit(() -> {
                for (MultipartFile file : files) {
                    try {
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


            boolean success = goodsService.addGoods(goods);
            //插入数据库失败后删除已经上传的图片
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
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    @PostMapping("/editGoods")
    public boolean editGoods(MultipartHttpServletRequest multipartHttpServletRequest){
        try {
            Map<String, MultipartFile> fileMap = multipartHttpServletRequest.getFileMap();
            Collection<MultipartFile> files = fileMap.values();
            String goodsName = multipartHttpServletRequest.getParameter("goodsName");
            String goodsPrice = multipartHttpServletRequest.getParameter("goodsPrice");
            String count = multipartHttpServletRequest.getParameter("count");
            String goodsId = multipartHttpServletRequest.getParameter("goodsId");
            String imgUrlsString=goodsService.getGoodInfo(goodsId).getImgUrls();
            String deletedUrls = multipartHttpServletRequest.getParameter("deletedUrlArr");

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

            Goods goods = new Goods();
            goods.setCount(Integer.valueOf(count));
            goods.setGoodsPrice(Double.valueOf(goodsPrice));
            goods.setGoodsName(goodsName);
            goods.setOnSale(true);
            goods.setGoodsId(Integer.valueOf(goodsId));
            Future future = threadFactory.submit(() -> {
                StringBuilder imgUrls = new StringBuilder();
                for (MultipartFile file : files) {

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

            boolean success = goodsService.updateGoods(goods);
            if (!success){
                threadFactory.execute(()->{
                    String[] imgArr = imgUrls2.split(",");
                    for (String url:imgArr) {
                        fastDFSClient.deleteFile(url);
                    }
                });
                return false;
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    @GetMapping("/deleteGoods")
    public boolean deleteGoods(HttpServletRequest request){
        String goodsId = request.getParameter("goodsId");
        String imgUrls = goodsService.getGoodInfo(goodsId).getImgUrls();
        if (imgUrls!=null&&!"".equals(imgUrls)){
            String[] imgArr = imgUrls.split(",");
            threadFactory.execute(()->{
                for (String url : imgArr) {
                    fastDFSClient.deleteFile(url);
                }
            });
        }


        return goodsService.deleteGoods(goodsId);
    }




    @GetMapping("/getSellerOrderList")
    public List<Map<String,Object>> getSellerOrderList(HttpServletRequest request){
        String sellerId = jwtUtil.getSellerId(request);
        String status = request.getParameter("status");
        List<Map<String, Object>> orderListFromSeller = orderService.getOrderListFromSeller(sellerId, status);
        return  orderListFromSeller;
    }


    /**
     * 改变商品在售状态
     * @param request
     * @return
     */
    @GetMapping("/changeOnSale")
    public boolean changeOnSale(HttpServletRequest request){
        String goodsId = request.getParameter("goodsId");
        String onSale = request.getParameter("onSale");
        return  goodsService.changeOnSale(goodsId,onSale);

    }

    @GetMapping("/cancelOrder")
    public boolean cancelOrder(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String sellerId = jwtUtil.getSellerId(request);
        Map<String,Object> order= orderService.queryOrderInfo(orderId);

        if (!order.get("sellerId").toString().equals(sellerId)){
            return false;
        }
        if ("3".equals(order.get("status"))){
            return false;
        }
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/deleteOrder")
    public boolean deleteOrder(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        Map<String,Object> order= orderService.queryOrderInfo(orderId);

        //判断订单是否属于这个seller
        if (!order.get("sellerId").toString().equals(jwtUtil.getSellerId(request))){
            return false;
        }
        //判断用户删除了订单没有，
        //如果用户已经删除了，那么userDeleted字段的值为true。这样的话，直接物理删除
        //如果用户没有删除，那么只修改sellerDeleted的字段的值为true

        return orderService.deleteOrderFromSeller(orderId);
    }
}
