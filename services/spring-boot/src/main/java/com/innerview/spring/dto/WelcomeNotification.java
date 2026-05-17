package com.innerview.spring.dto;

import com.innerview.spring.interfaces.EmailSendable;
import com.innerview.spring.interfaces.InAppSendable;

public class WelcomeNotification extends Notification implements EmailSendable, InAppSendable {

  private final String loginUrl;

  public WelcomeNotification(
      String recipientId, String recipientEmail, String platformName, String loginUrl) {
    super(recipientId, recipientEmail);
    this.loginUrl = loginUrl;
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
