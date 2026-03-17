package com.eduquiz.feature.subject.repository;

import com.eduquiz.feature.subject.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findByName(String name);

    boolean existsByName(String name);

    Page<Subject> findAllByIsActiveTrue(Pageable pageable);

    Optional<Subject> findByIdAndIsActiveTrue(UUID id);

    boolean existsByNameAndIsActiveTrue(String name);

    boolean existsByNameAndIdNotAndIsActiveTrue(String name, UUID id);
}
