package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.pager.starter.PageRequest;
import com.github.dreamroute.pager.starter.PageResponse;
import com.github.dreamroute.pager.starter.Pager;
import com.github.dreamroute.pager.starter.sample.entity.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Insert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

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
        Insert insert = insertInto("smart_user")
                .columns("id", "name")
                .values(1L, "w.dehai")
                .values(2L, "Jaedong")
                .values(3L, "Dreamroute")
                .build();
        new DbSetup(new DataSourceDestination(dataSource), insert).launch();
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

}
