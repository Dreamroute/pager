package com.github.dreamroute.pager.starter.sample.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dreamroute.pager.starter.api.PageResponse;
import com.github.dreamroute.pager.starter.api.Pager;
import com.github.dreamroute.pager.starter.interceptor.PagerInterceptor;
import com.github.dreamroute.pager.starter.sample.dto.UserPageRequest;
import com.github.dreamroute.pager.starter.sample.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring Boot 4 自动配置与分页验收测试。
 *
 * @author w.dehi
 */
@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PagerInterceptor pagerInterceptor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from smart_addr");
        jdbcTemplate.update("delete from smart_user");
        jdbcTemplate.update("insert into smart_user(id, name) values (1, 'one'), (2, 'two'), (3, 'three')");
        jdbcTemplate.update("insert into smart_addr(id, user_id, name) values (1, 1, 'a'), (2, 1, 'b'), (3, 2, 'c')");
    }

    @Test
    void registersInterceptorAndPaginatesSingleTable() {
        UserPageRequest request = new UserPageRequest();
        request.setPageNum(1);
        request.setPageSize(2);

        PageResponse<User> response = Pager.query(request, userMapper::findPage);

        assertThat(pagerInterceptor).isNotNull();
        assertThat(response.getTotalNum()).isEqualTo(3);
        assertThat(response.getList()).hasSize(2);
    }

    @Test
    void paginatesJoinedQueryByPrimaryTable() {
        UserPageRequest request = new UserPageRequest();
        request.setPageNum(1);
        request.setPageSize(2);

        PageResponse<User> response = Pager.query(request, userMapper::findJoinedPage);

        assertThat(response.getTotalNum()).isEqualTo(3);
        assertThat(response.getList()).isNotEmpty();
    }
}
