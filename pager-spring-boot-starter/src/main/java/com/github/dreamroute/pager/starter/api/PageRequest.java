package com.github.dreamroute.pager.starter.api;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@AllArgsConstructor
@NoArgsConstructor
public class PageRequest implements Serializable {

    private static int DEFAULT_PAGE_NUM = 1;
    private static int DEFAULT_PAGE_SIZE = 10;

    /**
     * 分页信息
     **/
    @Min(1)
    @NotNull
    @ApiModelProperty("页码")
    private int pageNum;

    @Min(1)
    @NotNull
    @Max(Integer.MAX_VALUE)
    @ApiModelProperty("每页行数")
    private int pageSize;

    public static class Builder implements Serializable {
        int pageNum = DEFAULT_PAGE_NUM;
        int pageSize = DEFAULT_PAGE_SIZE;

        /**
         * 设置当前页码
         */
        public Builder pageNum(int pageNum) {
            this.pageNum = pageNum;
            return this;
        }

        /**
         * 设置每页行数
         */
        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 同时设置当前页码 以及 每页行数
         */
        public Builder pageNumAndSize(int pageNum, int pageSize) {
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            return this;
        }

        PageRequest build() {
            return new PageRequest(pageNum, pageSize);
        }
    }

}
