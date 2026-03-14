package com.eduquiz.feature.examroom.repository;

import com.eduquiz.feature.examroom.entity.ExamRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamRoomRepository extends JpaRepository<ExamRoom, UUID> {
    Optional<ExamRoom> findByRoomCode(String roomCode);
}
