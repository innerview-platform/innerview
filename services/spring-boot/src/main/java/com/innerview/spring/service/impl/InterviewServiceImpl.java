package com.innerview.spring.service.impl;

import com.innerview.spring.core.util.RoomUtil;
import com.innerview.spring.dto.InstantInterviewRequest;
import com.innerview.spring.dto.InterviewResponse;
import com.innerview.spring.dto.InterviewSummaryDto;
import com.innerview.spring.dto.ScheduledInterviewRequest;
import com.innerview.spring.entity.Interview;
import com.innerview.spring.enums.Channel;
import com.innerview.spring.enums.InterviewStatus;
import com.innerview.spring.mapper.InterviewMapper;
import com.innerview.spring.repository.InterviewRepository;
import com.innerview.spring.service.InterviewService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.innerview.spring.entity.ScheduleNotification;

import com.innerview.spring.service.NotificationPublisherService;
import com.innerview.spring.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class InterviewServiceImpl implements InterviewService {
    private final InterviewRepository interviewRepository;
    private final InterviewMapper interviewMapper;
    private final UserProfileService userProfileService;
    private final NotificationPublisherService notificationPublisherService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${interview.duration}")
    private Integer interviewDuration;


    @Override
    public List<InterviewSummaryDto> getInterviewHistory(UUID userId) {
        return interviewRepository.findCompletedInterviewsByUserIdNative(userId).stream()
                .map(interviewMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    public InterviewResponse createInstantInterview(InstantInterviewRequest request, UUID userId) {
        Interview interview = new Interview();
        interview.setType(request.getInterviewType());
        interview.setOwnerId(userId);
        // FIX 1: Actually save the generated Room ID to the database entity
        String roomId = RoomUtil.generateUniqueRoomId();
        interview.setRoomId(roomId);
        //used Instant insted of LocalDateTime to avoid problems related to changing system clock or using cluster of servers
        interview.setStartTime(Instant.now());
        // Set status to active/in-progress for instant interviews
        interview.setStatus(InterviewStatus.STARTED);
        interview.setDurationMinutes(interviewDuration);

        Interview savedInterview = interviewRepository.save(interview);

        sendScheduleNotification(savedInterview,userId);

        InterviewResponse response = new InterviewResponse();
        response.setRoomId(savedInterview.getRoomId());
        response.setRoomLink(frontendUrl + "/room/join/" + savedInterview.getRoomId());

        return response;
    }

    @Override
    public InterviewResponse createScheduledInterview(
            ScheduledInterviewRequest request, UUID userId) {
        Interview interview = new Interview();

        // 1. Map core properties
        // (Assuming you have InterviewStatus.SCHEDULED in your enum)
        interview.setOwnerId(userId);
        interview.setType(request.getInterviewType());
        interview.setStatus(InterviewStatus.SCHEDULED);

        // 2. Generate and assign the Room ID
        String roomId = RoomUtil.generateUniqueRoomId();
        interview.setRoomId(roomId);

        // 3. Time
        Instant startTime = request.getStartTime();
        interview.setStartTime(startTime);
        interview.setDurationMinutes(interviewDuration);

        // Automatically calculate the end time based on duration
        interview.setEndTime(startTime.plus(interviewDuration, ChronoUnit.MINUTES));

        // 4. Save to Database
        Interview savedInterview = interviewRepository.save(interview);

        // 5. Construct Response
        InterviewResponse response = new InterviewResponse();
        response.setRoomId(savedInterview.getRoomId());
        // Assuming frontendUrl is a class-level variable like @Value("${frontend.url}")
        response.setRoomLink(frontendUrl + "/room/join/" + savedInterview.getRoomId());


        // sending notification

       sendScheduleNotification(savedInterview,userId);




        return response;
    }


    private void sendScheduleNotification(Interview interview,UUID userID){
        // sending notification
        var ownerProfile = userProfileService.getUserProfile(interview.getOwnerId());
        var userEmail = userProfileService.getUserProfile(userID).getUser().getEmail();
        var scheduleNotification=ScheduleNotification.builder().interviewId(interview.getId())
                .date(interview.getStartTime())
                .channel(Channel.EMAIL)
                .recipientEmail(userEmail)
                .OwnerAccount(ownerProfile.getUser().getEmail())
                .OwnerUsername(ownerProfile.getUser().getName())
                .endTime(interview.getEndTime())
                .durationMinutes(interview.getDurationMinutes())
                .payload(Map.of("schedulling", "the meeting scheduleed"))
                .build();

        //send it nonblocking
        notificationPublisherService.publishEvent(scheduleNotification);
    }
}