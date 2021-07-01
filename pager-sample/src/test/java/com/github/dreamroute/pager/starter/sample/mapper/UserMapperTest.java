package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.api.PageResponse;
import com.github.dreamroute.pager.starter.api.Pager;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTables;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTablesResp;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTables;
import com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTablesResp;
import com.github.dreamroute.pager.starter.sample.dto.SelectUseInCondition;
import com.github.dreamroute.pager.starter.sample.entity.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Insert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static com.github.dreamroute.pager.starter.api.Pager.page;
import static com.google.common.collect.Lists.newArrayList;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.truncate;

@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void init() {
        new DbSetup(new DataSourceDestination(dataSource), truncate("smart_user")).launch();
        new DbSetup(new DataSourceDestination(dataSource), truncate("smart_addr")).launch();
        new DbSetup(new DataSourceDestination(dataSource), truncate("smart_city")).launch();
        Insert initUser = insertInto("smart_user")
                .columns("id", "name")
                .values(1L, "w.dehai")
                .values(2L, "Jaedong")
                .values(3L, "Dreamroute")
                .values(4L, "w.dehai")
                .values(5L, "w.dehai")
                .build();
        new DbSetup(new DataSourceDestination(dataSource), initUser).launch();

        Insert initAddr = insertInto("smart_addr")
                .columns("id", "name", "user_id")
                .values(1L, "w.dehai", 1L)
                .values(2L, "Jaedong", 1L)
                .values(3L, "Jaedong", 4L)
                .values(4L, "Jaedong", 5L)
                .values(5L, "Jaedong", 5L)
                .build();
        new DbSetup(new DataSourceDestination(dataSource), initAddr).launch();

        Insert initCity = insertInto("smart_city")
                .columns("id", "name", "addr_id")
                .values(1L, "成都", 1L)
                .values(2L, "北京", 1L)
                .build();
        new DbSetup(new DataSourceDestination(dataSource), initCity).launch();
    }

    @Test
    void selectPageTest() {
        PageRequest<Object> request = new PageRequest<>();
        request.setPageNum(1);
        request.setPageSize(2);
        PageResponse<User> page = page(request, userMapper::selectPage);
        System.err.println(page);
    }

    @Test
    void selectOneTableTest() {
        PageRequest<User> request = new PageRequest<>();
        request.setPageNum(1);
        request.setPageSize(2);

        User user = new User();
        user.setName("w.dehai");
        request.setParam(user);

        PageResponse<User> result = Pager.page(request, userMapper::selectOneTable);
        System.err.println(result);
    }

    @Test
    void selectUseInConditionTest() {
        PageRequest<SelectUseInCondition> request = new PageRequest<>();
        request.setPageNum(1);
        request.setPageSize(2);

        SelectUseInCondition suc = new SelectUseInCondition();
        suc.setPageNum(1);
        suc.setPageSize(2);
        suc.setNames(newArrayList("w.dehai", "Dreamroute"));

        PageResponse<User> result = Pager.page(request, userMapper::selectUseInCondition);
        System.err.println(result);
    }

    @Test
    void selectOneTableNoResultTest() {
        PageRequest<User> request = new PageRequest<>();
        request.setPageNum(1);
        request.setPageSize(2);

        User user = new User();
        user.setName("~~");
        request.setParam(user);

        PageResponse<User> result = Pager.page(request, userMapper::selectOneTable);
        System.err.println(result);
    }

    @Test
    void selectFromTwoTablesTest() {
        PageRequest<SelectFromTwoTables> request = new PageRequest<>();
        request.setPageNum(1);
        request.setPageSize(2);

        SelectFromTwoTables param = new SelectFromTwoTables();
        param.setName("w.dehai");
        param.setUserId(1L);
        request.setParam(param);

        PageResponse<SelectFromTwoTablesResp> result = Pager.page(request, userMapper::selectFromTwoTables);
        System.err.println(result);
    }

    @Test
    void selectFromThreeTablesTest() {
        PageRequest<SelectFromThreeTables> request = new PageRequest<>();
        request.setPageNum(1);
        request.setPageSize(2);

        SelectFromThreeTables param = new SelectFromThreeTables();
        param.setName("w.dehai");
        param.setUserId(1L);
        param.setCityName("成都");
        request.setParam(param);

        PageResponse<SelectFromThreeTablesResp> result = page(request, userMapper::selectFromThreeTables);
        System.err.println(result);
    }

}
