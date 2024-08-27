package com.github.dreamroute.pager.starter.sample.mapper;

import static com.google.common.collect.Lists.newArrayList;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.truncate;

import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.api.PageResponse;
import com.github.dreamroute.pager.starter.api.Pager;
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
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Insert;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
        PageRequest request = new PageRequest();
        request.setPageNum(1);
        request.setPageSize(2);
        PageResponse<User> page = Pager.query(request, userMapper::selectPage);
        System.err.println(page);
    }

    @Test
    void selectFromOneTableTest() {
        SelectFromOneTable request = new SelectFromOneTable();
        request.setPageNum(1);
        request.setPageSize(2);
        request.setName("w.dehai");
        PageResponse<User> result = Pager.query(request, userMapper::selectFromOneTable);
        System.err.println(result);
    }

    @Test
    void selectUseInConditionTest() {

        SelectUseInCondition suc = new SelectUseInCondition();
        suc.setPageNum(1);
        suc.setPageSize(2);
        suc.setNames(newArrayList("w.dehai", "Dreamroute"));

        PageResponse<User> result = Pager.query(suc, userMapper::selectUseInCondition);
        System.err.println(result);
    }

    @Test
    void selectFromOneTableNoResultTest() {
        SelectFromOneTable request = new SelectFromOneTable();
        request.setPageNum(1);
        request.setPageSize(2);
        request.setName("~~");
        PageResponse<User> result = Pager.query(request, userMapper::selectFromOneTable);
        System.err.println(result);
    }

    @Test
    void inTest() {
        InTest request = new InTest();
        request.setPageNum(1);
        request.setPageSize(2);
        request.setNames(newArrayList("wangdehai", "Dreamroute"));
        request.setId(3L);
        //        List<User> users = userMapper.inTest(request);
        //        System.err.println(users);
        PageResponse<User> result = Pager.query(request, userMapper::inTest);
        System.err.println(result);
    }

    @Test
    void selectFromTwoTablesTest() {
        SelectFromTwoTables param = new SelectFromTwoTables();
        param.setPageNum(1);
        param.setPageSize(2);
        param.setName("w.dehai");
        param.setUserId(1L);
        PageResponse<SelectFromTwoTablesResp> result = Pager.query(param, userMapper::selectFromTwoTables);
        System.err.println(result);
    }

    @Test
    void selectFromThreeTablesTest() {
        SelectFromThreeTables request = new SelectFromThreeTables();
        request.setPageNum(1);
        request.setPageSize(2);
        request.setName("w.dehai");
        request.setUserId(1L);
        request.setCityName("成都");
        PageResponse<SelectFromThreeTablesResp> result = Pager.query(request, userMapper::selectFromThreeTables);
        System.err.println(result);
    }

    /**
     * 无order by的查询
     */
    @Test
    void withNotOrderByTest() {
        SelectFromTwoTables param = new SelectFromTwoTables();
        param.setPageNum(1);
        param.setPageSize(2);
        param.setName("w.dehai");
        param.setUserId(1L);
        // 多表
        PageResponse<SelectFromTwoTablesResp> result = Pager.query(param, userMapper::withNotOrderBy);
        System.err.println(result);

        // 单表

    }

    /**
     * 无查询条件的查询
     */
    @Test
    void withNoConditionTest() {
        SelectFromTwoTables param = new SelectFromTwoTables();
        param.setPageNum(1);
        param.setPageSize(2);

        // 多表
        PageResponse<SelectFromTwoTablesResp> result = Pager.query(param, userMapper::withNoCondition);
        System.err.println(result);

        // 单表
        PageResponse<User> resp = Pager.query(new PageRequest(1, 3), userMapper::withNoConditionSingleTable);
        System.err.println(resp);
    }

    @Test
    void withParamAnnoDirectTest() {
        WithParamAnno param = new WithParamAnno();
        param.setPageNum(1);
        param.setPageSize(2);
        param.setId(1L);
        List<User> result = userMapper.withParamAnno(param);
        System.err.println(result);
    }

    @Test
    void withParamAnnoTest() {
        WithParamAnno param = new WithParamAnno();
        param.setPageNum(1);
        param.setPageSize(2);
        param.setId(1L);

        PageResponse<User> result = Pager.query(param, userMapper::withParamAnno);
        System.err.println(result);
    }

    @Test
    void findByNameAndAgeTest() {
        List<User> users = userMapper.findByNameAndPassword("w.dehai", "123456");
        System.err.println(users);
    }

    @Test
    void multiParamsTest() {
        MultiParamsReq req = new MultiParamsReq();
        req.setId(1L);
        req.setName("w.dehai");
        List<User> result = userMapper.multiParams(req);
        System.err.println(result);
    }
}
