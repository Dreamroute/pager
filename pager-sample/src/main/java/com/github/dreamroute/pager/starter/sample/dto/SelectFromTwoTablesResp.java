package com.github.dreamroute.pager.starter.sample.dto;

import com.github.dreamroute.pager.starter.sample.entity.Addr;
import java.util.List;
import lombok.Data;

@Data
public class SelectFromTwoTablesResp {
    private Long id;
    private String name;
    private String password;
    private String phoneNo;
    private Long version;
    private List<Addr> addrs;
}
