package com.github.dreamroute.pager.starter.sample.dto;

import com.github.dreamroute.pager.starter.api.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @author w.dehai.2021/7/1.11:29
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class SelectUseInCondition extends PageRequest<Object> {
    private List<String> names;
}
