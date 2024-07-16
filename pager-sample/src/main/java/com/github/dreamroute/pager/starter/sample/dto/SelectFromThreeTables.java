package com.github.dreamroute.pager.starter.sample.dto;

import com.github.dreamroute.pager.starter.api.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SelectFromThreeTables extends PageRequest {
    private String name;
    private Long   userId;
    private String cityName;
}
