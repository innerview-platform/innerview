package com.innerview.spring.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;


@Configuration
@EnableAsync
public class AsyncConfig {


    @Bean(name = "redisThreadPool")
    public Executor redisThreadPool() {
        int cpus = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cpus);
        executor.setMaxPoolSize(cpus * 4);
        executor.setQueueCapacity(1_000);
        executor.setThreadNamePrefix("redis-persist-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(false);   // keep core threads warm


        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
