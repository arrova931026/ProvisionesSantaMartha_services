package com.shsm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SociedadHumanistaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SociedadHumanistaApplication.class, args);
    }
}
