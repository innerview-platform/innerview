package com.innerview.spring.repository;

import com.innerview.spring.entity.Interview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query(value = "SELECT i.* FROM interviews i "
            + "JOIN interview_participants p ON i.id = p.interview_id "
            + "WHERE p.user_id = :userId AND i.status = 'COMPLETED'", nativeQuery = true)
    List<Interview> findCompletedInterviewsByUserIdNative(@Param("userId") UUID userId);

    List<Interview> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Interview getInterviewsByRoomId(String roomId);

    @Query("""
            select distinct i
            from Interview i
            left join fetch i.problems
            where i.id = :id
            """)
    Optional<Interview> findByIdWithProblems(@Param("id") Long id);

    Optional<Interview> findByRoomId(String roomId);
}
