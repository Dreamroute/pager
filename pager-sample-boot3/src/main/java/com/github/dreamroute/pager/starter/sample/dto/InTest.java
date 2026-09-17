package com.github.dreamroute.pager.starter.sample.dto;

import com.github.dreamroute.pager.starter.api.PageRequest;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author w.dehai.2021/7/2.17:11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InTest extends PageRequest {
    private List<String> names;
    private Long id;
}
