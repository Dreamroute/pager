package com.github.dreamroute.pager.starter.sample;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 4 示例应用。
 *
 * @author w.dehi
 */
@SpringBootApplication
@MapperScan("com.github.dreamroute.pager.starter.sample.mapper")
public class PagerBoot4SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagerBoot4SampleApplication.class, args);
    }
}
