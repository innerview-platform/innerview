package com.innerview.spring.core.config;

import com.innerview.spring.entity.InterviewEvent;
import com.innerview.spring.repository.OutboxRepository;
import com.innerview.spring.service.InterviewNotificationService;
import com.interviews.notification.publisher.NotificationPublisher;
import com.innerview.spring.worker.EmailNotificationWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;


import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Spring configuration that wires the full notification pipeline.
 *
 * Dependency graph:
 *
 * inAppQueue  ──► inAppExecutor  (Ticket 4 — SSE worker)
 * emailQueue  ──► emailExecutor  ──► EmailNotificationWorker (x Threads)
 * ├── OutboxRepository (DynamoDB)
 * └── SesClient
 *
 * NotificationPublisher.publishEvent()
 * ├── routes IN_APP  ──► inAppQueue
 * └── routes EMAIL   ──► emailQueue
 */
@Configuration
public class NotificationServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceConfig.class);

    // 1. Dynamically read machine cores
    private static final int CORES = Runtime.getRuntime().availableProcessors();

    // 2. Scale threads for I/O bound tasks (4x cores for high network throughput)
    private static final int POOL_SIZE = CORES * 4;

    // 3. Queue buffer size to absorb sudden traffic spikes safely
    private static final int QUEUE_CAPACITY = 500;

    // ── Queues ────────────────────────────────────────────────────────────────

    /**
     * Dedicated queue for in-app (SSE) notifications.
     */
    @Bean
    public LinkedBlockingQueue<InterviewEvent> inAppQueue() {
        return new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    }

    /**
     * Dedicated queue for email notifications.
     */
    @Bean
    public LinkedBlockingQueue<InterviewEvent> emailQueue() {
        return new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    }

    // ── Thread pools ──────────────────────────────────────────────────────────

    /**
     * Permanently warm thread pool for in-app (SSE) delivery.
     * Uses graceful shutdown to prevent dropping messages during deployments.
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor inAppExecutor(LinkedBlockingQueue<InterviewEvent> inAppQueue) {
        return buildWarmPool(inAppQueue, "InApp");
    }

    /**
     * Permanently warm thread pool for email delivery.
     * Uses graceful shutdown to prevent dropping emails mid-flight during deployments.
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor emailExecutor(
            LinkedBlockingQueue<InterviewEvent> emailQueue,
            OutboxRepository outboxRepository,
            SesClient sesClient) {

        ThreadPoolExecutor executor = buildWarmPool(emailQueue, "Email");

        // Submit one EmailNotificationWorker per thread.
        for (int i = 0; i < POOL_SIZE; i++) {
            executor.submit(new EmailNotificationWorker(emailQueue, outboxRepository, sesClient));
        }

        log.info("emailExecutor started with {} permanently warm threads", POOL_SIZE);
        return executor;
    }

    // ── Publisher (router) ────────────────────────────────────────────────────

    @Bean
    public NotificationPublisher notificationPublisher(
            LinkedBlockingQueue<InterviewEvent> inAppQueue,
            LinkedBlockingQueue<InterviewEvent> emailQueue) {
        return new InterviewNotificationService(inAppQueue, emailQueue);
    }

    // ── Infrastructure beans ──────────────────────────────────────────────────

    @Bean
    public OutboxRepository outboxRepository(DynamoDbClient dynamoDbClient) {
        return new OutboxRepository(dynamoDbClient);
    }

    /**
     * SES client with explicit timeouts.
     * apiCallTimeout = 10s       — total budget including retries
     * apiCallAttemptTimeout = 5s — per-attempt budget
     */
    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5))
                        .build())
                .build();
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Creates a permanently warm, fixed-size thread pool.
     */
    private ThreadPoolExecutor buildWarmPool(LinkedBlockingQueue<InterviewEvent> queue, String namePrefix) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                POOL_SIZE,                     // corePoolSize  — always-warm threads
                POOL_SIZE,                     // maxPoolSize   — fixed; same as core
                0L, TimeUnit.MILLISECONDS,     // keepAlive     — unused because core == max
                queue,                         // the channel's dedicated queue
                new CustomizableThreadFactory(namePrefix + "-Worker-"), // Names threads for clean logging
                new ThreadPoolExecutor.CallerRunsPolicy()  // back-pressure on overflow
        );

        log.info("Built warm pool '{}': coreSize={} maxSize={} queueCapacity={}",
                namePrefix, POOL_SIZE, POOL_SIZE, QUEUE_CAPACITY);
        return executor;
    }
}