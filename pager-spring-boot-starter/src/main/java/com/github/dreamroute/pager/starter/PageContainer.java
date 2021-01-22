package com.github.dreamroute.pager.starter;

import java.util.ArrayList;

/**
 * @author w.dehi
 */
public class PageContainer<E> extends ArrayList<E> {
    private long total;
    private int pageNum;
    private int pageSize;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
