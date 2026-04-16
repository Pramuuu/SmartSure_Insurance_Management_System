package com.smartSure.ApiGatewaySmartSure;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewaySmartSureApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewaySmartSureApplication.class, args);
	}



}
