package com.github.dreamroute.pager.starter.sample.entity;

import com.github.dreamroute.mybatis.pro.core.annotations.Id;
import com.github.dreamroute.mybatis.pro.core.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("smart_user")
public class User {
    @Id
    private Long   id;
    private String name;
    private String password;
    private String phoneNo;
    private Long   version;
}
