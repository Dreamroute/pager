package com.github.dreamroute.pager.starter.sample.entity;

import java.util.List;
import lombok.Data;

@Data
public class Addr {
    private Long id;

    private String name;
    private Long userId;
    private List<City> cities;
}
