package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.InterviewType;

import java.util.UUID;

public class InstantInterviewRequest {
	UUID creatorUserId;
	InterviewType interviewType;
	InterviewRole creatorInterviewRole;
}
