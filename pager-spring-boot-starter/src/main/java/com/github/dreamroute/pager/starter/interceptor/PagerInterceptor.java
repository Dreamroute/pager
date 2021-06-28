package com.github.dreamroute.pager.starter.interceptor;

import cn.hutool.core.annotation.AnnotationUtil;
import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.anno.PagerContainer;
import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.exception.PaggerException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
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

import java.sql.Connection;
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

import static com.github.dreamroute.pager.starter.interceptor.ProxyUtil.getOriginObj;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 分页插件，原理：通过注解标注需要分页的接口方法，拦截该方法，抽取原生sql，然后做3个动作：
 * 1、根据原生sql语句生成一个统计的sql，并且执行查询，获得统计结果；
 * 2、改写原生sql，加上分页参数，执行查询操作；
 * 3、将统计信息和分页结果合并成返回信息；
 *
 * @author w.dehi
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class PagerInterceptor implements Interceptor, ApplicationListener<ContextRefreshedEvent> {

    private final ConcurrentHashMap<String, PagerContainer> pagerContainer = new ConcurrentHashMap<>();

    /**
     * 单表
     */
    private static final int SINGLE = 1;
    private static final String COUNT_NAME = "_$count$_";
    private static final String WHERE = " WHERE ";
    private static final String FROM = " FROM ";

    private Configuration config;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        SqlSessionFactory sqlSessionFactory = event.getApplicationContext().getBean(SqlSessionFactory.class);
        // 将此方法移动到Spring容器初始化之后执行的原因是：如果放在下方的intercept方法中来执行，
        // 那么就会有并发问题（获取ms的sqlSource然后修改sqlSource），那么就需要对该方法加锁，影响性能
        config = sqlSessionFactory.getConfiguration();
        parsePagerContainer();
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object param = args[1];
        PagerContainer pc = pagerContainer.get(ms.getId());
        // 拦截请求的条件：1. @Page标记接口，2.参数是：PageRequest
        if (pc == null || !(param instanceof PageRequest)) {
            return invocation.proceed();
        }

        Executor executor = (Executor) (getOriginObj(invocation.getTarget()));
        Transaction transaction = executor.getTransaction();

        Connection conn = transaction.getConnection();
        String count = pc.getCountSql();
        PreparedStatement ps = conn.prepareStatement(count);
        BoundSql countBoundSql = new BoundSql(config, count, pc.getOriginPmList(), param);
        ParameterHandler parameterHandler = config.newParameterHandler(ms, param, countBoundSql);
        parameterHandler.setParameters(ps);
        ResultSet rs = ps.executeQuery();
        PageContainer<Object> container = new PageContainer<>();
        while (rs.next()) {
            long totle = rs.getLong(COUNT_NAME);
            container.setTotal(totle);
        }
        ps.close();

//        String count = pc.getCountSql();
//        BoundSql countBoundSql = new BoundSql(config, count, pc.getOriginPmList(), param);
//        StatementHandler handler = config.newStatementHandler(executor, ms, param, RowBounds.DEFAULT, null, countBoundSql);
//        Statement stmt = prepareStatement(transaction, handler);
//        List<Object> query = handler.query(stmt, null);
//        System.err.println(query);

//        ParameterHandler parameterHandler = config.newParameterHandler(ms, param, countBoundSql);
//        PageContainer<Object> container = new PageContainer<>();
//        while (rs.next()) {
//            long totle = rs.getLong(COUNT_NAME);
//            container.setTotal(totle);
//        }
//        ps.close();

        PageRequest<?> pr = (PageRequest<?>) param;
        int pageNum = pr.getPageNum();
        int pageSize = pr.getPageSize();

        // 由于不希望在pageRequest中增加start参数，所以limit时改变pageNum来代替start，因此resp的pageNum需要在设置start之前进行设置
//        container.setPageNum(pageNum);
//        container.setPageSize(pr.getPageSize());
//
//        int start = (pageNum - 1) * pageSize;
//        pr.setPageNum(start);
//
//        if (container.getTotal() != 0) {
//            PreparedStatement preparedStatement = conn.prepareStatement(pc.getAfterSql());
//            BoundSql bs = new BoundSql(config, pc.getAfterSql(), pc.getAfterPmList(), param);
//            ParameterHandler ph = config.newParameterHandler(ms, param, bs);
//            ph.setParameters(preparedStatement);
//            ResultSet r = preparedStatement.executeQuery();
//            while (r.next()) {
//                System.err.println(r);
//            }
//            preparedStatement.close();
//
//            Object result = invocation.proceed();
//            List<?> ls = (List<?>) result;
//            container.addAll(ls);
//        }

//        return container;
        return null;
    }

    private Statement prepareStatement(Transaction transaction, StatementHandler handler) throws SQLException {
        Statement stmt;
        stmt = handler.prepare(transaction.getConnection(), transaction.getTimeout());
        handler.parameterize(stmt);
        return stmt;
    }

    /**
     * 这里将pagerContainer进行缓存，由于分页插件的执行sql是固定的，所以可以缓存
     */
    private void parsePagerContainer() {
        parseAnno();
        updateSqlSource();
    }

    private void parseAnno() {
        Collection<Class<?>> mappers = config.getMapperRegistry().getMappers();
        if (mappers != null && !mappers.isEmpty()) {
            for (Class<?> mapper : mappers) {
                String mapperName = mapper.getName();
                stream(mapper.getDeclaredMethods()).filter(method -> AnnotationUtil.hasAnnotation(method, Pager.class)).forEach(method -> {
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

    private void updateSqlSource() {
        this.pagerContainer.keySet().stream().map(config::getMappedStatement).forEach(ms -> {
            PagerContainer pc = this.pagerContainer.get(ms.getId());
            MetaObject mo = config.newMetaObject(ms);
            String beforeSql = (String) mo.getValue("sqlSource.sqlSource.sql");
            String afterSql = parseSql(beforeSql, ms.getId());
            pc.setAfterSql(afterSql);
            @SuppressWarnings("unchecked")
            List<ParameterMapping> beforePmList = (List<ParameterMapping>) mo.getValue("sqlSource.sqlSource.parameterMappings");
            pc.setOriginPmList(beforePmList);
            List<ParameterMapping> afterPmList = parseParameterMappings(config, ms.getId(), beforePmList);
            pc.setAfterPmList(afterPmList);
        });

    }

    private List<ParameterMapping> parseParameterMappings(Configuration config, String id, List<ParameterMapping> pmList) {
        List<ParameterMapping> result = new ArrayList<>(ofNullable(pmList).orElseGet(ArrayList::new));
        result.add(new ParameterMapping.Builder(config, "pageNum", int.class).build());
        result.add(new ParameterMapping.Builder(config, "pageSize", int.class).build());
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
            container.setCountSql("SELECT COUNT(*) " + COUNT_NAME + " FROM (" + sql + ") _$_t");
            String orderBy = ofNullable(body.getOrderByElements()).orElseGet(ArrayList::new).stream().map(Objects::toString).collect(joining(", "));
            orderBy = StringUtils.isNotBlank(orderBy) ? (" ORDER BY " + orderBy) : "";

            afterSql = sql + orderBy + " LIMIT ?, ?";
            container.setSingleTable(true);
        } else {
            String joins = body.getJoins().stream().map(Object::toString).collect(joining(" "));

            String alias = "";
            String distinctBy = container.getDistinctBy();
            if (distinctBy.indexOf('.') != -1) {
                alias = distinctBy.split("\\.")[0];
            }

            // 如果order by不为空，那么子查询的查询列需要将order by列也带上，否则H2会报错（order by列需要在查询列中），MySQL则不会
            String orderBy = "";
            String subQueryColumns = "";
            List<OrderByElement> orderbyList = body.getOrderByElements();
            if (!CollectionUtils.isEmpty(orderbyList)) {
                orderBy = " ORDER BY " + orderbyList.stream().map(Object::toString).collect(joining(", "));

                // order by列和主表id列重复，需要去重
                Set<String> orderbyListStr = orderbyList.stream().map(OrderByElement::getExpression).map(Objects::toString).collect(toSet());
                orderbyListStr.add(distinctBy);
                subQueryColumns = String.join(", ", orderbyListStr);
            }


            String afterFrom = FROM + from + " " + joins + WHERE + where;
            String subQuery = "SELECT DISTINCT " + subQueryColumns + afterFrom;
            String noCondition = "SELECT " + columns + FROM + from + " " + joins + " ";

            String result = noCondition + WHERE + distinctBy + " IN  (SELECT " + distinctBy + " FROM (" + subQuery + orderBy + " LIMIT ?, ?) " + alias + ")";
            if (StringUtils.isNoneBlank(where)) {
                result = result + " AND " + where;
            }
            afterSql = result + orderBy;
            String count = "SELECT count(DISTINCT " + distinctBy + ") " + COUNT_NAME + afterFrom;
            container.setCountSql(count);
        }
        return afterSql;
    }

}