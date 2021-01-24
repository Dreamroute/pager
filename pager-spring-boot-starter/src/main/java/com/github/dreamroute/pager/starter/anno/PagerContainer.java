package com.github.dreamroute.pager.starter.anno;

import lombok.Data;

/**
 * 包裹@Pager的属性
 */
@Data
public class PagerContainer {

    public static final String ID ="id";

    private String distinctColumn;
    private String in;
    private String count;
    private String sql;
}