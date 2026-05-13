package com.innerview.spring.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerview.spring.entity.ScheduleNotification;
import com.innerview.spring.repository.InAppNotificationRepository;
import com.innerview.spring.repository.OutboxRepository;
import com.innerview.spring.service.GoogleApiService;
import com.innerview.spring.service.notification.EmailNotificationWorker;
import com.innerview.spring.service.notification.InAppNotificationWorker;
import com.innerview.spring.service.notification.SseEmitterRegistry;
import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Spring configuration that wires the full notification pipeline.
 *
 * <p>Dependency graph:
 *
 * <p>inAppQueue ──► inAppExecutor (Ticket 4 — SSE worker) emailQueue ──► emailExecutor ──►
 * EmailNotificationWorker (x Threads) ├── OutboxRepository (DynamoDB) └── SesClient
 *
 * <p>NotificationPublisher.publishEvent() ├── routes IN_APP ──► inAppQueue └── routes EMAIL ──►
 * emailQueue
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

  /** Dedicated queue for in-app (SSE) notifications. */
  @Bean
  public LinkedBlockingQueue<ScheduleNotification> inAppQueue() {
    return new LinkedBlockingQueue<>(QUEUE_CAPACITY);
  }

  /** Dedicated queue for email notifications. */
  @Bean
  public LinkedBlockingQueue<ScheduleNotification> emailQueue() {
    return new LinkedBlockingQueue<>(QUEUE_CAPACITY);
  }

  // ── Thread pools ──────────────────────────────────────────────────────────

  /**
   * Permanently warm thread pool for in-app (SSE) delivery. Uses graceful shutdown to prevent
   * dropping messages during deployments.
   */
  @Bean(destroyMethod = "shutdown")
  public ThreadPoolExecutor inAppExecutor(
      LinkedBlockingQueue<ScheduleNotification> inAppQueue,
      OutboxRepository outboxRepository,
      InAppNotificationRepository inAppNotificationRepository,
      SseEmitterRegistry sseEmitterRegistry,
      ObjectMapper objectMapper) {

    ThreadPoolExecutor executor = buildWarmPool("InApp");

    for (int i = 0; i < POOL_SIZE; i++) {
      executor.submit(
          new InAppNotificationWorker(
              outboxRepository,
              inAppNotificationRepository,
              sseEmitterRegistry,
              objectMapper,
              inAppQueue));
    }

    return executor;
  }

  /**
   * Permanently warm thread pool for email delivery. Uses graceful shutdown to prevent dropping
   * emails mid-flight during deployments.
   */
  @Bean
  public ThreadPoolExecutor emailExecutor(
      OutboxRepository outboxRepository,
      JavaMailSender mailSender,
      GoogleApiService googleApiService,
      LinkedBlockingQueue<ScheduleNotification> emailQueue) {

    ThreadPoolExecutor executor = buildWarmPool("Email");

    for (int i = 0; i < POOL_SIZE; i++) {
      // 2. Pass googleApiService as the second argument
      executor.submit(
          new EmailNotificationWorker(outboxRepository, googleApiService, mailSender, emailQueue));
    }

    return executor;
  }

  // ── Infrastructure beans ──────────────────────────────────────────────────

  @Bean
  public OutboxRepository outboxRepository(DynamoDbClient dynamoDbClient) {
    return new OutboxRepository(dynamoDbClient);
  }

  @Value("${aws.dynamodb.endpoint}")
  private String dynamoEndpoint;

  @Bean
  public DynamoDbClient dynamoDbClient() {
    log.info(">>> DynamoDB endpoint value: '{}'", dynamoEndpoint);
    return DynamoDbClient.builder()
        .endpointOverride(URI.create(dynamoEndpoint))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
        .build();
  }

  // ── Factory ───────────────────────────────────────────────────────────────

  /** Creates a permanently warm, fixed-size thread pool. */
  private ThreadPoolExecutor buildWarmPool(String namePrefix) {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            POOL_SIZE, // corePoolSize
            POOL_SIZE, // maxPoolSize
            0L,
            TimeUnit.MILLISECONDS, // keepAlive
            new LinkedBlockingQueue<>(POOL_SIZE),
            new CustomizableThreadFactory(namePrefix + "-Worker-"),
            new ThreadPoolExecutor.CallerRunsPolicy());

    log.info("Built warm pool '{}': coreSize={} maxSize={}", namePrefix, POOL_SIZE, POOL_SIZE);
    return executor;
  }
}
