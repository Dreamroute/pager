package com.github.dreamroute.pager.starter.sample.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dreamroute.pager.starter.interceptor.ProxyUtil;
import java.lang.reflect.Proxy;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;

/**
 * @author w.dehi
 */
class ProxyUtilTest {

    @Test
    void unwrapsNestedMybatisPluginProxiesWithoutAccessingJdkPrivateFields() {
        Executor originalExecutor = (Executor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {Executor.class},
                (proxy, method, args) -> null
        );
        Executor firstProxy = (Executor) Plugin.wrap(originalExecutor, new QueryInterceptor());
        Executor nestedProxy = (Executor) Plugin.wrap(firstProxy, new QueryInterceptor());

        assertThat(ProxyUtil.getOriginObj(nestedProxy)).isSameAs(originalExecutor);
    }

    @Intercepts({
            @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
    })
    private static class QueryInterceptor implements Interceptor {

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            return invocation.proceed();
        }
    }
}
