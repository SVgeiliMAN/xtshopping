package com.xtbd.provider.interceptor;

import com.xtbd.provider.util.CookieUtil;
import com.xtbd.provider.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



@Component
public class UserLoginInterceptor implements HandlerInterceptor {
    @Resource
    JwtUtil jwtUtil;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getMethod().equals("OPTIONS")){
            return true;
        }
        String token  =  request.getHeader("token");
        System.out.println("token"+token);
        if (token==null||!jwtUtil.isTokenValid(token)||jwtUtil.getSellerId(token)!=null){
            response.setStatus(403);
            return false;
        }
        String userId = jwtUtil.getUserId(token);
        token = jwtUtil.getAccessToken(userId, null);
        response.setHeader("token",token);
        System.out.println("user拦截器被调用了！");
        return true;
    }
}
