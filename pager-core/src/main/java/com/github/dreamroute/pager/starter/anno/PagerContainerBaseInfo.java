package com.github.dreamroute.pager.starter.anno;

import lombok.Data;

/**
 * 包裹@Pager的属性
 */
@Data
public class PagerContainerBaseInfo {
    private boolean singleTable;
    private String distinctBy;
}
