package com.xtbd.provider.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtil {
    public static String getUserId(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        String userId = "";
        for (Cookie cookie : cookies) {
            String cookieName = cookie.getName();
            if (cookieName.equals("userId")){
                userId=cookie.getValue();
            }

        }
        return userId;
    }

    public static Cookie setUserId(String value){
        Cookie userIdCookie = new Cookie("userId", value);
        userIdCookie.setMaxAge(60*60*3);
        return userIdCookie;
    }
    public static String getSellerId(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        String sellerId = "";
        for (Cookie cookie : cookies) {
            String cookieName = cookie.getName();
            if (cookieName.equals("sellerId")){
                sellerId=cookie.getValue();
            }

        }
        return sellerId;
    }
    public static Cookie setSellerId(String value){
        Cookie sellerIdCookie = new Cookie("sellerId", value);
        sellerIdCookie.setMaxAge(60*60*3);
        return sellerIdCookie;
    }


}
