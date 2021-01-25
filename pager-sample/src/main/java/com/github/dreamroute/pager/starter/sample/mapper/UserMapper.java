package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.mybatis.pro.service.mapper.Mapper;
import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.sample.entity.More;
import com.github.dreamroute.pager.starter.sample.entity.User;

import java.util.List;

public interface UserMapper extends Mapper<User, Long> {

    @Pager
    List<User> selectPage(PageRequest<User> request);

    @Pager(distinctBy = "u.id")
    List<More> selectMore(PageRequest<User> request);

    List<User> findByName(String name);
}
