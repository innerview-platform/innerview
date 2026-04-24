package com.innerview.spring.service.impl;

import com.innerview.spring.dto.InstantInterviewRequest;
import com.innerview.spring.dto.InterviewResponse;
import com.innerview.spring.dto.InterviewSummaryDto;
import com.innerview.spring.dto.ScheduledInterviewRequest;
import com.innerview.spring.entity.Interview;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.mapper.InterviewMapper;
import com.innerview.spring.repository.InterviewRepository;
import com.innerview.spring.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InterviewServiceImpl implements InterviewService {
    final private InterviewRepository interviewRepository;
    final private InterviewMapper interviewMapper;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LENGTH = 6;
    @Value("${frontend.url}")
    private String frontendUrl;


    public static String generateUniqueRoomId() {
        StringBuilder roomId = new StringBuilder(ID_LENGTH);

        for (int i = 0; i < ID_LENGTH; i++) {
            // ThreadLocalRandom.current() is blazing fast in Spring Boot
            int randomIndex = ThreadLocalRandom.current().nextInt(CHARACTERS.length());
            roomId.append(CHARACTERS.charAt(randomIndex));
        }

        return roomId.toString();
    }

    @Override
    public List<InterviewSummaryDto> getInterviewHistory(Long userId) {
        return interviewRepository.findCompletedInterviewsByUserIdNative(userId)
                .stream()
                .map(interviewMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    public InterviewResponse createInstantInterview(InstantInterviewRequest request) {
        Interview interview = interviewMapper.toInterview(request);

        // FIX 1: Actually save the generated Room ID to the database entity
        String roomId = generateUniqueRoomId();
        interview.setRoomId(roomId);

        // FIX 2: Prevent DateTimeException. Use LocalDateTime.now() directly.
        interview.setStartTime(LocalDateTime.now());
        // Set status to active/in-progress for instant interviews
        interview.setStatus(InterviewStatus.STARTED);

        Interview savedInterview = interviewRepository.save(interview);

        InterviewResponse response = new InterviewResponse();
        response.setRoomId(savedInterview.getRoomId());
        response.setRoomLink(frontendUrl + "/room/join/" + savedInterview.getRoomId());

        return response;
    }

    @Override
    public InterviewResponse createScheduledInterview(ScheduledInterviewRequest request) {
        Interview interview = new Interview();

        // 1. Map core properties
        // (Assuming you have InterviewStatus.SCHEDULED in your enum)
        interview.setOwnerId(request.getCreatorUserId());
        interview.setType(request.getInterviewType());
        interview.setStatus(InterviewStatus.SCHEDULED);

        // 2. Generate and assign the Room ID
        String roomId = generateUniqueRoomId();
        interview.setRoomId(roomId);

        // 3. Time Calculations
        // Safely convert Instant to LocalDateTime using the system's timezone
        LocalDateTime start = LocalDateTime.ofInstant(request.getStartTime(), java.time.ZoneId.systemDefault());
        interview.setStartTime(start);
        interview.setDurationMinutes(request.getDurationMinutes());

        // Automatically calculate the end time based on duration
        if (request.getDurationMinutes() != null) {
            interview.setEndTime(start.plusMinutes(request.getDurationMinutes()));
        }

        // 4. Save to Database
        Interview savedInterview = interviewRepository.save(interview);

        // 5. Construct Response
        InterviewResponse response = new InterviewResponse();
        response.setRoomId(savedInterview.getRoomId());
        // Assuming frontendUrl is a class-level variable like @Value("${frontend.url}")
        response.setRoomLink(frontendUrl + "/room/join/" + savedInterview.getRoomId());

        return response;
    }
}
