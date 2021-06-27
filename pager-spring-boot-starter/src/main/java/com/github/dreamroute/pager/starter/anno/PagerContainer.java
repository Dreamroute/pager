package com.github.dreamroute.pager.starter.anno;

import lombok.Data;
import org.apache.ibatis.mapping.ParameterMapping;

import java.util.List;

/**
 * 包裹@Pager的属性
 */
@Data
public class PagerContainer {

    public static final String ID ="id";

    private String distinctBy = ID;
    private String countSql;
    private boolean singleTable;
    private String afterSql;
    private List<ParameterMapping> originPmList;
    private List<ParameterMapping> afterPmList;
}
