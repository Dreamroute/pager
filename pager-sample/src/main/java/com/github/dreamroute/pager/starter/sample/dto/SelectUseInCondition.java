package com.github.dreamroute.pager.starter.sample.dto;

import com.github.dreamroute.pager.starter.api.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author w.dehai.2021/7/1.11:29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SelectUseInCondition extends PageRequest {
    private List<String> names;
}
