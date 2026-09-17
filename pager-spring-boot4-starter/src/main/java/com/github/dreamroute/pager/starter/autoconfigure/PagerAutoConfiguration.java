package com.github.dreamroute.pager.starter.autoconfigure;

import com.github.dreamroute.pager.starter.interceptor.PagerInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Spring Boot 4 自动配置。
 *
 * @author w.dehi
 */
@AutoConfiguration
@AutoConfigureBefore(MybatisAutoConfiguration.class)
@ConditionalOnClass({PagerInterceptor.class, SqlSessionFactory.class})
public class PagerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PagerInterceptor pagerInterceptor() {
        return new PagerInterceptor();
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> pagerInterceptorInitializer(PagerInterceptor pagerInterceptor, SqlSessionFactory sqlSessionFactory) {
        return event -> {
            if (event.getApplicationContext().getParent() == null) {
                pagerInterceptor.initialize(sqlSessionFactory);
            }
        };
    }
}
