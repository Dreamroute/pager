package com.github.dreamroute.pager.starter.sample.dto;

import com.github.dreamroute.pager.starter.sample.entity.Addr;
import lombok.Data;

import java.util.List;

@Data
public class SelectFromThreeTablesResp {
    private Long id;
    private String name;
    private String password;
    private String phoneNo;
    private Long version;
    private List<Addr> addrs;
}
