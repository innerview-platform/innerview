package com.innerview.spring.service.redis;

import com.innerview.spring.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisExpirationListner implements MessageListener {

  private final InterviewService interviewService;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String expiredKey = message.toString(); // looks like "interview:123"

    if (expiredKey.startsWith("interview:")) {
      Long interviewId = Long.parseLong(expiredKey.split(":")[1]);

      // Trigger your logic!
      interviewService.executeEndInterviewTime(interviewId);
    }
  }
}
