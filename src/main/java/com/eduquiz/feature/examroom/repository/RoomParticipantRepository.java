package com.eduquiz.feature.examroom.repository;

import com.eduquiz.feature.examroom.entity.ExamRoom;
import com.eduquiz.feature.examroom.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, RoomParticipant.RoomParticipantId> {
    List<RoomParticipant> findByRoom(ExamRoom room);
}
