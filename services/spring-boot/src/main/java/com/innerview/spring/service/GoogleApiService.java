package com.innerview.spring.service;

import com.innerview.spring.entity.ScheduleNotification;

public interface GoogleApiService {
    String createInterviewEvent(ScheduleNotification scheduleNotification);
}

