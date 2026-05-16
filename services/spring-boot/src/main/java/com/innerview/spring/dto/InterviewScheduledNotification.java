package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewType;
import com.innerview.spring.enums.RoomSize;
import com.innerview.spring.interfaces.EmailSendable;
import com.innerview.spring.interfaces.InAppSendable;
import java.time.Instant;

public class InterviewScheduledNotification extends Notification
    implements EmailSendable, InAppSendable {

  private final Instant scheduledAt;
  private final String sessionUrl;
  private final InterviewType interviewType;
  private final RoomSize roomSize;

  public InterviewScheduledNotification(
      String recipientId,
      String recipientEmail,
      InterviewType interviewType,
      RoomSize roomSize,
      Instant scheduledAt,
      String sessionUrl) {
    super(recipientId, recipientEmail);
    this.scheduledAt = scheduledAt;
    this.sessionUrl = sessionUrl;
    this.interviewType = interviewType;
    this.roomSize = roomSize;
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
