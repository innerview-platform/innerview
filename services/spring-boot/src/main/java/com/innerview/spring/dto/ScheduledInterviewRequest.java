package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewType;

import java.time.Instant;
import java.util.UUID;

public class ScheduledInterviewRequest {
	UUID creatorUserId;
	InterviewType interviewType;
	InterviewRole creatorInterviewRole;
	Instant startTime;
	Integer durationMinutes;
}