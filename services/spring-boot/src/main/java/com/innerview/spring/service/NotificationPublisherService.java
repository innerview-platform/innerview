package com.innerview.spring.service;

import com.innerview.spring.entity.InterviewEvent;

public interface NotificationPublisherService {
    void publishEvent(InterviewEvent event);
}
