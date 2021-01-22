package com.github.dreamroute.pager.starter;

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
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.apache.ibatis.mapping.SqlCommandType.SELECT;

/**
 * 分页插件
 *
 * @author w.dehi
 */
@Intercepts(@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
public class PagerInterceptor implements Interceptor {
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
        BoundSql boundSql = ms.getBoundSql(param);
        String sql = boundSql.getSql();
        String countSql = "select count(*) c from (" + sql + ") t";
        Executor executor = (Executor) (invocation.getTarget());
        Transaction transaction = executor.getTransaction();
        Connection conn = transaction.getConnection();
        PreparedStatement ps = conn.prepareStatement(countSql);
        BoundSql countBoundSql = new BoundSql(config, countSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
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

        sql = "select * from (" + sql + ") t limit ?, ?";
        MetaObject moms = config.newMetaObject(ms);

        ParameterMapping startMapping = new ParameterMapping.Builder(config, "pageNum", int.class).build();
        ParameterMapping limitMapping = new ParameterMapping.Builder(config, "pageSize", int.class).build();

        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        List<ParameterMapping> pmList = new ArrayList<>(ofNullable(parameterMappings).orElseGet(ArrayList::new));
        pmList.add(startMapping);
        pmList.add(limitMapping);

        // sql和pm都需要设置在ms里，设置在boundsql里没用，因为使用的是ms里的sql和pm
        moms.setValue("sqlSource.sqlSource.sql", sql);
        moms.setValue("sqlSource.sqlSource.parameterMappings", pmList);

        Object result = invocation.proceed();
        List<?> ls = (List<?>) result;
        container.addAll(ls);

        return container;
    }
}