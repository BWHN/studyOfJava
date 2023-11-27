package org.study.java.solution.interface_retry;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/retry")
public class RetryTest {

    private static final int RETRY_TIMES = 3;

    /**
     * 模拟超时接口
     */
    @GetMapping("/mayFailure")
    public void mayFailure() {
        new MayFailureInterface().doSomething();
    }

    /**
     * 循环重试
     */
    @GetMapping("/retryByLoop")
    public void retryByLoop() throws InterruptedException {
//        MayFailureInterface mayFailureInterface = new MayFailureInterface();
//        int i = 0;
//        while (i++ < RETRY_TIMES) {
//            try {
//                mayFailureInterface.doSomething();
//                break;
//            } catch (TimeoutException e) {
//                System.out.println("timeout, retry times: " + i);
//                TimeUnit.SECONDS.sleep(1); // 延迟1秒后重试
//            }
//        }
    }

    @GetMapping("/retryByClient")
    public void retryByClient() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setRetryHandler(new DefaultHttpRequestRetryHandler(RETRY_TIMES, true))
                .setConnectionTimeToLive(3, TimeUnit.SECONDS)
                .build();
        try {
            HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/retry/mayFailure");
            httpClient.execute(httpGet);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
