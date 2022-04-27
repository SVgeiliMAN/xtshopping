package com.xtbd.provider.interceptor;

import com.xtbd.provider.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class SellerLoginInterceptor implements HandlerInterceptor {
    @Resource
    JwtUtil jwtUtil;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getMethod().equals("OPTIONS")){
            return true;
        }
        String token  =  request.getHeader("token");
        if (token==null||!jwtUtil.isTokenValid(token)||jwtUtil.getUserId(token)!=null){
            response.setStatus(403);
            return false;
        }
        String sellerId = jwtUtil.getSellerId(token);
        token = jwtUtil.getAccessToken(null, sellerId);
        response.setHeader("token",token);
        System.out.println("token"+token);
        System.out.println("seller拦截器被调用了！");
        return true;
    }
}