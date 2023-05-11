package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.mybatis.pro.service.mapper.BaseMapper;
import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.api.PageResponse;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromOneTable;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTables;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTablesResp;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTables;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTablesResp;
import com.github.dreamroute.pager.starter.sample.dto.SelectUseInCondition;
import com.github.dreamroute.pager.starter.sample.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User, Long> {

    @Pager
    List<User> selectFromOneTable(SelectFromOneTable request);

    @Pager(distinctBy = "u.id")
    List<SelectFromTwoTablesResp> selectFromTwoTables(SelectFromTwoTables request);

    @Pager(distinctBy = "u.id")
    List<SelectFromThreeTablesResp> selectFromThreeTables(SelectFromThreeTables request);

    @Pager
    @Select("select * from smart_user")
    List<User> selectPage(PageRequest request);

    @Pager
    List<User> selectUseInCondition(SelectUseInCondition request);

    @Pager(distinctBy = "u.id")
    List<SelectFromTwoTablesResp> withNotOrderBy(SelectFromTwoTables request);

    @Pager(distinctBy = "u.id")
    List<SelectFromTwoTablesResp> withNoCondition(SelectFromTwoTables request);

    @Pager
    List<User> withNoConditionSingleTable(PageRequest pageRequest);

    List<User> findByName(String name);
}
