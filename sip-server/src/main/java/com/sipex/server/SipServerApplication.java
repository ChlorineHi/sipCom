package com.sipex.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sipex.server.mapper")
public class SipServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SipServerApplication.class, args);
        System.out.println("SIP服务器已启动，端口：8080");
    }
}

