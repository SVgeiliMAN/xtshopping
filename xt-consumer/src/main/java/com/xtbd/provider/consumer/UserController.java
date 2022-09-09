package com.xtbd.provider.consumer;
import com.alibaba.dubbo.config.annotation.Reference;
import com.xtbd.Entity.Order;
import com.xtbd.Entity.Seller;
import com.xtbd.Entity.User;
import com.xtbd.provider.util.JwtUtil;
import com.xtbd.service.OrderService;
import com.xtbd.service.ShoppingTrolleyService;
import com.xtbd.service.UserService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/user")
public class UserController {

    @DubboReference(interfaceClass = UserService.class,timeout = 12000,check = false)
    private UserService userService;

    @DubboReference(interfaceClass = ShoppingTrolleyService.class,timeout = 12000,check = false)
    private ShoppingTrolleyService shoppingTrolleyService;

    @DubboReference(interfaceClass = OrderService.class,timeout = 12000,check = false)
    private OrderService orderService;

    @Resource
    private JwtUtil jwtUtil;

    @GetMapping("/userInfo")
    public User getUserInfo(HttpServletRequest request,HttpServletResponse response){
        String token = request.getHeader("token");
        if (null==token||"".equals(token)){
            response.setStatus(403);
        }else {
            String userId = jwtUtil.getUserId(token);
            return userService.getUserInfoById(userId);
        }

        return null;
    }

    @PostMapping("/register")
    public boolean addUser(@RequestBody User user){
        return userService.addUser(user);

    }

    @PostMapping("/login")
    public User login(HttpServletResponse response, @RequestBody User user){
        //判断用户名密码是否正确
        boolean correct = userService.login(user);
        if(correct){
            //查询用户信息
            User userInfo = userService.getUserInfoByNickName(user);
            //生成token
            String token = jwtUtil.getAccessToken(userInfo.getUserId().toString(),null);
            //把token响应头中
            response.setHeader("token",token);
            //返回用户信息
            return userInfo;
        }
        return null;
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request){
        String token = request.getHeader("token");
        jwtUtil.inValidToken(token);
    }


    @PostMapping("/submitOrder")
    public boolean submitOrder(HttpServletRequest request,@RequestBody Order order){
        String userId = jwtUtil.getUserId(request);
        order.setUserId(Integer.valueOf(userId));
        return orderService.submitOrder(order);
    }
    @PostMapping("/submitOrders")
    public boolean submitOrders(HttpServletRequest request,@RequestBody Map<String, Object> map){
        List goodsIdArr =(List) map.get("goodsIds");
        String userId = jwtUtil.getUserId(request);
        String address =(String) map.get("address");

        return orderService.submitOrders(userId,goodsIdArr,address);
    }


    @PostMapping("/addToShoppingTrolley")
    public boolean addToShoppingTrolley(HttpServletRequest request, @RequestBody Map<String,String> map){
        String userId = jwtUtil.getUserId(request);
        String goodsId = map.get("goodsId");
        Integer num= Integer.valueOf(map.get("num"));
        if (num<=0){
            return false;
        }
        return shoppingTrolleyService.setToShoppingTrolley(userId,goodsId,num);
    }

    @GetMapping("/deleteFromShoppingTrolley")
    public boolean deleteFromShoppingTrolley(HttpServletRequest request){
        String userId = jwtUtil.getUserId(request);
        String goodsId = request.getParameter("goodsId");
        return shoppingTrolleyService.deleteFromShoppingTrolley(userId, goodsId);

    }

    @GetMapping("/shoppingTrolley")
    public List queryShoppingTrolley(HttpServletRequest request){
        String userId = jwtUtil.getUserId(request);
        return shoppingTrolleyService.queryShoppingTrolley(userId);
    }


    @GetMapping("/getUserOrderList")
    public List queryOrderList(HttpServletRequest request){
        String userId = jwtUtil.getUserId(request);
        String status = request.getParameter("status");

        return orderService.getOrderListFromUser(userId,status);
    }

    @GetMapping("/cancelOrder")
    public boolean cancelOrder(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/deleteOrder")
    public  boolean  deleteOrder(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        return orderService.deleteOrderFromUser(orderId);
    }


}
