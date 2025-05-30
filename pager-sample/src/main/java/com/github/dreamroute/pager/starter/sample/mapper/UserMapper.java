package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.mybatis.pro.service.mapper.BaseMapper;
import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.sample.dto.InTest;
import com.github.dreamroute.pager.starter.sample.dto.MultiParamsReq;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromOneTable;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTables;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTablesResp;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTables;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTablesResp;
import com.github.dreamroute.pager.starter.sample.dto.SelectUseInCondition;
import com.github.dreamroute.pager.starter.sample.dto.WithParamAnno;
import com.github.dreamroute.pager.starter.sample.entity.User;

import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User, Long> {

    @Pager
    List<User> selectFromOneTable(SelectFromOneTable request);

    @Pager
    List<User> inTest(InTest request);

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

    @Pager
    List<User> withParamAnno(@Param("req") WithParamAnno wpa);

    List<User> findByNameAndPassword(String name, String password);

    @Pager
    List<User> multiParams(@Param("req") MultiParamsReq req);

    List<User> foreachTest(List<String> names);
}
