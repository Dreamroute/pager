package com.github.dreamroute.pager.starter.sample;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 示例应用。
 *
 * @author w.dehi
 */
@SpringBootApplication
@MapperScan(basePackages = {"com.github.dreamroute.pager.starter.sample.mapper"})
public class PagerSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagerSampleApplication.class, args);
    }
}
