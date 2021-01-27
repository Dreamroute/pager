# pager
**MyBatis分页插件，支持单表分页，多表关联查询的分页**

### 使用方法
* SpringBoot引入依赖
```xml
<dependency>
    <groupId>com.github.dreamroute</groupId>
    <artifactId>pager-spring-boot-starter</artifactId>
    <version>latest version</version>
</dependency>
```
* 在Mapper接口方法上添加@Pager注解，并将接口的参数改为类型为PageRequest
* 调用方法，例如: `PageResponse<User> result = Pager.page(request, userMapper::selectXxx);`
* 完成接入，你无需编写统计SQL语句，也无需关心多表联查数据分页不准确的问题，统统插件帮你完成

### 分页原理
#### 单表
* 原始SQL：
```
SELECT
	* 
FROM
	smart_user 
WHERE
	NAME = #{param.name}
```
* 被插件拦截，自动插入分页信息之后的SQL：
```
SELECT
	* 
FROM
	smart_user 
WHERE
	NAME = ? 
LIMIT ?, ?
```
* 被插件拦截，自动生成统计SQL：
```
SELECT
	COUNT( * ) _count_ 
FROM
	( SELECT * FROM smart_user WHERE NAME = 'w.dehai' ) t
```

#### 多表
* 原始SQL：
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
	u.NAME = #{param.name} and a.user_id = #{param.userId} 
ORDER BY
	u.id DESC,
	u.NAME ASC
```
* 被插件拦截，自动插入分页信息之后的SQL：
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
		u.id 
	FROM
		(
		SELECT DISTINCT
			u.id,
			u.NAME 
		FROM
			smart_user u
			LEFT JOIN smart_addr a ON u.id = a.user_id 
		WHERE
			u.NAME = ? 
			AND a.user_id = ? 
		ORDER BY
			u.id DESC,
			u.NAME ASC 
		LIMIT ?, ? 
		) u 
	) 
	AND u.NAME = ?
	AND a.user_id = ? 
ORDER BY
	u.id DESC,
	u.NAME ASC
```
* 被插件拦截，自动生成统计SQL：
```
SELECT
	count( DISTINCT u.id ) __count__ 
FROM
	smart_user u
	LEFT JOIN smart_addr a ON u.id = a.user_id 
WHERE
	u.NAME = ? 
	AND a.user_id = ?
```

# 举例
> 下列举的例都在本工程下的pager-sample中，可以clone下来运行单元测试
### 建表：
```
CREATE TABLE `smart_user`
(
    `id`       bigint(20) NOT NULL AUTO_INCREMENT,
    `name`     varchar(32) DEFAULT NULL,
    `password` varchar(32) DEFAULT '123456',
    `version`  bigint(20)  DEFAULT NULL,
    `phone_no` varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `smart_addr`
(
    `id`       bigint(20) NOT NULL AUTO_INCREMENT,
    `name`     varchar(32) DEFAULT NULL,
    `user_id` bigint(20),
    PRIMARY KEY (`id`)
);

CREATE TABLE `smart_city`
(
    `id`       bigint(20) NOT NULL AUTO_INCREMENT,
    `name`     varchar(32) DEFAULT NULL,
    `addr_id` bigint(20),
    PRIMARY KEY (`id`)
);
```

# 单表分页
#### 请求参数对象：
```
@Data
public class User {
    private String name;
}

```
#### UserMapper接口
```
public interface UserMapper {

    @Pager
    List<User> selectOneTable(PageRequest<User> request);

}
```
#### SQL语句
```
<select id="selectOneTable" resultType="com.github.dreamroute.pager.starter.sample.entity.User">
    select * from smart_user where name = #{param.name}
</select>
```
#### 请求
```
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
```

# 多表分页

#### 请求参数对象：
```java
@Data
public class City {
    private Long id;
    private String name;
    private Long addrId;
}


```
```java
@Data
public class Addr {
    private Long id;
    private String name;
    private Long userId;
}
```

### 请求参数对象：
```java
@Data
public class User {
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

    @Pager(distinctBy = "u.id")
    List<SelectFromTwoTablesResp> selectFromTwoTables(PageRequest<SelectFromTwoTables> request);

    @Pager(distinctBy = "u.id")
    List<SelectFromThreeTablesResp> selectFromThreeTables(PageRequest<SelectFromThreeTables> request);
}
```
### @Pager注解说明：
* 需要分页的查询接口需要被@Pager标记，同时，多表还需要设置distinctBy属性，单表不需要
* 请求被分页拦截的条件：1. @Page标记接口，2.参数是：PageRequest
* @Pager的属性，distinctBy（主表id列，默认是"id"），用于主表去重

### SQL语句：
```
<select id="selectFromTwoTables" resultMap="twoTablesResultMap">
    select u.*, a.id aid, a.name aname, a.user_id from smart_user u left join smart_addr a on u.id = a.user_id where u.name = #{param.name} and a.user_id = #{param.userId} order by u.id desc, u.name asc
</select>
<resultMap id="twoTablesResultMap" type="com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTablesResp">
    <id column="id" property="id" />
    <result column="name" property="name"/>
    <collection property="addrs" ofType="addr">
        <id column="aid" property="id"/>
        <result column="aname" property="name"/>
        <result column="user_id" property="userId"/>
    </collection>
</resultMap>

<select id="selectFromThreeTables" resultMap="threeTablesResultMap">
    select u.*, a.id aid, a.name aname, a.user_id, c.id cid, c.name cname, c.addr_id from smart_user u left join smart_addr a on u.id = a.user_id left join smart_city c on a.id = c.addr_id
    where u.name = #{param.name} and a.user_id = #{param.userId} and c.name = #{param.cityName} order by u.id desc, u.name asc
</select>
<resultMap id="threeTablesResultMap" type="com.github.dreamroute.pager.starter.sample.dto.SelectFromThreeTablesResp">
    <id column="id" property="id" />
    <result column="name" property="name"/>
    <collection property="addrs" ofType="addr">
        <id column="aid" property="id"/>
        <result column="aname" property="name"/>
        <result column="user_id" property="userId"/>
        <collection property="cities" ofType="city">
            <id column="cid" property="id"/>
            <result column="cname" property="name"/>
            <result column="addr_id" property="addrId"/>
        </collection>
    </collection>
</resultMap>
```
#### 请求
```
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
