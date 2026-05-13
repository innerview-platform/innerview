package com.innerview.spring.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.innerview.spring.entity.ScheduleNotification;
import com.innerview.spring.service.GoogleApiService;
import com.innerview.spring.service.notification.EmailNotificationWorker;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@AllArgsConstructor
public class GoogleApiServiceImpl implements GoogleApiService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private static final Logger log = LoggerFactory.getLogger(GoogleApiServiceImpl.class);

    @Override
    public String createInterviewEvent(ScheduleNotification scheduleNotification) {
        try {
            // 1. Get the Access Token — return null gracefully if user hasn't done OAuth
            OAuth2AuthorizedClient client = authorizedClientService
                    .loadAuthorizedClient("google", scheduleNotification.getRecipientEmail());

            if (client == null) {
                log.warn("User {} has no Google OAuth token — skipping calendar event",
                        scheduleNotification.getRecipientEmail());
                return null;
            }

            String accessToken = client.getAccessToken().getTokenValue();

            // 2. Build the Google Calendar Client
            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                    .setApplicationName("Innerview")
                    .build();

            String title = "Technical Interview: " + scheduleNotification.getRecipientEmail() + " @ Innerview";
            String description = "Discussion";

            // 3. Define the Event
            Event event = new Event()
                    .setSummary(title)
                    .setDescription(description);

            // 4. Set Start/End Time
            DateTime startDateTime = new DateTime(
                    scheduleNotification.getDate()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli());
            event.setStart(new EventDateTime().setDateTime(startDateTime));

            DateTime endDateTime = new DateTime(
                    scheduleNotification.getEndTime()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli());
            event.setEnd(new EventDateTime().setDateTime(endDateTime));

            // 5. Execute the Insert
            event = service.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .execute();

            log.info("Calendar event created: {}", event.getHtmlLink());
            return event.getHangoutLink();

        } catch (Exception e) {
            log.error("Failed to create Google Calendar event for user={} — skipping",
                    scheduleNotification.getRecipientEmail(), e);
            return null; // never crash the email flow
        }
    }
}