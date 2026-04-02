package com.eduquiz.feature.examroom.repository;

import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.examroom.entity.ExamRoom;
import com.eduquiz.feature.examroom.entity.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamRoomRepository extends JpaRepository<ExamRoom, UUID> {

    Optional<ExamRoom> findByRoomCode(String roomCode);

    Page<ExamRoom> findByTeacherOrderByCreatedAtDesc(User teacher, Pageable pageable);

    List<ExamRoom> findByStatusAndStartTimeBefore(RoomStatus status, LocalDateTime time);

    List<ExamRoom> findByStatusInAndEndTimeBefore(List<RoomStatus> statuses, LocalDateTime time);

    boolean existsByRoomCode(String roomCode);
}
