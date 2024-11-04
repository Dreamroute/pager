package com.github.dreamroute.pager.starter.interceptor;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat.Name;
import com.github.dreamroute.pager.starter.anno.Pager;
import com.github.dreamroute.pager.starter.anno.PagerContainer;
import com.github.dreamroute.pager.starter.anno.PagerContainerBaseInfo;
import com.github.dreamroute.pager.starter.api.PageRequest;
import lombok.extern.slf4j.Slf4j;
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.github.dreamroute.pager.starter.interceptor.ProxyUtil.getOriginObj;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.repeat;

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
@Slf4j
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class PagerInterceptor implements Interceptor, ApplicationListener<ContextRefreshedEvent> {

    private final ConcurrentHashMap<String, PagerContainerBaseInfo> pagerContainer = new ConcurrentHashMap<>();

    // 单表
    private static final int SINGLE = 1;
    private static final String COUNT_NAME = "_$count$_";
    private static final String SELECT = "SELECT ";
    private static final String WHERE = " WHERE ";
    private static final String FROM = " FROM ";
    private static final String DISTINCT = " DISTINCT ";

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
                            PagerContainerBaseInfo container = new PagerContainerBaseInfo();
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

        Object param = invocation.getArgs()[1];
        // 如果参数不加@Param，那么这里是单个，如果加了，那么这里是个Map，取其一即可
        Object objParam = param;
        String paramAlias = null;

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        // 如果是@Param风格，那么需要获取到对象参数以及@Param的value
        if (param instanceof ParamMap) {
            IllegalArgumentException ex = new IllegalArgumentException("接口" + ms.getId() + "参数有误, 分页接口参数必有且仅能有一个，并且是继承了PageRequest的，需要把多个参数封装在一个对象中");
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

        PagerContainerBaseInfo pc = pagerContainer.get(ms.getId());

        // 拦截请求的条件：1. @Page标记接口，2.参数是：PageRequest
        if (pc == null || !(objParam instanceof PageRequest)) {
            return invocation.proceed();
        }

        BoundSql boundSql = ms.getBoundSql(param);
        String beforeSql = boundSql.getSql();
        PagerContainer p = parseSql(beforeSql, ms.getId());
        List<ParameterMapping> beforePmList = boundSql.getParameterMappings();
        p.setOriginPmList(beforePmList);
        List<ParameterMapping> afterPmList = parseParameterMappings(config, beforePmList, paramAlias, p.isSingleTable());
        p.setAfterPmList(afterPmList);

        Executor executor = (Executor) (getOriginObj(invocation.getTarget()));
        Transaction transaction = executor.getTransaction();

        // 处理统计信息
        BoundSql countBoundSql = new BoundSql(config, p.getCountSql(), p.getOriginPmList(), param);
        copyProps(boundSql, countBoundSql, config);
        MappedStatement m = new Builder(config, ms.getId() + "(分页统计)", new StaticSqlSource(config, p.getCountSql()), SqlCommandType.SELECT).build();
        StatementHandler countHandler = config.newStatementHandler(executor, m, param, RowBounds.DEFAULT, null, countBoundSql);
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
            BoundSql bizBoundSql = new BoundSql(config, p.getAfterSql(), p.getAfterPmList(), param);
            copyProps(boundSql, bizBoundSql, config);
            StatementHandler bizHandler = config.newStatementHandler(executor, ms, param, RowBounds.DEFAULT, null, bizBoundSql);
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

    private List<ParameterMapping> parseParameterMappings(Configuration config, List<ParameterMapping> pmList, String alias, boolean isSingleTable) {
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
        if (!isSingleTable) {
            result.addAll(ofNullable(pmList).orElseGet(ArrayList::new));
        }
        return result;
    }

    private PagerContainer parseSql(String sql, String id) {
        PagerContainerBaseInfo container = pagerContainer.get(id);
        PagerContainer resp = BeanUtil.copyProperties(container, PagerContainer.class);
        String afterSql;
        SQLStatement statement = SQLUtils.parseSingleMysqlStatement(sql);

        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        statement.accept(visitor);
        Set<Name> names = visitor.getTables().keySet();
        List<String> tableList = names.stream().map(Name::getName).collect(Collectors.toList());

        MySqlSelectQueryBlock q = ((MySqlSelectQueryBlock) (((SQLSelectStatement) statement).getSelect().getQuery()));
        String colums = q.getSelectList().stream().map(SQLSelectItem::toString).collect(Collectors.joining(","));

        String from = q.getFrom().toString();
        String where = q.getWhere() != null ? q.getWhere().toString() : "";
        String wh = StringUtils.isNotBlank(where) ? (WHERE + where) : "";
        int orderByColums = ofNullable(q.getOrderBy())
                .map(SQLOrderBy::getItems)
                .orElseGet(ArrayList::new)
                .size();
        if (orderByColums > 1) {
            throw new IllegalArgumentException("分页插件不支持多个排序字段!");
        }
        String orderBy = q.getOrderBy() != null ? q.getOrderBy().toString() : "";

        if (CollUtil.isNotEmpty(tableList) && tableList.size() == SINGLE) {
            String countSql = SELECT + "COUNT(*) " + COUNT_NAME + " " + FROM + from + wh;
            resp.setCountSql(countSql);
            afterSql = sql + " LIMIT ?, ?";
            resp.setSingleTable(true);
        } else {
            String distinctBy = resp.getDistinctBy();

            String alias = "";
            String masterTableId = "";

            if (distinctBy.indexOf('.') != -1) {
                alias = distinctBy.split("\\.")[0];
                masterTableId = distinctBy.split("\\.")[1];
            }


            String subQuery1 = SELECT + DISTINCT + distinctBy + FROM + from + wh + " " + orderBy + " LIMIT ?, ?";
            String newAlias = "_" + alias;
            subQuery1 = subQuery1.replaceAll("\\bAS\\s+" + alias + "\\b", "AS " + newAlias)
                    .replaceAll("\\b" + alias + "\\b", newAlias);

            String innerAlias = "__" + repeat(alias, 2);
            String subQuery2 = SELECT + innerAlias + "." + masterTableId + FROM + "(" + subQuery1 + ") " + innerAlias;
            String andWh = StringUtils.isNotBlank(wh) ? (" AND " + where) : "";
            afterSql = SELECT + colums + FROM + from + WHERE + distinctBy + " IN (" + subQuery2 + ")" + andWh + " " + orderBy;

            String count = SELECT + "count(DISTINCT " + distinctBy + ") " + COUNT_NAME + FROM + from + wh;
            resp.setCountSql(count);
        }
        resp.setAfterSql(afterSql);
        return resp;
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
