package com.xtbd.serviceshoppingtrolley;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubboConfiguration
@SpringBootApplication
public class ServiceShoppingTrolleyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceShoppingTrolleyApplication.class, args);
	}

}
