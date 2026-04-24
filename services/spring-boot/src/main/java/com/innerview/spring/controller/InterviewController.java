package com.innerview.spring.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.innerview.spring.dto.InstantInterviewRequest;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

	@GetMapping("/user/{userId}/history")
	public ResponseEntity<?> getUserInterviewHistory(@PathVariable UUID userId) {
		//Logic: Passes the request to interviewService.getInterviewHistory(userId).
		return ResponseEntity.ok("User interview history");
	}

	@PostMapping("/instant")
	public ResponseEntity<?> createInstantInterview(@RequestBody InstantInterviewRequest instantInterviewRequest) {
		interviewService.createInstantInterview(instantInterviewRequest);
		return ResponseEntity.ok("Instant interview created");
	}

	@PostMapping("/scheduled")
	public ResponseEntity<?> createScheduledInterview(@RequestBody InstantInterviewRequest req) {
		//Logic: Passes the request to interviewService.createScheduledInterview(request).
		return ResponseEntity.ok("Scheduled interview created");
	}

}
