package com.github.dreamroute.pager.starter.api;

import com.github.dreamroute.pager.starter.interceptor.PageContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * 查询辅助工具类
 *
 * @author w.dehi
 */
public class Pager {
    private Pager() {}

    /**
     * 调用此方法，就能获取到分页信息
     *
     * @param pageRequest 分页请求
     * @param query 分页mapper方法
     * @param <T> 分页请求参数类型
     * @param <R> 分页mapper的返回值类型
     * @return 返回此次查询的分页信息
     */
    public static <T, R> PageResponse<R> page(PageRequest<T> pageRequest, Function<PageRequest<T>, List<R>> query) {
        PageContainer<R> resp = (PageContainer<R>) query.apply(pageRequest);
        PageResponse<R> result = new PageResponse<>();
        List<R> data = new ArrayList<>(ofNullable(resp).orElseGet(PageContainer::new));
        result.setData(data);
        result.setTotalNum(resp.getTotal());
        result.setPageNum(resp.getPageNum());
        result.setPageSize(resp.getPageSize());
        return result;
    }
}
