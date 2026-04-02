package com.eduquiz.feature.examroom.service;

import com.eduquiz.feature.examroom.entity.ExamRoom;
import com.eduquiz.feature.examroom.entity.RoomStatus;
import com.eduquiz.feature.examroom.repository.ExamRoomRepository;
import com.eduquiz.feature.examsession.entity.ExamSession;
import com.eduquiz.feature.examsession.entity.SessionStatus;
import com.eduquiz.feature.examsession.repository.ExamSessionRepository;
import com.eduquiz.feature.examsession.service.ExamSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Room Scheduler — runs every minute.
 * - openScheduledRooms(): SCHEDULED rooms with startTime <= now → set OPEN
 * - closeExpiredRooms(): OPEN rooms with endTime <= now → set CLOSED + auto-submit
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomSchedulerService {

    private final ExamRoomRepository examRoomRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamSessionService examSessionService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void openScheduledRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamRoom> rooms = examRoomRepository.findByStatusAndStartTimeBefore(RoomStatus.SCHEDULED, now);

        for (ExamRoom room : rooms) {
            room.setStatus(RoomStatus.OPEN);
            examRoomRepository.save(room);
            log.info("[RoomScheduler] Opened room: {} ({})", room.getRoomCode(), room.getId());
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamRoom> rooms = examRoomRepository.findByStatusInAndEndTimeBefore(
                List.of(RoomStatus.OPEN, RoomStatus.IN_PROGRESS), now);

        for (ExamRoom room : rooms) {
            // Auto-submit all in-progress sessions
            List<ExamSession> activeSessions = examSessionRepository
                    .findByRoomAndStatus(room, SessionStatus.IN_PROGRESS);
            for (ExamSession session : activeSessions) {
                try {
                    examSessionService.autoSubmitSession(session);
                } catch (Exception e) {
                    log.error("[RoomScheduler] Failed to auto-submit session {}: {}", session.getId(), e.getMessage());
                }
            }

            room.setStatus(RoomStatus.CLOSED);
            examRoomRepository.save(room);
            log.info("[RoomScheduler] Closed room: {} ({}), auto-submitted {} sessions",
                    room.getRoomCode(), room.getId(), activeSessions.size());
        }
    }
}
