package com.innerview.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class SpringServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringServicesApplication.class, args);
	}

}
