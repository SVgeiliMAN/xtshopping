package com.xtbd.provider.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    public  String getAccessToken(String userId,String sellerId) {
        JwtBuilder jwtBuilder = Jwts.builder();

        long nowMillis = System.currentTimeMillis();
        // 7个官方Payload字段
        jwtBuilder.setId("1.0"); // 编号/版本
        jwtBuilder.setIssuer("谢堂村委会"); // 发行人
        jwtBuilder.setSubject("SUBJECT"); // 主题
        jwtBuilder.setAudience("AUDIENCE"); // 受众
        jwtBuilder.setIssuedAt(new Date(nowMillis)); // 签发时间
        jwtBuilder.setNotBefore(new Date(nowMillis)); // 生效时间
        jwtBuilder.setExpiration(new Date(nowMillis + (60 * 60 * 1000))); // 失效时间

        jwtBuilder.claim("userId", userId);
        jwtBuilder.claim("sellerId", sellerId);

        // 定义私钥
        String HS256KEY = "xtbdxtbd";
        // 签名算法
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        // 计算签名key值
        Key signingKey = new SecretKeySpec(Base64.decodeBase64(HS256KEY), signatureAlgorithm.getJcaName());

        // 进行签名
        jwtBuilder.signWith(signatureAlgorithm, signingKey);

        // 获取token字符串
        String tokenString = jwtBuilder.compact();

        return tokenString;
    }


    public boolean isTokenValid(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey("xtbdxtbd").parseClaimsJws(token.trim()).getBody();
            Date date = claims.getExpiration();
            return new Date().before(date);
        } catch (Exception e) {
            System.out.println("token不可用");
            return false;
        }
    }

    public String getUserId(String token){
        Claims claims = Jwts.parser().setSigningKey("xtbdxtbd").parseClaimsJws(token.trim()).getBody();
        return (String)claims.get("userId");
    }
    public String getUserId(HttpServletRequest request){
        try {
            String token = request.getHeader("token");
            Claims claims = Jwts.parser().setSigningKey("xtbdxtbd").parseClaimsJws(token.trim()).getBody();
            return (String)claims.get("userId");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getSellerId(String token){
        try {
            Claims claims = Jwts.parser().setSigningKey("xtbdxtbd").parseClaimsJws(token.trim()).getBody();
            return (String)claims.get("sellerId");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public String getSellerId(HttpServletRequest request){
        try {
            String token = request.getHeader("token");
            Claims claims = Jwts.parser().setSigningKey("xtbdxtbd").parseClaimsJws(token.trim()).getBody();
            return (String)claims.get("sellerId");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}
