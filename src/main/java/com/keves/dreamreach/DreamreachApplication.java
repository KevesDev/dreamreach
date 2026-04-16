package com.keves.dreamreach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DreamreachApplication {

    public static void main(String[] args) {
        SpringApplication.run(DreamreachApplication.class, args);
    }

}