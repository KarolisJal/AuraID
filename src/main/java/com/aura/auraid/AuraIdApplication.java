package com.aura.auraid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuraIdApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuraIdApplication.class, args);
	}

}
