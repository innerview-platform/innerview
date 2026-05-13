package com.innerview.spring.service.impl;

import com.innerview.spring.entity.ScheduleNotification;
import com.innerview.spring.service.NotificationPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;


/**
 *this is the service responsible for the routing to the queues
 * it is non blocking drop the message in the queue based on the type of it
 * if there is no size in queues we drop message in dynamodb
 *  */
public class NotificationService implements NotificationPublisherService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final LinkedBlockingQueue<ScheduleNotification> inAppQueue;
    private final LinkedBlockingQueue<ScheduleNotification> emailQueue;

    public NotificationService(
            LinkedBlockingQueue<ScheduleNotification> inAppQueue,
            LinkedBlockingQueue<ScheduleNotification> emailQueue) {
        this.inAppQueue = inAppQueue;
        this.emailQueue = emailQueue;
    }

    /**
     * Routes the event using offer() (NON-BLOCKING).
     * * If the queue is under 500 capacity, it enters RAM instantly (0.1ms).
     * If the queue is full, offer() returns false. We simply drop the memory handoff,
     * knowing the DynamoDB Poller will safely pick it up later.
     */
    @Override
    public void publishEvent(ScheduleNotification event) {
        if (event == null) {
            log.warn("publishEvent() called with null event — ignoring");
            return;
        }

        switch (event.getChannel()) {
            case IN_APP -> {
                boolean accepted = inAppQueue.offer(event);
                if (accepted) {
                    log.debug("Routed event {} to inAppQueue", event.getEventId());
                } else {
                    log.warn("inAppQueue full! Event {} dropped from RAM. Deferring to DynamoDB Poller.", event.getEventId());
                    // logic here inshallah dropping message in disk db and the schedulled sweeper will send them
                }
            }
            case EMAIL -> {
                boolean accepted = emailQueue.offer(event);
                if (accepted) {
                    log.debug("Routed event {} to emailQueue", event.getEventId());
                } else {
                    log.warn("emailQueue full! Event {} dropped from RAM. Deferring to DynamoDB Poller.", event.getEventId());
                    // logic here of dropping it
                }
            }
            default -> log.warn("Unknown channel '{}' on event {} — dropping silently",
                    event.getChannel(), event.getEventId());
        }
    }
}