package com.eduquiz.feature.examroom.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.examroom.dto.CreateRoomRequest;
import com.eduquiz.feature.examroom.dto.JoinRoomRequest;
import com.eduquiz.feature.examroom.dto.RoomResponse;
import com.eduquiz.feature.examroom.dto.RoomResultResponse;
import com.eduquiz.feature.examroom.service.ExamRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class ExamRoomController {

    private final ExamRoomService roomService;

    /** POST /api/v1/rooms — create a room (TEACHER only) */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(roomService.createRoom(request, currentUser));
    }

    /** GET /api/v1/rooms/my — teacher's own rooms (paginated) */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<RoomResponse>>> getMyRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(roomService.getMyRooms(currentUser, pageable));
    }

    /** GET /api/v1/rooms/check/{roomCode} — public room status check (student waiting room) */
    @GetMapping("/check/{roomCode}")
    public ResponseEntity<ApiResponse<RoomResponse>> checkRoomByCode(
            @PathVariable String roomCode) {
        return ResponseEntity.ok(roomService.checkRoomByCode(roomCode));
    }

    /** GET /api/v1/rooms/{id} — room detail */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(roomService.getRoomDetail(id, currentUser));
    }

    /** PUT /api/v1/rooms/{id}/status — manually update room status (TEACHER) */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String status = body.get("status");
        String endTimeStr = body.get("endTime");
        LocalDateTime newEndTime = endTimeStr != null ? LocalDateTime.parse(endTimeStr) : null;
        return ResponseEntity.ok(roomService.updateRoomStatus(id, status, newEndTime, currentUser));
    }

    /** GET /api/v1/rooms/{id}/results — room exam results (TEACHER) */
    @GetMapping("/{id}/results")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RoomResultResponse>> getRoomResults(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(roomService.getRoomResults(id, currentUser));
    }

    /** POST /api/v1/rooms/join — student joins a room */
    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
            @Valid @RequestBody JoinRoomRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(roomService.joinRoom(request, currentUser));
    }
}
