package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;
@Data
public class ScheduledInterviewRequest {
	InterviewType interviewType;
	InterviewRole creatorInterviewRole;
	Instant startTime;
}
