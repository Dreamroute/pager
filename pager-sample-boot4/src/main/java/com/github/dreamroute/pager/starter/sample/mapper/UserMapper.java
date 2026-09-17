package com.github.dreamroute.pager.starter.sample.mapper;

import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.sample.dto.UserPageRequest;
import com.github.dreamroute.pager.starter.sample.entity.User;
import java.util.List;
import org.apache.ibatis.annotations.Select;

/**
 * 示例 Mapper。
 *
 * @author w.dehi
 */
public interface UserMapper {

    @Pager
    @Select("select id, name from smart_user order by id")
    List<User> findPage(UserPageRequest request);

    @Pager(distinctBy = "u.id")
    @Select("select u.id, u.name from smart_user u left join smart_addr a on u.id = a.user_id order by u.id")
    List<User> findJoinedPage(UserPageRequest request);
}
