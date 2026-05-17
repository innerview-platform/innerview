package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.enums.ReminderInterval;
import com.innerview.spring.enums.RoomSize;
import com.innerview.spring.interfaces.EmailSendable;
import com.innerview.spring.interfaces.InAppSendable;
import java.time.Instant;

public class InterviewReminderNotification extends Notification
    implements EmailSendable, InAppSendable {

  private final Instant scheduledAt;
  private final String sessionUrl;
  private final InterviewType interviewType;
  private final RoomSize roomSize;
  private final ReminderInterval interval;

  public InterviewReminderNotification(
      String recipientId,
      String recipientEmail,
      InterviewType interviewType,
      RoomSize roomSize,
      Instant scheduledAt,
      ReminderInterval reminderInterval,
      String sessionUrl) {
    super(recipientId, recipientEmail);
    this.scheduledAt = scheduledAt;
    this.sessionUrl = sessionUrl;
    this.interviewType = interviewType;
    this.roomSize = roomSize;
    this.interval = reminderInterval;
  }

  @Override
  public String toInAppContent() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'toInAppContent'");
  }

  @Override
  public String toEmailContent() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'toEmailContent'");
  }
}
