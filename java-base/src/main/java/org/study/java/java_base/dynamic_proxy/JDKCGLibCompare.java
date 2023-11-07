package org.study.java.java_base.dynamic_proxy;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.util.StopWatch;

import java.lang.reflect.Proxy;

public class JDKCGLibCompare {
    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        // 记录JDK动态代理执行时间
        testJDK(stopWatch);
        // 记录CGLib动态代理执行时间
        testCGLib(stopWatch);
    }
    private static void testJDK(StopWatch stopWatch) {
        stopWatch.start("JDK 创建代理对象");
        Programmer programmer = new Programmer();
        Worker proxy = (Worker) Proxy.newProxyInstance(
                programmer.getClass().getClassLoader(),
                programmer.getClass().getInterfaces(),
                new WorkHandler(programmer));
        stopWatch.stop();
        for (int i = 1; i <= 15; i++) {
            stopWatch.start("执行方法第" + i + "次数");
            proxy.work();
            stopWatch.stop();
        }
        System.out.println(stopWatch.prettyPrint());
    }
    private static void testCGLib(StopWatch stopWatch) {
        stopWatch.start("CGLib 创建代理对象");
        DaoMethodInterceptor methodInterceptor = new DaoMethodInterceptor();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Dao.class);
        enhancer.setCallback(methodInterceptor);
        Dao dao = (Dao) enhancer.create();
        stopWatch.stop();
        for (int i = 1; i <= 15; i++) {
            stopWatch.start("执行方法第" + i + "次数");
            dao.select();
            stopWatch.stop();
        }
        System.out.println(stopWatch.prettyPrint());
    }
}
