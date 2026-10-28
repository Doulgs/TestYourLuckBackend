package com.doulgs.testyourluck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TestyourluckApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestyourluckApiApplication.class, args);
	}

}
