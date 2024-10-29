package com.timzaak.cloud;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo
@SpringBootApplication
@MapperScan("com.timzaak.cloud.mapper")
public class OthersApplication {
    public static void main(String[] args) {
        SpringApplication.run(OthersApplication.class, args);
        System.out.println("OthersApplication is running");
    }
}
