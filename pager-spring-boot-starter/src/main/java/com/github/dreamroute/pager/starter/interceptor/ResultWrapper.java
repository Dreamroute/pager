package com.github.dreamroute.pager.starter.interceptor;

import java.util.ArrayList;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author w.dehi
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResultWrapper<E> extends ArrayList<E> {
    private long total;
    private int pageNum;
    private int pageSize;
}
