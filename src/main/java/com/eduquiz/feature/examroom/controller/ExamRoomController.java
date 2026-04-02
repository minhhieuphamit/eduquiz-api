package com.eduquiz.feature.examroom.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.examroom.dto.CreateRoomRequest;
import com.eduquiz.feature.examroom.dto.JoinRoomRequest;
import com.eduquiz.feature.examroom.dto.RoomResponse;
import com.eduquiz.feature.examroom.dto.RoomResultResponse;
import com.eduquiz.feature.examroom.entity.RoomStatus;
import com.eduquiz.feature.examroom.service.ExamRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exam Room", description = "Quản lý phòng thi")
public class ExamRoomController {

    private final ExamRoomService examRoomService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Tạo phòng thi mới (TEACHER)")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examRoomService.createRoom(request, teacher));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Danh sách phòng thi của tôi (TEACHER)")
    public ResponseEntity<ApiResponse<PageResponse<RoomResponse>>> getMyRooms(
            @AuthenticationPrincipal User teacher,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(examRoomService.getMyRooms(teacher, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Chi tiết phòng thi (TEACHER)")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(examRoomService.getRoomDetail(id, teacher));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Cập nhật trạng thái phòng thi (TEACHER)")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User teacher) {
        RoomStatus newStatus = RoomStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(examRoomService.updateRoomStatus(id, newStatus, teacher));
    }

    @GetMapping("/{id}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Kết quả phòng thi (TEACHER)")
    public ResponseEntity<ApiResponse<RoomResultResponse>> getRoomResults(
            @PathVariable UUID id,
            @AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(examRoomService.getRoomResults(id, teacher));
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Học sinh tham gia phòng thi")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
            @Valid @RequestBody JoinRoomRequest request,
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(examRoomService.joinRoom(request.getRoomCode(), student));
    }

    @GetMapping("/check/{roomCode}")
    @Operation(summary = "Kiểm tra trạng thái phòng thi (public)")
    public ResponseEntity<ApiResponse<RoomResponse>> checkRoom(@PathVariable String roomCode) {
        return ResponseEntity.ok(examRoomService.checkRoomByCode(roomCode));
    }
}
