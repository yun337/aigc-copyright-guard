package com.copyright;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 链创守护 - AIGC作品版权保护平台后端启动类
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CopyrightGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopyrightGuardApplication.class, args);
    }
}
