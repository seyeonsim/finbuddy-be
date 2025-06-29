package com.http200ok.finbuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinbuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinbuddyApplication.class, args);
	}

}
