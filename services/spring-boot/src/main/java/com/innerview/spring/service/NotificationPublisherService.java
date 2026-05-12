package com.innerview.spring.service;

import com.innerview.spring.entity.scheduleNotification;

public interface NotificationPublisherService {
    void publishEvent(scheduleNotification event);
}
