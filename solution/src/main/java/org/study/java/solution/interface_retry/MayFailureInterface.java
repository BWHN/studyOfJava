package org.study.java.solution.interface_retry;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MayFailureInterface {
    private int i = 0;
    public void doSomething() {
        if (i++ < 2) {  // 模拟超时
            try {
                log.info("receive request");
                TimeUnit.SECONDS.sleep(600);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("invoke success");
    }
}
