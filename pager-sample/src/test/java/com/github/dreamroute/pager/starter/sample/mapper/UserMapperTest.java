package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.api.PageResponse;
import com.github.dreamroute.pager.starter.api.Pager;
import com.github.dreamroute.pager.starter.sample.entity.More;
import com.github.dreamroute.pager.starter.sample.entity.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Insert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static com.alibaba.fastjson.JSON.toJSONString;
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
    }

    @Test
    void selectByPageTest() {
        PageRequest<User> request = PageRequest.<User>builder()
                .pageNum(1)
                .pageSize(2)
                .param(User.builder().name("w.dehai").build())
                .build();
        PageResponse<User> result = Pager.page(request, userMapper::selectPage);
        System.err.println(result);
    }

    @Test
    void selectMoreTest() {
        PageRequest<User> request = PageRequest.<User>builder()
                .pageNum(1)
                .pageSize(2)
                .param(User.builder().name("w.dehai").build())
                .build();
        PageResponse<More> result = Pager.page(request, userMapper::selectMore);
        System.err.println(toJSONString(result, true));
    }

}
