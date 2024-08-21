package com.github.dreamroute.pager.starter.sample.entity;

import com.github.dreamroute.mybatis.pro.core.annotations.Id;
import com.github.dreamroute.mybatis.pro.core.annotations.Table;
import java.util.List;
import lombok.Data;

@Data
@Table("smart_addr")
public class Addr {
    @Id
    private Long id;

    private String name;
    private Long userId;
    private List<City> cities;
}
