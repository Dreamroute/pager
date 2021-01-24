package com.github.dreamroute.pager.starter.interceptor;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;

/**
 * @author w.dehi
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageContainer<E> extends ArrayList<E> {
    private long total;
    private int pageNum;
    private int pageSize;
}
