/*
 The MIT License (MIT)

 Copyright (c) 2016 342252328@qq.com

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
*/
package com.github.dreamroute.pager.starter.interceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.plugin.Plugin;

public final class ProxyUtil {

    private ProxyUtil() {} // private constructor

    /**
     * <p>Get the original target object from nested MyBatis plugin proxies.</p>
     *
     * <p>Other JDK dynamic proxies are returned as-is because their invocation handlers do not
     * necessarily expose a target object.</p>
     *
     * @param target proxy-object
     * @return original target object
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOriginObj(T target) {
        Object origin = target;
        while (origin != null && Proxy.isProxyClass(origin.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(origin);
            if (!(handler instanceof Plugin)) {
                break;
            }
            MetaObject metaObject = SystemMetaObject.forObject(handler);
            origin = metaObject.getValue("target");
        }
        return (T) origin;
    }
}
