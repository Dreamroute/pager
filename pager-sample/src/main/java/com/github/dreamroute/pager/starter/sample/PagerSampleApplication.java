package com.github.dreamroute.pager.starter.sample;

import com.github.dreamroute.sqlprinter.starter.anno.EnableSQLPrinter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableSQLPrinter
@SpringBootApplication
@MapperScan(basePackages = { "com.github.dreamroute.pager.starter.sample.mapper" })
public class PagerSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagerSampleApplication.class, args);
    }

}
