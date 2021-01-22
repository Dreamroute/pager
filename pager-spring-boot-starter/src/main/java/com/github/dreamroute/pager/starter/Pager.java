package com.github.dreamroute.pager.starter;

import java.util.List;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * 查询辅助工具类
 *
 * @author w.dehi
 */
public class Pager {
    public static <T, R> PageResponse<R> page(PageRequest<T> pageRequest, Function<PageRequest<T>, List<R>> query) {
        PageContainer<R> resp = (PageContainer<R>) query.apply(pageRequest);
        PageResponse<R> result = new PageResponse<>();
        List<R> data = ofNullable(resp).orElseGet(PageContainer::new).stream().collect(toList());
        result.setTotalNum(resp.getTotal());
        result.setPageNum(resp.getPageNum());
        result.setPageSize(resp.getPageSize());
        result.setData(data);
        return result;
    }
}
