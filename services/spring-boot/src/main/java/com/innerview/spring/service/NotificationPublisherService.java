package com.innerview.spring.service;

import com.innerview.spring.entity.ScheduleNotification;

public interface NotificationPublisherService {
    void publishEvent(ScheduleNotification event);
}
