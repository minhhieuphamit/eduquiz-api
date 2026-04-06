package com.eduquiz.feature.examroom.repository;

import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.examroom.entity.ExamRoom;
import com.eduquiz.feature.examroom.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, RoomParticipant.RoomParticipantId> {

    List<RoomParticipant> findByRoom(ExamRoom room);

    Optional<RoomParticipant> findByRoomAndUser(ExamRoom room, User user);

    int countByRoom(ExamRoom room);
}
