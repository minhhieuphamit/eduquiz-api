package com.eduquiz.feature.auth.repository;

import com.eduquiz.feature.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // ── User management queries ──

    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:roleName IS NULL OR u.role.name = :roleName) " +
            "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
