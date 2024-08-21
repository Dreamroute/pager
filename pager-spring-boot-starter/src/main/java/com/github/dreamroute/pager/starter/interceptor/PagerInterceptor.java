package com.github.dreamroute.pager.starter.interceptor;

import static com.github.dreamroute.pager.starter.anno.PagerContainer.ID;
import static com.github.dreamroute.pager.starter.interceptor.ProxyUtil.getOriginObj;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import cn.hutool.core.annotation.AnnotationUtil;
import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.anno.PagerContainer;
import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.exception.PaggerException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.CollectionUtils;

/**
 * 分页插件，原理：通过注解标注需要分页的接口方法，拦截该方法，抽取原生sql，然后做如下几个动作：
 * <ol>
 *     <li>根据原生sql语句生成一个统计的sql，并且执行查询，获得统计结果；</li>
 *     <li>根据上一步结果判断，如果统计不为0，那么改写原生sql，加上分页参数，执行查询操作，获取查询结果；否则无需进行查询直接返回结果</li>
 *     <li>将上述两个结果封装成分页结果；</li>
 * </ol>
 *
 * @author w.dehi
 */
@Intercepts({
    @Signature(
            type = Executor.class,
            method = "query",
            args = {
                MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class
            }),
    @Signature(
            type = Executor.class,
            method = "query",
            args = {
                MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class,
                CacheKey.class, BoundSql.class
            })
})
public class PagerInterceptor implements Interceptor, ApplicationListener<ContextRefreshedEvent> {

    private final ConcurrentHashMap<String, PagerContainer> pagerContainer = new ConcurrentHashMap<>();

    // 单表
    private static final int SINGLE = 1;
    private static final String COUNT_NAME = "_$count$_";
    private static final String WHERE = " WHERE ";
    private static final String FROM = " FROM ";

    private Configuration config;

    /**
     * Spring启动完毕之后，就将需要分页的Mapper抽取出来，存入缓存
     */
    public void onApplicationEvent(ContextRefreshedEvent event) {
        SqlSessionFactory sqlSessionFactory = event.getApplicationContext().getBean(SqlSessionFactory.class);
        config = sqlSessionFactory.getConfiguration();
        Collection<Class<?>> mappers = config.getMapperRegistry().getMappers();
        if (mappers != null && !mappers.isEmpty()) {
            for (Class<?> mapper : mappers) {
                String mapperName = mapper.getName();
                stream(mapper.getDeclaredMethods())
                        .filter(method -> AnnotationUtil.hasAnnotation(method, Pager.class))
                        .forEach(method -> {
                            PagerContainer container = new PagerContainer();
                            String dictinctBy = AnnotationUtil.getAnnotationValue(method, Pager.class, "distinctBy");
                            if (isNotBlank(dictinctBy)) {
                                container.setDistinctBy(dictinctBy);
                            }
                            pagerContainer.put(mapperName + "." + method.getName(), container);
                        });
            }
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object param = invocation.getArgs()[1];
        // 如果参数不加@Param，那么这里是单个，如果加了，那么这里是个Map，取其一即可
        Object objParam = param;
        String paramAlias = null;

        // 如果是@Param风格，那么需要获取到对象参数以及@Param的value
        if (param instanceof ParamMap) {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "接口" + ms.getId() + "参数有误, 分页接口参数必有且仅能有一个，并且是继承了PageRequest的，需要把多个参数封装在一个对象中");
            ParamMap<?> p = (ParamMap<?>) param;
            // 如果不是分页查询，直接返回，避免其他查询走下方流程
            boolean pageSelect = p.values().stream().anyMatch(PageRequest.class::isInstance);
            if (!pageSelect) {
                return invocation.proceed();
            }
            if (p.size() != 2) {
                throw ex;
            }
            objParam = p.values().stream().findAny().orElseThrow(() -> ex);
            paramAlias = p.keySet().stream()
                    .filter(e -> !e.toLowerCase().matches("param\\d+"))
                    .findAny()
                    .orElseThrow(() -> ex);
        }

        PagerContainer pc = pagerContainer.get(ms.getId());

        // 拦截请求的条件：1. @Page标记接口，2.参数是：PageRequest
        if (pc == null || !(objParam instanceof PageRequest)) {
            return invocation.proceed();
        }

        BoundSql boundSql = ms.getBoundSql(param);
        String beforeSql = boundSql.getSql();
        String afterSql = parseSql(beforeSql, ms.getId());
        pc.setAfterSql(afterSql);
        List<ParameterMapping> beforePmList = boundSql.getParameterMappings();
        pc.setOriginPmList(beforePmList);
        List<ParameterMapping> afterPmList = parseParameterMappings(config, ms.getId(), beforePmList, paramAlias);
        pc.setAfterPmList(afterPmList);

        Executor executor = (Executor) (getOriginObj(invocation.getTarget()));
        Transaction transaction = executor.getTransaction();

        // 处理统计信息
        BoundSql countBoundSql = new BoundSql(config, pc.getCountSql(), pc.getOriginPmList(), param);
        copyProps(boundSql, countBoundSql, config);
        MappedStatement m = new Builder(
                        config,
                        ms.getId() + "(分页统计)",
                        new StaticSqlSource(config, pc.getCountSql()),
                        SqlCommandType.SELECT)
                .build();
        StatementHandler countHandler =
                config.newStatementHandler(executor, m, param, RowBounds.DEFAULT, null, countBoundSql);
        Statement countStmt = prepareStatement(transaction, countHandler);
        ((PreparedStatement) countStmt).execute();
        ResultSet rs = countStmt.getResultSet();
        ResultWrapper<Object> container = new ResultWrapper<>();
        while (rs.next()) {
            long totle = rs.getLong(COUNT_NAME);
            container.setTotal(totle);
        }
        countStmt.close();

        // 由于不希望在pageRequest中增加start参数，复用pageNum, limit ?, ?的第一个参数pageNum的值使用start，因此resp的pageNum需要在设置start之前进行设置
        PageRequest pr = (PageRequest) objParam;
        int pageNum = pr.getPageNum();
        int pageSize = pr.getPageSize();
        container.setPageNum(pageNum);
        container.setPageSize(pageSize);
        int start = (pageNum - 1) * pageSize;
        pr.setPageNum(start);

        // 处理业务查询
        if (container.getTotal() != 0L) {
            BoundSql bizBoundSql = new BoundSql(config, pc.getAfterSql(), pc.getAfterPmList(), param);
            StatementHandler bizHandler =
                    config.newStatementHandler(executor, ms, param, RowBounds.DEFAULT, null, bizBoundSql);
            Statement bizStmt = prepareStatement(transaction, bizHandler);
            List<Object> data = bizHandler.query(bizStmt, null);
            bizStmt.close();
            container.addAll(data);
        }

        // 恢复
        pr.setPageNum(pageNum);

        return container;
    }

    private Statement prepareStatement(Transaction transaction, StatementHandler handler) throws SQLException {
        Statement stmt;
        stmt = handler.prepare(transaction.getConnection(), transaction.getTimeout());
        handler.parameterize(stmt);
        return stmt;
    }

    private List<ParameterMapping> parseParameterMappings(
            Configuration config, String id, List<ParameterMapping> pmList, String alias) {
        List<ParameterMapping> result = new ArrayList<>(ofNullable(pmList).orElseGet(ArrayList::new));

        String pageNum = "pageNum";
        String pageSize = "pageSize";
        if (isNotBlank(alias)) {
            pageNum = alias + "." + pageNum;
            pageSize = alias + "." + pageSize;
        }

        result.add(new ParameterMapping.Builder(config, pageNum, int.class).build());
        result.add(new ParameterMapping.Builder(config, pageSize, int.class).build());
        // 多表情况下：由于插件改写sql会在sql的末尾增加一次查询条件，所以这里需要在sql末尾再次增加一次查询条件
        if (!pagerContainer.get(id).isSingleTable()) {
            result.addAll(ofNullable(pmList).orElseGet(ArrayList::new));
        }
        return result;
    }

    private String parseSql(String sql, String id) {
        PagerContainer container = pagerContainer.get(id);
        Select select;
        String afterSql;
        try {
            select = (Select) CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            throw new PaggerException("SQL语句异常，你的sql语句是: [" + sql + "]", e);
        }
        List<String> tableList = new TablesNamesFinder().getTableList(select);

        PlainSelect body = (PlainSelect) select.getSelectBody();
        String columns = body.getSelectItems().stream().map(Object::toString).collect(joining(","));
        String from = body.getFromItem().toString();
        String where = ofNullable(body.getWhere()).map(Object::toString).orElse("");

        if (tableList != null && tableList.size() == SINGLE) {
            where = StringUtils.isNotBlank(where) ? (WHERE + where) : "";
            sql = "SELECT " + columns + FROM + from + where;
            container.setCountSql("SELECT COUNT(1) " + COUNT_NAME + " FROM (" + sql + ") _$_t");
            String orderBy = ofNullable(body.getOrderByElements()).orElseGet(ArrayList::new).stream()
                    .map(Objects::toString)
                    .collect(joining(", "));
            orderBy = StringUtils.isNotBlank(orderBy) ? (" ORDER BY " + orderBy) : "";
            afterSql = sql + orderBy + " LIMIT ?, ?";
            container.setSingleTable(true);
        } else {
            String joins = body.getJoins().stream().map(Object::toString).collect(joining(" "));

            String alias = "";
            String distinctBy = container.getDistinctBy();
            if (isEmpty(distinctBy) || Objects.equals(ID, distinctBy)) {
                throw new PaggerException(
                        "多表查询需要设置@Pager的主表distinctBy属性, 设置方式参考插件的文档distinctBy设置方法。sql: " + select.toString());
            }
            if (distinctBy.indexOf('.') != -1) {
                alias = distinctBy.split("\\.")[0];
            }

            // 如果order by不为空，那么子查询的查询列需要将order by列也带上，否则H2会报错（order by列需要在查询列中），MySQL则不会
            String orderBy = "";
            String subQueryColumns = "";
            List<OrderByElement> orderbyList = body.getOrderByElements();
            if (!CollectionUtils.isEmpty(orderbyList)) {
                orderBy = " ORDER BY "
                        + orderbyList.stream().map(Object::toString).collect(joining(", "));

                // order by列和主表id列重复，需要去重
                Set<String> orderbyListStr = orderbyList.stream()
                        .map(OrderByElement::getExpression)
                        .map(Objects::toString)
                        .collect(toSet());
                orderbyListStr.add(distinctBy);
                subQueryColumns = String.join(", ", orderbyListStr);
            } else {
                subQueryColumns = distinctBy;
            }

            String wh = StringUtils.isNotBlank(where) ? (WHERE + where) : "";
            String afterFrom = FROM + from + " " + joins + wh;
            String subQuery = "SELECT DISTINCT " + subQueryColumns + afterFrom;
            String noCondition = "SELECT " + columns + FROM + from + " " + joins + " ";

            String result = noCondition + WHERE + distinctBy + " IN  (SELECT " + distinctBy + " FROM (" + subQuery
                    + orderBy + " LIMIT ?, ?) " + alias + ")";
            if (StringUtils.isNoneBlank(where)) {
                result = result + " AND " + where;
            }
            afterSql = result + orderBy;
            String count = "SELECT count(DISTINCT " + distinctBy + ") " + COUNT_NAME + afterFrom;
            container.setCountSql(count);
        }
        return afterSql;
    }

    /**
     * 复制两个属性到新的BoundSql中，否则对于特殊参数的处理会报错，比如where xx in ()这种的。
     * 原因是：创建MappedStatement的时候参数全部使用的是StaticSqlSource类型的SqlSource，而真实的情况是不一定全都是StaticSqlSource
     */
    private static void copyProps(BoundSql oldBs, BoundSql newBs, Configuration config) {
        MetaObject oldMo = config.newMetaObject(oldBs);
        Object ap = oldMo.getValue("additionalParameters");
        Object mp = oldMo.getValue("metaParameters");

        MetaObject newMo = config.newMetaObject(newBs);
        newMo.setValue("additionalParameters", ap);
        newMo.setValue("metaParameters", mp);
    }
}
