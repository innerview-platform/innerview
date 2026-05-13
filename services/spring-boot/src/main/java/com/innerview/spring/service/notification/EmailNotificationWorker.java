package com.innerview.spring.service.notification;

import com.innerview.spring.entity.ScheduleNotification;
import com.innerview.spring.entity.OutboxRecord;
import com.innerview.spring.enums.Channel;
import com.innerview.spring.enums.OutboxStatus;
import com.innerview.spring.repository.OutboxRepository;
import com.innerview.spring.service.GoogleApiService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

@AllArgsConstructor
public class EmailNotificationWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationWorker.class);
    private final OutboxRepository outboxRepository;
    private final GoogleApiService googleApiService;
    private final JavaMailSender mailSender;

    static final int  ATTEMPTS_THRESHOLD = 3;
    static final long BACKOFF_1ST_MS     = 30_000L;
    static final long BACKOFF_2ND_MS     = 120_000L;

    LinkedBlockingQueue<ScheduleNotification> emailQueue;

    // ── Date formatters ───────────────────────────────────────────────────────
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' h:mm a");
    private static final ZoneId            DISPLAY_ZONE = ZoneId.of("UTC");

    @Override
    public void run() {
        log.info("EmailNotificationWorker started on thread {}", Thread.currentThread().getName());

        while (!Thread.currentThread().isInterrupted()) {
            ScheduleNotification event = null;
            try {
                event = emailQueue.take();
                processEvent(event);

                if (event.getChannel() == Channel.EMAIL) {
                    try {
                        String meetLink = googleApiService.createInterviewEvent(event);
                        if (meetLink != null) {
                            log.info("Google Meet link created for eventId={}: {}", event.getEventId(), meetLink);
                        }
                    } catch (Exception e) {
                        log.warn("Google Calendar skipped for eventId={}: {}", event.getEventId(), e.getMessage());
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("EmailNotificationWorker interrupted — shutting down");

            } catch (Exception e) {
                log.error("Unexpected error processing email event={} — continuing loop",
                        event != null ? event.getEventId() : "null", e);
            }
        }
    }

    // ── Process ───────────────────────────────────────────────────────────────

    private void processEvent(ScheduleNotification event) {
        if (event == null) {
            log.error("processing null event (error in the queue passing)");
            return;
        }

        if (!event.getChannel().name().equals(Channel.EMAIL.name())) {
            log.error("email worker cannot process this event");
            return;
        }

        long now = System.currentTimeMillis();
        OutboxRecord record = new OutboxRecord(event.getEventId(), event.getChannel().name(), now);

        boolean written = outboxRepository.putPending(record);
        if (!written) {
            log.error("Outbox write failed for eventId={} — skipping delivery", event.getEventId());
            return;
        }

        Optional<OutboxRecord> existing = outboxRepository.findByKey(event.getEventId(), event.getChannel().name());

        if (existing.isPresent()) {
            OutboxRecord stored = existing.get();
            if (stored.getSesMessageId() != null && stored.getStatus().name().equals(OutboxStatus.SENT.name())) {
                log.info("Email already sent (mockMessageId={}), skipping: eventId={}",
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
            log.debug("SENDING lock not acquired for eventId={} — another thread owns it", event.getEventId());
            return;
        }

        attemptSmtpDelivery(event, record);
    }

    // ── Delivery ──────────────────────────────────────────────────────────────

    private void attemptSmtpDelivery(ScheduleNotification event, OutboxRecord record) {
        String toEmail = (String) event.getPayload().getOrDefault("toEmail", event.getRecipientEmail());
        String subject = (String) event.getPayload().getOrDefault("subject", "Your interview is scheduled — InnerView");
        String channel = event.getChannel().name();
        String eventId = event.getEventId();

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom("innerview@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildHtml(event), true); // true = isHtml

            mailSender.send(mime);

            String mockMessageId = "smtp-" + UUID.randomUUID();
            outboxRepository.markSent(eventId, channel, mockMessageId);
            log.info("Email delivered: eventId={} mockMessageId={}", eventId, mockMessageId);

        } catch (MessagingException | MailException e) {
            int newAttempts = record.getAttempts() + 1;

            if (newAttempts >= ATTEMPTS_THRESHOLD) {
                outboxRepository.markDead(eventId, channel);
                log.error("Email DEAD after {} attempts: eventId={} error={}",
                        ATTEMPTS_THRESHOLD, eventId, e.getMessage());
            } else {
                long additionTime = newAttempts == 1 ? BACKOFF_1ST_MS : BACKOFF_2ND_MS;
                long nextTime     = additionTime + System.currentTimeMillis();
                outboxRepository.markPendingForRetry(eventId, channel, newAttempts, nextTime);
                log.warn("Email delivery failed (attempt {}/{}): eventId={} nextRetry=+{}s error={}",
                        newAttempts, ATTEMPTS_THRESHOLD, eventId, additionTime / 1000, e.getMessage());
            }
        }
    }

    // ── HTML builder ──────────────────────────────────────────────────────────

    private String buildHtml(ScheduleNotification event) {
        // Resolve display values
        String userName  = (String) event.getPayload().getOrDefault("userName",  event.getRecipientEmail());
        String ownerName = event.getOwnerUsername() != null ? event.getOwnerUsername() : "Your Interviewer";
        String roomUrl   = (String) event.getPayload().getOrDefault("roomUrl",   "#");
        String calUrl    = (String) event.getPayload().getOrDefault("calendarUrl","#");
        String manageUrl = (String) event.getPayload().getOrDefault("manageUrl", "#");

        String interviewTime;
        String timezone;
        if (event.getDate() != null) {
            ZonedDateTime zdt = event.getDate().atZone(DISPLAY_ZONE);
            interviewTime = TIME_FMT.format(zdt);
            timezone      = "UTC";
        } else {
            interviewTime = "TBD";
            timezone      = "";
        }

        String duration = event.getDurationMinutes() != null
                ? event.getDurationMinutes().toString()
                : "60";

        // Read the HTML template and substitute placeholders
        String html = HTML_TEMPLATE
                .replace("{{userName}}",          escapeHtml(userName))
                .replace("{{ownerName}}",         escapeHtml(ownerName))
                .replace("{{interviewTime}}",     escapeHtml(interviewTime))
                .replace("{{interviewTimezone}}", escapeHtml(timezone))
                .replace("{{duration}}",          escapeHtml(duration))
                .replace("{{roomUrl}}",           roomUrl)
                .replace("{{calendarUrl}}",       calUrl)
                .replace("{{manageUrl}}",         manageUrl);

        return html;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ── Embedded HTML template ────────────────────────────────────────────────

    private static final String HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="color-scheme" content="dark">
<meta name="supported-color-schemes" content="dark">
<title>Interview Scheduled — InnerView</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500&display=swap');
  body { margin: 0; padding: 0; background-color: #0a0613; }
  .hover-purple:hover { background-color: #7c3aed !important; }
  @media only screen and (max-width: 600px) {
    .container { width: 100% !important; }
    .px-mobile { padding-left: 24px !important; padding-right: 24px !important; }
    .stack { display: block !important; width: 100% !important; padding: 16px 0 !important; border-right: none !important; }
    .h1-mobile { font-size: 28px !important; line-height: 36px !important; }
  }
</style>
</head>
<body style="margin:0; padding:0; background-color:#0a0613; font-family:'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">

<div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">
  Your mock interview with {{ownerName}} is locked in for {{interviewTime}}.
</div>

<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background:#0a0613; background-image: radial-gradient(ellipse at top, rgba(139,92,246,0.15) 0%, transparent 60%);">
  <tr>
    <td align="center" style="padding: 48px 16px;">
      <table role="presentation" class="container" width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%;">

        <tr>
          <td class="px-mobile" style="padding: 0 8px 32px 8px;">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td align="left">
                  <span style="display:inline-block; padding:8px 14px; background:rgba(139,92,246,0.12); border:1px solid rgba(139,92,246,0.3); border-radius:999px; font-size:11px; font-weight:600; letter-spacing:2px; color:#c4b5fd; text-transform:uppercase; font-family:'Inter', sans-serif;">
                    InnerView
                  </span>
                </td>
              </tr>
            </table>
          </td>
        </tr>

        <tr>
          <td style="background:#13091f; background-image: linear-gradient(180deg, #1a0f2e 0%, #100820 100%); border:1px solid rgba(139,92,246,0.18); border-radius:20px; overflow:hidden;">

            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td style="height:3px; background:linear-gradient(90deg, #6366f1 0%, #a855f7 50%, #ec4899 100%); font-size:0; line-height:0;">&nbsp;</td>
              </tr>
            </table>

            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td class="px-mobile" style="padding: 40px 48px 8px 48px;">
                  <span style="display:inline-block; padding:6px 12px; background:rgba(34,197,94,0.12); border:1px solid rgba(34,197,94,0.35); border-radius:999px; font-size:11px; font-weight:600; letter-spacing:1.5px; color:#86efac; text-transform:uppercase;">
                    Confirmed
                  </span>
                </td>
              </tr>
              <tr>
                <td class="px-mobile" style="padding: 16px 48px 0 48px;">
                  <h1 class="h1-mobile" style="margin:0; font-size:34px; line-height:42px; font-weight:800; color:#ffffff; letter-spacing:-0.5px;">
                    Interview <span style="background:linear-gradient(90deg, #a855f7, #d8b4fe); -webkit-background-clip:text; -webkit-text-fill-color:transparent; background-clip:text;">scheduled.</span>
                  </h1>
                </td>
              </tr>
              <tr>
                <td class="px-mobile" style="padding: 16px 48px 36px 48px;">
                  <p style="margin:0; font-size:15px; line-height:24px; color:#a8a3b8;">
                    Hey <strong style="color:#e4d9ff; font-weight:600;">{{userName}}</strong> — your mock interview with <strong style="color:#e4d9ff; font-weight:600;">{{ownerName}}</strong> is locked in. Add it to your calendar and show up sharp.
                  </p>
                </td>
              </tr>
            </table>

            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" class="px-mobile" style="padding: 0 48px;">
              <tr>
                <td style="background:rgba(10,6,19,0.6); border:1px solid rgba(139,92,246,0.15); border-radius:14px; padding: 4px 0;">

                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td style="padding: 20px 24px; border-bottom:1px solid rgba(139,92,246,0.1);">
                        <p style="margin:0 0 6px 0; font-size:10px; letter-spacing:2px; color:#7c6f9d; text-transform:uppercase; font-weight:600;">When</p>
                        <p style="margin:0; font-size:17px; line-height:24px; color:#ffffff; font-weight:600;">{{interviewTime}}</p>
                        <p style="margin:4px 0 0 0; font-size:13px; color:#8b85a3;">{{interviewTimezone}}</p>
                      </td>
                    </tr>
                  </table>

                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td class="stack" width="50%" style="padding: 20px 24px; border-bottom:1px solid rgba(139,92,246,0.1); border-right:1px solid rgba(139,92,246,0.1); vertical-align:top;">
                        <p style="margin:0 0 6px 0; font-size:10px; letter-spacing:2px; color:#7c6f9d; text-transform:uppercase; font-weight:600;">Interviewer</p>
                        <p style="margin:0; font-size:15px; line-height:22px; color:#ffffff; font-weight:600;">{{ownerName}}</p>
                      </td>
                      <td class="stack" width="50%" style="padding: 20px 24px; border-bottom:1px solid rgba(139,92,246,0.1); vertical-align:top;">
                        <p style="margin:0 0 6px 0; font-size:10px; letter-spacing:2px; color:#7c6f9d; text-transform:uppercase; font-weight:600;">Candidate</p>
                        <p style="margin:0; font-size:15px; line-height:22px; color:#ffffff; font-weight:600;">{{userName}}</p>
                      </td>
                    </tr>
                  </table>

                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td style="padding: 20px 24px;">
                        <p style="margin:0 0 6px 0; font-size:10px; letter-spacing:2px; color:#7c6f9d; text-transform:uppercase; font-weight:600;">Duration</p>
                        <p style="margin:0; font-size:15px; line-height:22px; color:#ffffff; font-weight:600;">{{duration}} minutes</p>
                      </td>
                    </tr>
                  </table>

                </td>
              </tr>
            </table>

            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td class="px-mobile" align="center" style="padding: 36px 48px 12px 48px;">
                  <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td style="border-radius:10px; background:linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%); box-shadow: 0 8px 24px rgba(139,92,246,0.35);">
                        <a href="{{roomUrl}}" class="hover-purple" style="display:inline-block; padding:14px 32px; font-size:14px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:10px; letter-spacing:0.2px;">
                          Open your room
                        </a>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td class="px-mobile" align="center" style="padding: 0 48px 40px 48px;">
                  <a href="{{calendarUrl}}" style="font-size:13px; color:#a8a3b8; text-decoration:none; border-bottom:1px dashed rgba(168,163,184,0.3); padding-bottom:2px;">
                    Add to calendar →
                  </a>
                </td>
              </tr>
            </table>

            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr>
                <td class="px-mobile" style="padding: 24px 48px; background:rgba(10,6,19,0.4); border-top:1px solid rgba(139,92,246,0.1);">
                  <p style="margin:0; font-size:12px; line-height:18px; color:#7c6f9d; font-family:'JetBrains Mono', monospace;">
                    <span style="color:#a855f7;">$</span> innerview --status scheduled<br>
                    <span style="color:#86efac;">&gt;</span> room ready · editor synced · runtime warm
                  </p>
                </td>
              </tr>
            </table>

          </td>
        </tr>

        <tr>
          <td class="px-mobile" style="padding: 32px 8px 0 8px;" align="center">
            <p style="margin:0 0 8px 0; font-size:12px; color:#5d5673;">
              Need to reschedule? <a href="{{manageUrl}}" style="color:#a8a3b8; text-decoration:underline;">Manage interview</a>
            </p>
            <p style="margin:0; font-size:11px; color:#3d3651;">
              InnerView · A sharper room for serious mock interviews
            </p>
          </td>
        </tr>

      </table>
    </td>
  </tr>
</table>

</body>
</html>
""";
}