package com.kuberfashion.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class KuberFashionApplication {

	public static void main(String[] args) {
		SpringApplication.run(KuberFashionApplication.class, args);
	}

}
