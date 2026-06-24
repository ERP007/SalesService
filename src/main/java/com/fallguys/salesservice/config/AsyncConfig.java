package com.fallguys.salesservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 실행 설정. outbox 즉시 발행을 커밋(요청) 스레드에서 떼어내기 위한 전용 풀.
 * 풀 포화 시 CallerRuns로 일시 동기 강등(무제한 스레드 생성 방지). 발행 누락은 폴러가 수렴.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor outboxRelayExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("outbox-relay-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
