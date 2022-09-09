package com.xtbd.serviceshoppingtrolley.configuration;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient getRedissonClient(){
        Config config = new Config();
        //单机模式  依次设置redis地址和密码
        config.useSingleServer()
                .setAddress("redis://124.221.229.69:6379")
                .setDatabase(15)
                .setPassword("xtbdxtbd");

        return Redisson.create(config);
    }
}
