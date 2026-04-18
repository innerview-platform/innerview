package com.innerview.spring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.innerview.spring.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByResetPasswordToken(String resetPasswordToken);


	Optional<User> findByProviderId(String providerId);
}
