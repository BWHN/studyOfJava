package org.study.java.solution.interface_retry;

import java.util.concurrent.TimeoutException;

public class RetryByLoop {
    private static final int RETRY_TIMES = 3;
    public static void main(String[] args) throws InterruptedException {
        MayFailureInterface mayFailureInterface = new MayFailureInterface();
        int i = 0;
        while (i++ < RETRY_TIMES) {
            try {
                mayFailureInterface.doSomething();
                break;
            } catch (TimeoutException e) {
                System.out.println("timeout, retry times: " + i);
                Thread.sleep(1000); // 延迟1秒后重试
            }
        }
    }
}
