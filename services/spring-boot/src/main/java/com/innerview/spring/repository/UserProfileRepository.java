package com.innerview.spring.repository;

import com.innerview.spring.dto.UserAverageRatingProjection;
import com.innerview.spring.dto.UserAverageRatingResponse;
import com.innerview.spring.entity.User;
import com.innerview.spring.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile,Long> {


    @Query(value = "SELECT IFNULL(AVG(rating), 0) as averageRating, COUNT(*) as totalReviews " +
            "FROM feedback WHERE reviewee_id = :userId", nativeQuery = true)
    UserAverageRatingProjection getUserAverageRating(@Param("userId") UUID userId);


    UserProfile getUserProfileByUser(User user);

    UserProfile getUserProfileByUser_Id(UUID userId);
}
