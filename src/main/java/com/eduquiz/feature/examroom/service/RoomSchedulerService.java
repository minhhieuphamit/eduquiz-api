package com.eduquiz.feature.examroom.service;

/**
 * Room Scheduler (@Scheduled).
 * <p>
 * Chạy mỗi phút:
 * - openScheduledRooms(): tìm rooms SCHEDULED có startTime <= now → set OPEN
 * - closeExpiredRooms(): tìm rooms OPEN/IN_PROGRESS có endTime <= now → set CLOSED
 * + tự động submit bài cho HS chưa nộp (publish Kafka event)
 * <p>
 * TODO: @Service + @Scheduled(fixedRate = 60000)
 */
public class RoomSchedulerService {
}
