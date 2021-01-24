# pager
MyBatis分页插件，支持单表、多表关联查询的分页

### SpringBoot，引入依赖：
```xml
<dependency>
    <groupId>com.github.dreamroute</groupId>
    <artifactId>pager-spring-boot-starter</artifactId>
    <version>latest version</version>
</dependency>
```
# 举例：
> 由于单表较简单，这里举多表关联查询分页

### 编写实体`More`和`Addr`，关系为1对多
```java
@Data
public class More {
    private Long id;
    private String name;
    private String password;
    private String phoneNo;
    private Long version;
    private List<Addr> addrs;
}

```
```java
@Data
public class Addr {
    @Id
    private Long id;
    private String name;
    private Long userId;
}
```

### 请求参数对象：
```java
@Data
public class User {
    @Id
    private Long id;
    private String name;
    private String password;
    private String phoneNo;
    private Long version;
}

```

### 编写Mapper接口，并且给接口添加Pager注解：
```java
public interface UserMapper {

    @Pager
    List<User> selectPage(PageRequest<User> request);

    @Pager(in = "u.id")
    List<More> selectMore(PageRequest<User> request);
}
```
### @Pager注解说明：
0. 单表无需设置@Pager的属性
1. 请求被分页拦截的条件：1. @Page标记接口，2.参数是：PageRequest
2. @Pager的属性，distinctBy（主表id，默认是"id"），用于主表去重
3. @Pager的属性，in（主表id），多表关联分页查询必须要设置（类似下方sql中的`u.id`）

### 编写sql：
```
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.github.dreamroute.pager.starter.sample.mapper.UserMapper">
    <select id="selectPage" resultType="user">
        select * from smart_user where name = #{param.name}
    </select>

    <select id="selectMore" resultMap="moreResultMap">
        select u.*, a.id aid, a.name aname, a.user_id from smart_user u left join smart_addr a on u.id = a.user_id where u.name = #{param.name}
    </select>
    <resultMap id="moreResultMap" type="more">
        <id column="id" property="id" />
        <result column="name" property="name"/>
        <collection property="addrs" ofType="addr">
            <id column="aid" property="id"/>
            <result column="aname" property="name"/>
            <result column="user_id" property="userId"/>
        </collection>
    </resultMap>

</mapper>
```
### 被分页拦截改写之后的sql：
##### 分页sql:
```
SELECT
	u.*,
	a.id aid,
	a.NAME aname,
	a.user_id 
FROM
	smart_user u
	LEFT JOIN smart_addr a ON u.id = a.user_id 
WHERE
	u.id IN (
	SELECT
		* 
	FROM
		(
		SELECT DISTINCT
			id 
		FROM
			(
			SELECT
				u.*,
				a.id aid,
				a.NAME aname,
				a.user_id 
			FROM
				smart_user u
				LEFT JOIN smart_addr a ON u.id = a.user_id 
			WHERE
				u.NAME = 'w.dehai' 
			) t 
			LIMIT 0,
			2 
		) tt 
	)
```
##### 统计sql：
```
SELECT
	count( id ) c 
FROM
	(
	SELECT DISTINCT
		id 
	FROM
		(
		SELECT
			u.*,
			a.id aid,
			a.NAME aname,
			a.user_id 
		FROM
			smart_user u
			LEFT JOIN smart_addr a ON u.id = a.user_id 
		WHERE
			u.NAME = 'w.dehai' 
		) t 
	) tt
```

### 查询：
```
@Test
void selectMoreTest() {
    PageRequest<User> request = new PageRequest<>();
    request.setPageNum(1);
    request.setPageSize(2);
    User user = new User();
    user.setName("w.dehai");
    request.setParam(user);
    PageResponse<More> result = Pager.page(request, userMapper::selectMore);
    System.err.println(result);
}
```

### 得到的结果：
```java
@Data
public class PageResponse<T> {

    private int pageNum;
    private int pageSize;
    private long totalNum;
    private List<T> data;

}

```
