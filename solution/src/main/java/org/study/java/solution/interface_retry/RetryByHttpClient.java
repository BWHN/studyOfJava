package org.study.java.solution.interface_retry;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.concurrent.TimeoutException;

public class RetryByHttpClient {
    private static final int RETRY_TIMES = 3;
    public static void main(String[] args) {
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
        HttpUriRequest httpUriRequest = new HttpUriRequest();
        httpClient.ex()
    }

}
