package org.study.java.solution.interface_retry;

import java.util.concurrent.TimeoutException;

public class MayFailureInterface {
    private int i = 0;
    public void doSomething() throws TimeoutException {
        if (i++ < 2) {  // 模拟失败
            throw new TimeoutException();
        }
        System.out.println("invoke success");
    }
}
