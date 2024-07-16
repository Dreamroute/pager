package com.github.dreamroute.pager.starter.jsqlparser;

import com.github.dreamroute.pager.starter.interceptor.PagerInterceptor;
import com.github.dreamroute.pager.starter.interceptor.ProxyUtil;
import org.junit.jupiter.api.Test;

class ProxyUtilTest {

    @Test
    void getOriginObjTest() {
        Object plugin = new PagerInterceptor().plugin(new Audi());
        Object originObj = ProxyUtil.getOriginObj(plugin);
        System.err.println(originObj);
    }

}

interface Car {
    void drive();
}

class Audi implements Car {
    @Override
    public void drive() {
    }
}