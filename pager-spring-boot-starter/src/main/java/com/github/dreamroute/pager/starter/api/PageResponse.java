package com.github.dreamroute.pager.starter.api;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页结果
 *
 * @author w.dehai
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> implements Serializable {

    private int pageNum;
    private int pageSize;
    private long totalNum;
    private List<T> list;
}
