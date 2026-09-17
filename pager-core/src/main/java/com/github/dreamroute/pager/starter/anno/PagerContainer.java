package com.github.dreamroute.pager.starter.anno;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.mapping.ParameterMapping;

/**
 * 包裹@Pager的属性
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PagerContainer extends PagerContainerBaseInfo {

    public static final String ID = "id";

    private String countSql;
    private String afterSql;
    private List<ParameterMapping> originPmList;
    private List<ParameterMapping> afterPmList;
}
