package com.innerview.spring.repository;

import com.innerview.spring.dto.UserAverageRatingProjection;
import com.innerview.spring.dto.UserAverageRatingResponse;
import com.innerview.spring.entity.User;
import com.innerview.spring.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile,Long> {


    @Query("SELECT COALESCE(AVG(f.rating), 0.0) AS averageRating, COUNT(f) AS totalReviews " +
            "FROM Feedback f WHERE f.reviewee.id = :userId")
    UserAverageRatingProjection getUserAverageRating(@Param("userId") UUID userId);



    Optional<UserProfile> getUserProfileByUser(User user);

    Optional<UserProfile> getUserProfileByUser_Id(UUID userId);
}
