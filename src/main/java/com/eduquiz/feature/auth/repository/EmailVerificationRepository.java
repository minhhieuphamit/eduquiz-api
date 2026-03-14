package com.eduquiz.feature.auth.repository;

import com.eduquiz.feature.auth.entity.EmailVerification;
import com.eduquiz.feature.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findTopByUserOrderByCreatedAtDesc(User user);

    int countByUserAndCreatedAtAfter(User user, LocalDateTime since);
}
