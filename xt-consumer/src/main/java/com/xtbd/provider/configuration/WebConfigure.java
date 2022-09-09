package com.xtbd.provider.configuration;

import com.xtbd.provider.interceptor.SellerLoginInterceptor;
import com.xtbd.provider.interceptor.UserLoginInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.annotation.Resource;

@Configuration
public class WebConfigure implements WebMvcConfigurer {

    @Resource
    UserLoginInterceptor userLoginInterceptor;
    @Resource
    SellerLoginInterceptor sellerLoginInterceptor;


    @Bean
    public WebMvcConfigurer webMvcConfigurer()
    {
        return new WebMvcConfigurer(){
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(userLoginInterceptor)
                        .addPathPatterns("/user/**")
                        .excludePathPatterns("/user/login","/user/register","/user/logout");
                registry.addInterceptor(sellerLoginInterceptor)
                        .addPathPatterns("/seller/**")
                        .excludePathPatterns("/seller/login","/seller/register","/seller/logout");
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        //是否发送Cookie
                        .allowCredentials(true)
                        //设置放行哪些原始域   SpringBoot2.4.4下低版本使用.allowedOrigins("*")
                        .allowedOriginPatterns("http://localhost:8080")
                        //放行哪些请求方式
//                        .allowedMethods(new String[]{"GET", "POST", "PUT", "DELETE"})
                        .allowedMethods("*") //或者放行全部
                        //放行哪些原始请求头部信息
                        .allowedHeaders("*")
                        //暴露哪些原始请求头部信息
                        .exposedHeaders("token")
                        .maxAge(3600)
                        ;
            }
        };




    }
    //websocket配置
    @Bean
    public ServerEndpointExporter serverEndpoint() {
        return new ServerEndpointExporter();
    }
    @Bean
    //文件上传解析器
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setDefaultEncoding("UTF-8");
        //resolveLazily属性启用是为了推迟文件解析，以在在UploadAction中捕获文件大小异常
        resolver.setResolveLazily(false);
        return resolver;
    }

}
