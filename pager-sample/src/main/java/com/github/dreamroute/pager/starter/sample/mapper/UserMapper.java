package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.mybatis.pro.service.mapper.BaseMapper;
import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTables;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTablesResp;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTables;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTablesResp;
import com.github.dreamroute.pager.starter.sample.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User, Long> {

    @Pager
    List<User> selectOneTable(PageRequest<User> request);

    @Pager(distinctBy = "u.id")
    List<SelectFromTwoTablesResp> selectFromTwoTables(PageRequest<SelectFromTwoTables> request);

    @Pager(distinctBy = "u.id")
    List<SelectFromThreeTablesResp> selectFromThreeTables(PageRequest<SelectFromThreeTables> request);

    List<User> findByName(String name);

    @Pager
    @Select("select * from smart_user")
    List<User> selectPage(PageRequest<Object> request);
}
