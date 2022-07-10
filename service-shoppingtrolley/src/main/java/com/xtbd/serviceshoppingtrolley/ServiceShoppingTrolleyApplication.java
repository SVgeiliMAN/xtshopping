package com.xtbd.serviceshoppingtrolley;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ServiceShoppingTrolleyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceShoppingTrolleyApplication.class, args);
	}

}
