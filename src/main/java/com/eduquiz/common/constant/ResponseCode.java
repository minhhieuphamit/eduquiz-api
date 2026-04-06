package com.eduquiz.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    // ══════════════════════════════════════════════════════
    // 1000–1999  Common / System
    // ══════════════════════════════════════════════════════
    SUCCESS(1000, "Success"),
    CREATED_SUCCESS(1001, "Created successfully"),
    UPDATED_SUCCESS(1002, "Updated successfully"),
    DELETED_SUCCESS(1003, "Deleted successfully"),

    BAD_REQUEST(1400, "Bad request"),
    VALIDATION_ERROR(1401, "Validation failed"),
    RESOURCE_NOT_FOUND(1404, "Resource not found"),
    DUPLICATE_RESOURCE(1409, "Resource already exists"),
    TOO_MANY_REQUESTS(1429, "Too many requests"),
    INTERNAL_SERVER_ERROR(1500, "Internal server error"),
    SERVICE_UNAVAILABLE(1503, "Service unavailable"),

    // ══════════════════════════════════════════════════════
    // 2000–2099  Auth / Token
    // ══════════════════════════════════════════════════════
    AUTH_LOGIN_SUCCESS(2000, "Login successful"),
    AUTH_REGISTER_SUCCESS(2001, "Registration successful"),
    AUTH_LOGOUT_SUCCESS(2002, "Logout successful"),
    AUTH_REFRESH_SUCCESS(2003, "Token refreshed successfully"),
    AUTH_ME_SUCCESS(2004, "User info fetched successfully"),
    AUTH_PASSWORD_RESET_SUCCESS(2005, "Password reset successful"),
    AUTH_PASSWORD_CHANGED(2006, "Password changed successfully"),
    AUTH_PROFILE_UPDATED(2007, "Profile updated successfully"),

    AUTH_UNAUTHORIZED(2401, "Unauthorized"),
    AUTH_INVALID_CREDENTIALS(2402, "Invalid email or password"),
    AUTH_TOKEN_INVALID(2403, "Invalid token"),
    AUTH_TOKEN_EXPIRED(2404, "Token expired"),
    AUTH_REFRESH_TOKEN_INVALID(2405, "Invalid refresh token"),
    AUTH_REFRESH_TOKEN_EXPIRED(2406, "Refresh token expired"),
    AUTH_ACCOUNT_NOT_VERIFIED(2407, "Account not verified"),
    AUTH_FORBIDDEN(2408, "Access denied"),
    AUTH_ACCOUNT_INACTIVE(2409, "Account is inactive"),
    AUTH_PASSWORD_INCORRECT(2410, "Current password is incorrect"),
    AUTH_NEW_PASSWORD_SAME_AS_OLD(2411, "New password must differ from old password"),

    // ══════════════════════════════════════════════════════
    // 2100–2199  OTP / Email Verification
    // ══════════════════════════════════════════════════════
    OTP_SENT_SUCCESS(2100, "OTP sent successfully"),
    OTP_RESENT_SUCCESS(2101, "OTP resent successfully"),
    OTP_VERIFIED_SUCCESS(2102, "OTP verified successfully"),

    OTP_INVALID(2501, "Invalid OTP"),
    OTP_EXPIRED(2502, "OTP expired"),
    OTP_ALREADY_USED(2503, "OTP already used"),
    OTP_MAX_ATTEMPTS_EXCEEDED(2504, "OTP attempts exceeded"),
    OTP_RESEND_LIMIT_EXCEEDED(2505, "OTP resend limit exceeded"),
    OTP_NOT_FOUND(2506, "OTP not found"),
    EMAIL_ALREADY_VERIFIED(2507, "Email already verified"),
    EMAIL_SEND_FAILED(2508, "Failed to send email"),

    // ══════════════════════════════════════════════════════
    // 3000–3999  User / Profile / Role
    // ══════════════════════════════════════════════════════
    USER_CREATED_SUCCESS(3000, "User created successfully"),
    USER_FETCH_SUCCESS(3001, "User fetched successfully"),
    USER_UPDATED_SUCCESS(3002, "User updated successfully"),
    USER_LIST_SUCCESS(3003, "User list fetched successfully"),
    USER_ROLE_UPDATED(3004, "User role updated successfully"),
    USER_DEACTIVATED(3005, "User deactivated successfully"),
    USER_ACTIVATED(3006, "User activated successfully"),
    USER_DELETED(3007, "User deleted successfully"),

    USER_NOT_FOUND(3401, "User not found"),
    EMAIL_ALREADY_EXISTS(3402, "Email already exists"),
    USERNAME_ALREADY_EXISTS(3403, "Username already exists"),
    ROLE_NOT_FOUND(3404, "Role not found"),
    INSUFFICIENT_ROLE(3405, "Insufficient role"),
    USER_CANNOT_DEACTIVATE_SELF(3406, "Cannot deactivate your own account"),
    USER_CANNOT_CHANGE_OWN_ROLE(3407, "Cannot change your own role"),
    USER_ALREADY_ACTIVE(3408, "User is already active"),
    USER_ALREADY_INACTIVE(3409, "User is already inactive"),

    // ══════════════════════════════════════════════════════
    // 4000–4999  Subject / Chapter / Question / Exam
    // ══════════════════════════════════════════════════════
    SUBJECT_CREATED_SUCCESS(4000, "Subject created successfully"),
    SUBJECT_FETCH_SUCCESS(4005, "Subject fetched successfully"),
    SUBJECT_LIST_SUCCESS(4006, "Subject list fetched successfully"),
    SUBJECT_UPDATED_SUCCESS(4007, "Subject updated successfully"),
    SUBJECT_DELETED_SUCCESS(4008, "Subject deleted successfully"),

    CHAPTER_CREATED_SUCCESS(4001, "Chapter created successfully"),
    CHAPTER_FETCH_SUCCESS(4010, "Chapter fetched successfully"),
    CHAPTER_LIST_SUCCESS(4011, "Chapter list fetched successfully"),
    CHAPTER_UPDATED_SUCCESS(4012, "Chapter updated successfully"),
    CHAPTER_DELETED_SUCCESS(4013, "Chapter deleted successfully"),

    QUESTION_CREATED_SUCCESS(4002, "Question created successfully"),
    QUESTION_FETCH_SUCCESS(4020, "Question fetched successfully"),
    QUESTION_LIST_SUCCESS(4021, "Question list fetched successfully"),
    QUESTION_UPDATED_SUCCESS(4022, "Question updated successfully"),
    QUESTION_DELETED_SUCCESS(4023, "Question deleted successfully"),

    QUESTION_SHARED_SUCCESS(4050, "Question shared to all teachers"),
    QUESTION_UNSHARED_SUCCESS(4051, "Question unshared from all teachers"),

    EXAM_CREATED_SUCCESS(4003, "Exam created successfully"),
    EXAM_UPDATED_SUCCESS(4004, "Exam updated successfully"),
    EXAM_FETCH_SUCCESS(4030, "Exam fetched successfully"),
    EXAM_LIST_SUCCESS(4031, "Exam list fetched successfully"),
    EXAM_DELETED_SUCCESS(4032, "Exam deleted successfully"),

    SUBJECT_NOT_FOUND(4401, "Subject not found"),
    SUBJECT_NAME_DUPLICATE(4409, "Subject name already exists"),
    CHAPTER_NOT_FOUND(4402, "Chapter not found"),
    CHAPTER_NAME_DUPLICATE(4410, "Chapter name already exists in this subject"),
    QUESTION_NOT_FOUND(4403, "Question not found"),
    EXAM_NOT_FOUND(4404, "Exam not found"),
    EXAM_NO_QUESTIONS(4405, "Exam must contain at least one question"),
    EXAM_INVALID_DURATION(4406, "Invalid exam duration"),
    QUESTION_INVALID_CORRECT_ANSWER(4407, "Invalid correct answer"),
    EXAM_ALREADY_PUBLISHED(4408, "Exam already published"),

    QUESTION_NOT_AUTHORIZED(4450, "Not authorized to modify this question"),

    // ══════════════════════════════════════════════════════
    // 5000–5999  Room
    // ══════════════════════════════════════════════════════
    ROOM_CREATED_SUCCESS(5000, "Room created successfully"),
    ROOM_UPDATED_SUCCESS(5001, "Room updated successfully"),
    ROOM_JOIN_SUCCESS(5002, "Joined room successfully"),

    ROOM_NOT_FOUND(5401, "Room not found"),
    ROOM_CODE_INVALID(5402, "Invalid room code"),
    ROOM_NOT_OPEN(5403, "Room is not open"),
    ROOM_ALREADY_CLOSED(5404, "Room already closed"),
    ROOM_ALREADY_JOINED(5405, "User already joined room"),
    ROOM_FULL(5406, "Room is full"),
    ROOM_EXAM_NOT_ASSIGNED(5407, "No exam assigned to room"),
    ROOM_INVALID_STATUS_TRANSITION(5408, "Invalid room status transition"),

    // ══════════════════════════════════════════════════════
    // 6000–6999  Exam Session
    // ══════════════════════════════════════════════════════
    EXAM_SESSION_STARTED(6000, "Exam session started"),
    EXAM_ANSWER_SAVED(6001, "Answer saved"),
    EXAM_SUBMIT_SUCCESS(6002, "Exam submitted successfully"),
    EXAM_AUTO_SUBMIT_SUCCESS(6003, "Exam auto submitted"),
    EXAM_RESULT_FETCH_SUCCESS(6004, "Result fetched successfully"),
    EXAM_SESSION_FETCH_SUCCESS(6005, "Exam session fetched successfully"),
    EXAM_SESSION_LIST_SUCCESS(6006, "Exam session list fetched successfully"),
    EXAM_ANSWERS_SAVED(6007, "Answers saved successfully"),

    EXAM_SESSION_NOT_FOUND(6401, "Exam session not found"),
    EXAM_SESSION_ALREADY_STARTED(6402, "Exam session already started"),
    EXAM_SESSION_NOT_STARTED(6403, "Exam session not started"),
    EXAM_SESSION_ALREADY_SUBMITTED(6404, "Exam session already submitted"),
    EXAM_SESSION_EXPIRED(6405, "Exam session expired"),
    EXAM_QUESTION_NOT_IN_SESSION(6406, "Question does not belong to session"),
    EXAM_ANSWER_INVALID(6407, "Invalid answer"),
    EXAM_RESULT_NOT_READY(6408, "Result is not ready"),

    // ══════════════════════════════════════════════════════
    // 7000–7999  File / Import / Export
    // ══════════════════════════════════════════════════════
    IMPORT_SUCCESS(7000, "Import successful"),
    EXPORT_SUCCESS(7001, "Export successful"),

    FILE_UPLOAD_FAILED(7401, "File upload failed"),
    FILE_TOO_LARGE(7402, "File too large"),
    FILE_TYPE_NOT_SUPPORTED(7403, "File type not supported"),
    IMPORT_FAILED(7404, "Import failed"),
    IMPORT_PARTIAL_SUCCESS(7405, "Import partially successful"),

    // ══════════════════════════════════════════════════════
    // 8000–8999  Statistics
    // ══════════════════════════════════════════════════════
    STATS_ADMIN_OVERVIEW_SUCCESS(8000, "Admin stats fetched successfully"),

    // ══════════════════════════════════════════════════════
    // 9000–9999  Async / Background
    // ══════════════════════════════════════════════════════
    EVENT_PUBLISH_FAILED(9001, "Failed to publish event"),
    GRADING_FAILED(9002, "Failed to grade exam"),
    LEADERBOARD_UPDATE_FAILED(9003, "Failed to update leaderboard"),
    AUDIT_LOG_FAILED(9004, "Failed to write audit log");

    private final int code;
    private final String message;
}
