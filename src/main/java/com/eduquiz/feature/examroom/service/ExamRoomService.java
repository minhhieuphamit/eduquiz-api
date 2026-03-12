package com.eduquiz.feature.examroom.service;

/**
 * Exam Room Service.
 * <p>
 * - createRoom(request, teacherId): tạo phòng, generate room code
 * - getMyRooms(teacherId): danh sách phòng của teacher
 * - getRoomDetail(roomId): chi tiết + danh sách HS
 * - joinRoom(roomCode, studentId): HS tham gia phòng
 * - getMyExamInRoom(roomId, studentId): lấy đề được phát (khi OPEN)
 * → FULL_RANDOM: generate đề mới cho HS
 * → POOL_RANDOM: random 1 đề từ pool
 * - getRoomResults(roomId): kết quả phòng thi (cho Teacher)
 * <p>
 * TODO: @Service
 */
public class ExamRoomService {
}
