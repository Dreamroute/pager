package com.github.dreamroute.pager.starter.sample.entity;

import lombok.Data;

import java.util.List;

@Data
public class More {
    private Long id;
    private String name;
    private String password;
    private String phoneNo;
    private Long version;
    private List<Addr> addrs;
}
