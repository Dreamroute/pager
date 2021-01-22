package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.mybatis.pro.service.mapper.Mapper;
import com.github.dreamroute.pager.starter.PageRequest;
import com.github.dreamroute.pager.starter.sample.entity.User;

import java.util.List;

public interface UserMapper extends Mapper<User, Long> {
    List<User> selectPage(PageRequest<User> request);
}
