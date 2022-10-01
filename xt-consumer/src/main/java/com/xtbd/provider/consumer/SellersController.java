package com.xtbd.provider.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbd.Entity.Goods;
import com.xtbd.Entity.Seller;
import com.xtbd.Entity.Image;
import com.xtbd.provider.util.JwtUtil;
import com.xtbd.provider.util.ThreadFactory;
import com.xtbd.service.GoodsService;
import com.xtbd.service.OrderService;
import com.xtbd.service.SellersService;
import org.apache.commons.io.FilenameUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/seller")
public class SellersController{


    @DubboReference(interfaceClass = SellersService.class,check = false)
    private SellersService sellersService;
    @DubboReference(interfaceClass = OrderService.class,check = false)
    private OrderService orderService;
    @DubboReference(interfaceClass = GoodsService.class,check = false,protocol ="hessian")
    private GoodsService goodsService;

    @Resource
    private ThreadFactory threadFactory;
    @Resource
    private JwtUtil jwtUtil;

    ObjectMapper objectMapper = new ObjectMapper();
    @PostMapping("register")
    public boolean register(@RequestBody Seller seller){
        return sellersService.addSeller(seller);
    }
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
    @PostMapping("/logout")
    public void logout(HttpServletRequest request){
        String token = request.getHeader("token");
        jwtUtil.inValidToken(token);
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
            goods.setOnSale(true);
            goods.setBelongTo(Integer.valueOf(jwtUtil.getSellerId(request)));
            ArrayList<Image> list = new ArrayList<>();
            for (MultipartFile multipartFile : files) {
                Image image = new Image();
                byte[] bytes = multipartFile.getBytes();
                long size = multipartFile.getSize();
                String extName = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
                image.setExtName(extName);
                image.setBytes(bytes);
                image.setSize(size);
                list.add(image);
            }
            return goodsService.addGoods(goods,list);
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

            String deletedUrls = multipartHttpServletRequest.getParameter("deletedUrlArr");
            ArrayList<Image> list = new ArrayList<>();

            for (MultipartFile file : files) {
                long size = file.getSize();
                Image image = new Image();
                image.setBytes(file.getBytes());
                image.setGoodsId(Integer.valueOf(goodsId));
                String extName = FilenameUtils.getExtension(file.getOriginalFilename());
                image.setExtName(extName);
                image.setSize(size);
                list.add(image);
            }
            Goods goods = new Goods();
            goods.setGoodsName(goodsName);
            goods.setGoodsPrice(Double.valueOf(goodsPrice));
            goods.setCount(Integer.valueOf(count));
            goods.setGoodsId(Integer.valueOf(goodsId));
            goods.setOnSale(true);
            return goodsService.updateGoods(goods,deletedUrls,list);

        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    @GetMapping("/deleteGoods")
    public boolean deleteGoods(HttpServletRequest request){
        String goodsId = request.getParameter("goodsId");
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
