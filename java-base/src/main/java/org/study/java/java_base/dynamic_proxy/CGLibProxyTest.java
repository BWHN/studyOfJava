package org.study.java.java_base.dynamic_proxy;

import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

// 目标类
class Dao {
    public void select() {
        System.out.println("select success");
    }
}
// 方法增强类
class DaoMethodInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("before invoke");
        // 这里和 InvocationHandler 不同，直接使用方法代理
        Object result = methodProxy.invokeSuper(o, objects);
        System.out.println("after invoke");
        return result;
    }
}
// 测试类
public class CGLibProxyTest {
    public static void main(String[] args) {
//        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:/code/studyOfJava");
        DaoMethodInterceptor methodInterceptor = new DaoMethodInterceptor();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Dao.class);
        enhancer.setCallback(methodInterceptor);
        // 相较于 JDK 动态代理，这里我们并不需要创建目标类对象
        Dao dao = (Dao) enhancer.create();
        dao.select();
    }
}
