package com.github.dreamroute.pager.starter.interceptor;

import cn.hutool.core.annotation.AnnotationUtil;
import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.anno.PagerContainer;
import com.github.dreamroute.pager.starter.api.PageRequest;
import com.github.dreamroute.pager.starter.exception.PaggerException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
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
import org.apache.ibatis.transaction.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dreamroute.pager.starter.anno.PagerContainer.ID;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.ibatis.mapping.SqlCommandType.SELECT;

/**
 * 分页插件
 *
 * @author w.dehi
 */
@Intercepts(@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
public class PagerInterceptor implements Interceptor {

    private ConcurrentHashMap<String, PagerContainer> pagerContainer;

    /**
     * 单表
     */
    private static final int SINGLE = 1;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object param = args[1];

        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        // 非select不处理
        if (sqlCommandType != SELECT) {
            return invocation.proceed();
        }

        if (!(param instanceof PageRequest)) {
            return invocation.proceed();
        }

        Configuration config = ms.getConfiguration();
        parsePagerContainer(config);

        BoundSql boundSql = ms.getBoundSql(param);
        String sql = boundSql.getSql();

        parseSql(sql, ms.getId());

        Executor executor = (Executor) (invocation.getTarget());
        Transaction transaction = executor.getTransaction();
        Connection conn = transaction.getConnection();
        String count = pagerContainer.get(ms.getId()).getCount();
        PreparedStatement ps = conn.prepareStatement(count);
        BoundSql countBoundSql = new BoundSql(config, count, boundSql.getParameterMappings(), boundSql.getParameterObject());
        ParameterHandler parameterHandler = config.newParameterHandler(ms, boundSql.getParameterObject(), countBoundSql);
        parameterHandler.setParameters(ps);
        ResultSet rs = ps.executeQuery();
        PageContainer<Object> container = new PageContainer<>();
        while (rs.next()) {
            long totle = rs.getLong("c");
            container.setTotal(totle);
        }
        ps.close();

        PageRequest<?> pr = (PageRequest<?>) param;
        int pageNum = pr.getPageNum();
        int pageSize = pr.getPageSize();

        // 由于不希望在pageRequest中增加start参数，所以limit时改变pageNum来代替start，因此resp的pageNum需要在设置start之前进行设置
        container.setPageNum(pageNum);
        container.setPageSize(pr.getPageSize());

        int start = (pageNum - 1) * pageSize;
        pr.setPageNum(start);

        MetaObject moms = config.newMetaObject(ms);

        ParameterMapping startMapping = new ParameterMapping.Builder(config, "pageNum", int.class).build();
        ParameterMapping limitMapping = new ParameterMapping.Builder(config, "pageSize", int.class).build();

        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        List<ParameterMapping> pmList = new ArrayList<>(ofNullable(parameterMappings).orElseGet(ArrayList::new));
        pmList.add(startMapping);
        pmList.add(limitMapping);

        // sql和pm都需要设置在ms里，设置在boundsql里没用，因为使用的是ms里的sql和pm
        moms.setValue("sqlSource.sqlSource.sql", pagerContainer.get(ms.getId()).getSql());
        moms.setValue("sqlSource.sqlSource.parameterMappings", pmList);

        if (container.getTotal() != 0) {
            Object result = invocation.proceed();
            List<?> ls = (List<?>) result;
            container.addAll(ls);
        }

        return container;
    }

    private void parsePagerContainer(Configuration config) {
        if (pagerContainer == null) {
            pagerContainer = new ConcurrentHashMap<>();
            Collection<Class<?>> mappers = config.getMapperRegistry().getMappers();
            if (mappers != null && !mappers.isEmpty()) {
                for (Class<?> mapper : mappers) {
                    String mapperName = mapper.getName();
                    stream(mapper.getDeclaredMethods()).filter(method -> AnnotationUtil.hasAnnotation(method, Pager.class)).forEach(method -> {
                        String dictinctBy = AnnotationUtil.getAnnotationValue(method, Pager.class, "distinctBy");
                        PagerContainer container = new PagerContainer();
                        container.setDistinctColumn(StringUtils.isEmpty(dictinctBy) ? ID : dictinctBy);
                        String in = AnnotationUtil.getAnnotationValue(method, Pager.class, "in");
                        in = ofNullable(in).orElseThrow(() -> new PaggerException("多表分页查询必须要设置@Pager的属性in"));
                        container.setIn(in);
                        pagerContainer.put(mapperName + "." + method.getName(), container);
                    });
                }
            }
        }
    }

    private void parseSql(String sql, String id) {
        try {
            PagerContainer container = pagerContainer.get(id);
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            List<String> tableList = new TablesNamesFinder().getTableList(select);

            if (tableList != null && tableList.size() == SINGLE) {
                container.setCount("SELECT COUNT (*) c FROM (" + sql + ") t");
                sql += " LIMIT ?, ?";
                container.setSql(sql);
            } else {
                PlainSelect body = (PlainSelect) select.getSelectBody();
                String columns = body.getSelectItems().stream().map(Object::toString).collect(joining(","));
                String from = body.getFromItem().toString();
                String joins = body.getJoins().stream().map(Object::toString).collect(joining(","));
                String noCondition = "SELECT " + columns + " FROM " + from + " " + joins + " ";

                String result = noCondition + " WHERE " +  container.getIn() + " IN  (SELECT * FROM (SELECT DISTINCT " +  container.getDistinctColumn() + " from  (" + sql + ") t LIMIT ?, ?) tt)";
                container.setSql(result);

                String count = "SELECT count(" + container.getDistinctColumn() + ") c FROM (SELECT DISTINCT " +  container.getDistinctColumn() + " from (" + sql + ") t) tt";
                container.setCount(count);
            }
        } catch (Exception e) {
            throw new PaggerException("SQL语句异常，你的sql语句是: [" + sql + "]");
        }
    }
}