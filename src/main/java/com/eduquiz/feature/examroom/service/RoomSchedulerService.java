package com.eduquiz.feature.examroom.service;

import com.eduquiz.feature.examroom.entity.ExamRoom;
import com.eduquiz.feature.examroom.entity.RoomStatus;
import com.eduquiz.feature.examroom.repository.ExamRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomSchedulerService {

    private final ExamRoomRepository roomRepository;

    /**
     * Every 30 seconds: open SCHEDULED rooms whose startTime has passed.
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void openScheduledRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamRoom> toOpen = roomRepository
                .findByStatusAndStartTimeLessThanEqual(RoomStatus.SCHEDULED, now);

        if (toOpen.isEmpty()) return;

        log.info("[RoomScheduler.open] Opening {} scheduled rooms", toOpen.size());
        for (ExamRoom room : toOpen) {
            room.setStatus(RoomStatus.OPEN);
            roomRepository.save(room);
            log.info("[RoomScheduler.open] Opened room={}", room.getId());
        }
    }

    /**
     * Every 30 seconds: close OPEN/IN_PROGRESS rooms whose endTime has passed.
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    @Transactional
    public void closeExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamRoom> toClose = roomRepository
                .findByStatusInAndEndTimeLessThanEqual(
                        List.of(RoomStatus.OPEN, RoomStatus.IN_PROGRESS), now);

        if (toClose.isEmpty()) return;

        log.info("[RoomScheduler.close] Closing {} expired rooms", toClose.size());
        for (ExamRoom room : toClose) {
            room.setStatus(RoomStatus.CLOSED);
            roomRepository.save(room);
            log.info("[RoomScheduler.close] Closed room={}", room.getId());
        }
    }
}
