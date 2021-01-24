# pager
MyBatis分页插件，支持单表、多表关联查询

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
1. 拦截请求的条件：1. @Page标记接口，2.参数是：PageRequest
2. @Pager的属性，distinctBy（默认是"id"），用于主表去重
3. @Pager的属性，in（主表id），多表关联分页查询必须要设置（类似下方sql中的`u.id`）

### 编写sql：
```xml
<select id="selectMore" resultMap="moreResultMap">
    select u.*, a.id aid, a.name aname, a.user_id from smart_user u left join smart_addr a on u.id = a.user_id where u.name = #{param.name}
</select>
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