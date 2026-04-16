package com.smartSure.config_server_smart_sure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerSmartSureApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerSmartSureApplication.class, args);
	}

}
