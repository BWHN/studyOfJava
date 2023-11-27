package org.study.java.solution.interface_retry;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.concurrent.TimeoutException;

@RestController("/retry")
public class RetryTest {

    private static final int RETRY_TIMES = 3;

    @GetMapping("/retryByLoop")
    public void retryByLoop() throws InterruptedException {
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

    @GetMapping("/retryByLoop")
    public void retryByClient() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setRetryHandler((response, executionCount, context) -> {
                    if (executionCount > RETRY_TIMES) { // 超过重试次数，则放弃重试
                        return false;
                    }
                    Throwable cause = response.getCause();
                    if (cause instanceof TimeoutException) {  // 遇到超时进行重试
                        return true;
                    }
                    return false; // 其他情况不进行重试
                })
                .build();
    }

}
