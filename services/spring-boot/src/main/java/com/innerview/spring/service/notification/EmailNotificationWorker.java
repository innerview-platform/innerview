package com.innerview.spring.service.notification;

import com.innerview.spring.entity.scheduleNotification;
import com.innerview.spring.entity.OutboxRecord;
import com.innerview.spring.enums.Channel;
import com.innerview.spring.enums.OutboxStatus;
import com.innerview.spring.repository.OutboxRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;



@AllArgsConstructor
public class EmailNotificationWorker  implements  Runnable{

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationWorker.class);
    private final OutboxRepository outboxRepository;
    private final SesClient sesClient;

    static final int  ATTEMPTS_THRESHOLD     = 3;
    static final long BACKOFF_1ST_MS     = 30_000L;   // 30 seconds
    static final long BACKOFF_2ND_MS     = 120_000L;  // 2 minutes

    LinkedBlockingQueue<scheduleNotification> emailQueue;

    private static final String FROM_ADDRESS = "notifications@interviews.com";

    @Override
    public void run() {
        log.info("EmailNotificationWorker started on thread {}", Thread.currentThread().getName());

        while (!Thread.currentThread().isInterrupted()) {
            scheduleNotification event = null;
            try {
                event = emailQueue.take();
                processEvent(event);

            } catch (InterruptedException e) {
                // Graceful shutdown — restore interrupt flag and exit the loop
                Thread.currentThread().interrupt();
                log.info("EmailNotificationWorker interrupted — shutting down");

            } catch (Exception e) {
                // Never let a single bad event kill the thread
                log.error("Unexpected error processing email event={} — continuing loop",
                        event != null ? event.getEventId() : "null", e);
            }
        }

    }

    private void processEvent(scheduleNotification event) {
        if (event == null) {
            log.error("processing null event (error in the queue passing) ");
            return;
        }

        if (!event.getChannel().name().equals(Channel.EMAIL.name())) {
            log.error("email worker {} cannot process this event  ");
            return;
        }

        long now = System.currentTimeMillis();

        // this is the current new state of the task in memory
        OutboxRecord record = new OutboxRecord(event.getEventId(), event.getChannel().name(), now);

        // writes it if it never already exists
        boolean written = outboxRepository.putPending(record);
        if (!written) {
            log.error("Outbox write failed for eventId={} — skipping delivery", event.getEventId());
            return;
        }


        //history of this task
        Optional<OutboxRecord> existing = outboxRepository.findByKey(event.getEventId(), event.getChannel().name());


        // check idempotency
        if (existing.isPresent()) {

            OutboxRecord stored = existing.get();
            if(stored.getSesMessageId() != null && stored.getStatus().name().equals(OutboxStatus.SENT.name()) ){
                log.info("Email already sent (sesMessageId={}), skipping: eventId={}",
                        stored.getSesMessageId(), stored.getEventId());
                return;
            }

            record.setAttempts(stored.getAttempts());
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = outboxRepository.acquireSendingLock(event.getEventId(), event.getChannel().name(), now);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error acquiring SENDING lock for eventId={} — skipping", event.getEventId(), e);
            return;
        }

        if (!lockAcquired) {
            // Another thread (e.g. the poller) already owns this record — skip entirely
            log.debug("SENDING lock not acquired for eventId={} — another thread owns it", event.getEventId());
            return;
        }

        //  Step 4: Call SES message is ok to sent
        attemptSesDelivery(event, record);

    }

    private SendEmailRequest buildSesRequest(scheduleNotification event) {
        // Extract email-specific fields from the flexible payload map
        // Callers are expected to populate these when channel = EMAIL
        String subject = (String) event.getPayload().getOrDefault("subject", "Interview Notification");
        String body    = (String) event.getPayload().getOrDefault("body",    "You have a new interview notification.");
        String toEmail = (String) event.getPayload().getOrDefault("toEmail", event.getRecipientEmail());

        return SendEmailRequest.builder()
                .source(FROM_ADDRESS)
                .destination(Destination.builder().toAddresses(toEmail).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .text(Content.builder().data(body).charset("UTF-8").build())
                                .build())
                        .build())
                .build();
    }


    private void attemptSesDelivery(scheduleNotification event, OutboxRecord record){


        SendEmailRequest sendEmailRequest= buildSesRequest(event);

        String channel = event.getChannel().name();

        String eventId = event.getEventId();
        try {

            //happy path
            SendEmailResponse response=sesClient.sendEmail(sendEmailRequest);
            String sesMessageId = response.messageId();
            outboxRepository.markSent(eventId,channel,sesMessageId);
            log.info("Email delivered: eventId={} sesMessageId={}", eventId, sesMessageId);
        }catch (SesException | software.amazon.awssdk.core.exception.SdkClientException e){

            //persist first for safety

            int newAttempts = record.getAttempts()+1;

            if(newAttempts >= ATTEMPTS_THRESHOLD){
                // TO DEAD LATER QUEUE
                outboxRepository.markDead(eventId,channel);
                log.error("Email DEAD after {} attempts: eventId={} error={}",
                        ATTEMPTS_THRESHOLD, eventId, e.getMessage());
            }else {
                long additionTime= newAttempts == 1 ?  BACKOFF_1ST_MS : BACKOFF_2ND_MS;
                long nextTime = additionTime + System.currentTimeMillis();


                outboxRepository.markPendingForRetry(eventId, channel, newAttempts , nextTime );
                log.warn("Email delivery failed (attempt {}/{}): eventId={} nextRetry=+{}s error={}",
                        newAttempts, ATTEMPTS_THRESHOLD, eventId, additionTime / 1000, e.getMessage());



            }

        }

    }



}
