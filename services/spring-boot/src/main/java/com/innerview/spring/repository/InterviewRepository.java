package com.innerview.spring.repository;

import com.innerview.spring.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query(value = "SELECT i.* FROM interviews i " +
            "JOIN interview_participants p ON i.id = p.interview_id " +
            "WHERE p.user_id = :userId AND i.status = 'COMPLETED'",
            nativeQuery = true)
    List<Interview> findCompletedInterviewsByUserIdNative(@Param("userId") Long userId);

    Interview getInterviewsByRoomId(String roomId);
}
