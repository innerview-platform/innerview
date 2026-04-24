package com.innerview.spring.dto;

import com.innerview.spring.enums.InterviewStatus;
import lombok.Data;

@Data
public class InterviewResponse {
	String roomId;
	String roomLink;
}