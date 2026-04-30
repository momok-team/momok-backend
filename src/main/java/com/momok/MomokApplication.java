package com.momok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MomokApplication {

	public static void main(String[] args) {
		SpringApplication.run(MomokApplication.class, args);
	}

}
