# pager

#### 市面上的mybatis分页插件存在的问题

* 对于多表查询的支持都不够好，比如一对多分页结果往往会存在分页不准的问题
* 面对极端情况，比如主表、子表都存在查询条件的场景，市面上的分页插件基本上就无能为力
* 解决PageHelper的关联查询不准问题：
  * 分页插件不支持嵌套结果映射
  * https://github.com/pagehelper/Mybatis-PageHelper/blob/master/wikis/zh/Important.md

#### 开发本插件的目的

* 解决市面上mybatis分页插件存在的问题
* 插件使用要足够便捷
* 编写的原始SQL语句要足够简单

#### 此插件，支持单表分页，多表关联查询的分页，能支持比较极端的复杂查询分页

> * 目前只兼容MySQL和H2数据库
> * 此插件与其他分页插件有冲突，比如和PageHelper，只能二选一
> * 如果你的查询结果和预期不一样，那么很可能是有其他分页插件冲突了

#### 分页原理

* 假设前端页面展示用户列表分页查询页面，并且带有用户名的模糊查询；

* 你的查询用户的原始sql语句类似这样：

  ```
  select * from user where name like concat('%',#{name},'%')
  ```

* 当mybatis执行到此sql时，插件内部会拦截此sql（用户无需关心插件做了什么），做如下事情：

    * 插件会根据此sql生成统计sql，并且执行统计查询

    * 如果统计sql的查询结果不为0，那么改写原始sql语句，加入分页pageNum和pageSize，类似这样：

      ```
      select * from user where name like concat('%', ? ,'%') limit ?, ?
      ```

    * 将统计结果和分页数据进行组装，返回给调用方

#### 项目中引入插件
* 引入依赖
```xml
<dependency>
    <groupId>com.github.dreamroute</groupId>
    <artifactId>pager-spring-boot-starter</artifactId>
    <version>latest version</version>
</dependency>
```
* SpringBoot应用会自动注入插件
* Spring MVC按照正常插件引入方式引入插件`com.github.dreamroute.pager.starter.interceptor.PagerInterceptor`

#### 当前最新版本，[点击查看](https://search.maven.org/artifact/com.github.dreamroute/pager-spring-boot-starter)

#### 项目中使用插件

* 实体：

  ```java
  @Data
  public class User {
      @Id
      private Long id;
      private String name;
      private String phoneNo;
  }
  ```

* 在Mapper接口的方法上加入注解：`@Pager`

  ```
  public interface UserMapper {
      @Pager
      List<User> listUsers(UserReq request);
  }
  ```

* 方法参数使用`com.github.dreamroute.pager.starter.api.PageRequest`的子类

  ```
  @Data
  public class UserReq extends PageRequest {
      private String name;
  }
  ```

* 编写原始查询sql

  ```
  select * from user where name like concat('%',#{name},'%')
  ```

* 设置`@Pager`的属性`distinctBy`，关联查询需要设置

  > 1、@Pager的属性，distinctBy（默认是"id"），用在多表查询的主表去重，否则1对多，多对多的查询会出现分页不准确问题，一般来说是主表别名+主键字段，如：
  > `select * from user u left join addr a on u.id = a.uid where xxx order by u.id`
  >
  > 那么@Pager(distinctBy = "u.id")
  >
  > 2、如果你的系统中的主键都是使用`id`这个名字，那么`单表`可以不用设置

* 构造查询参数：

  ```
  UserReq request = new UserReq();
  request.setPageNum(1);
  request.setPageSize(2);
  request.setName("xxx");
  ```

* 执行查询

  ```
  PageResponse<User> result = Pager.page(request, userMapper::listUsers);
  ```

* 大功告成

  > 说明：你无需编写统计SQL语句，也无需关心多表联查数据分页不准确的问题，统统插件帮你完成
  > 只支持主表排序，禁止子表排序，比如: select * from a left join b on xxx order by a.id, b.id，这里不能有b.id

### 

#### 详细原理

#### 单表
* 原始SQL：
```
SELECT
	* 
FROM
	smart_user 
WHERE
	NAME = #{name}
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
	COUNT( * ) _$count$_ 
FROM
	( SELECT * FROM smart_user WHERE NAME = ? ) _$_t
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
	u.NAME = #{name} and a.user_id = #{userId} 
ORDER BY
	u.id DESC
```
* 切记: 多表必须要使用<resultMap>方式处理结果, 利用mybatis自动折叠功能, 例如:
```xml
  <resultMap id="twoTablesResultMap" type="com.github.dreamroute.pager.starter.sample.dto.SelectFromTwoTablesResp">
      <id column="id" property="id" />
      <result column="name" property="name"/>
      <collection property="addrs" ofType="com.github.dreamroute.pager.starter.sample.entity.Addr">
          <id column="aid" property="id"/>
          <result column="aname" property="name"/>
          <result column="user_id" property="userId"/>
      </collection>
  </resultMap>
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
			u.id
		FROM
			smart_user u
			LEFT JOIN smart_addr a ON u.id = a.user_id 
		WHERE
			u.NAME = ? 
			AND a.user_id = ? 
		ORDER BY
			u.id DESC
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

#### 若干demo举例

> 1、参考本项目的pager-sample模块里面的`UserMapperTest`这个类下的单元测试，包含了各种场景的分页查询
>
> 2、默认是H2数据库，项目clone下来直接就可以运行单元测试，如果想使用mysql则需要创建下面的表

#### 建表：
```
DROP TABLE IF EXISTS `smart_user`;
DROP TABLE IF EXISTS `smart_addr`;
DROP TABLE IF EXISTS `smart_city`;
DROP TABLE IF EXISTS `backup_table`;

CREATE TABLE `smart_user`
(
    `id`       bigint(20) NOT NULL AUTO_INCREMENT,
    `name`     varchar(32) DEFAULT NULL,
    `password` varchar(32) DEFAULT '123456',
    `version`  bigint(20) DEFAULT NULL,
    `phone_no` varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `smart_addr`
(
    `id`      bigint(20) NOT NULL AUTO_INCREMENT,
    `name`    varchar(32)  DEFAULT NULL,
    `user_id` bigint(20) NOT NULL,
    `detail`  varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `smart_city`
(
    `id`      bigint(20) NOT NULL AUTO_INCREMENT,
    `name`    varchar(32) DEFAULT NULL,
    `addr_id` bigint(20),
    PRIMARY KEY (`id`)
);

CREATE TABLE `backup_table`
(
    `id`         bigint(20) not null auto_increment primary key,
    `table_name` varchar(100)  default null,
    `data`       varchar(2000) default null
);
```