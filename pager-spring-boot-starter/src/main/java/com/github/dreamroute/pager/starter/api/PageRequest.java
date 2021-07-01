package com.github.dreamroute.pager.starter.api;

import com.google.errorprone.annotations.NoAllocation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 分页请求
 *
 * @author w.dehai
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequest<E> implements Serializable {

    /** 分页信息 **/
    @Min(1)
    @NotNull
    private int pageNum = 1;

    @Min(1)
    @NotNull
    @Max(Integer.MAX_VALUE)
    private int pageSize = 10;

    /** 请求参数对象 **/
    @Valid
    private E param;

}
