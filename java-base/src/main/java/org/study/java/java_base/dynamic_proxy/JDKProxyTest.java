package org.study.java.java_base.dynamic_proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// 目标接口
interface Worker {
      void work();
}
// 目标类
class Programmer implements Worker {
      @Override
      public void work() {
            System.out.println("coding");
      }
}
// 目标对象功能增强
class WorkHandler implements InvocationHandler {
      private Object target;
      public WorkHandler(Object target) {
            this.target = target;
      }
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("before work...");
            Object invoke = method.invoke(target, args);   // 注意这里是 target 而非 proxy
            System.out.println("after work...");
            return invoke;
      }
}
// 测试类
// -Dsun.misc.ProxyGenerator.saveGeneratedFiles=true
public class JDKProxyTest {
      public static void main(String[] args) {
            Programmer programmer = new Programmer();
            Worker proxy = (Worker) Proxy.newProxyInstance(
                                        programmer.getClass().getClassLoader(),
                                        programmer.getClass().getInterfaces(),
                                        new WorkHandler(programmer));
            proxy.work();
      }
}
