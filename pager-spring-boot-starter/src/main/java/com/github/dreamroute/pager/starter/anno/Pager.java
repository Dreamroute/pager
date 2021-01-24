package com.github.dreamroute.pager.starter.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 分页相关配置
 *
 * @author w.dehi
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Pager {

    /**
     * 主表主键id，用于去重
     */
    String distinctBy() default "id";

    /**
     * 主表id查询列
     */
    String in() default "id";
}
