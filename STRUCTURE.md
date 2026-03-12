# EduQuiz API - Project Structure

```
eduquiz-api/
│
├── src/main/java/com/eduquiz/
│   │
│   ├── EduquizApplication.java              # Main entry point (@EnableScheduling)
│   │
│   ├── config/                              # ⚙️ Global configurations
│   │   ├── CorsConfig.java                  #    CORS cho Angular frontend
│   │   └── SwaggerConfig.java               #    OpenAPI 3 + JWT scheme
│   │
│   ├── security/                            # 🔐 Security & Authentication
│   │   ├── SecurityConfig.java              #    SecurityFilterChain, role-based access
│   │   ├── jwt/
│   │   │   ├── JwtUtil.java                 #    Generate/validate JWT tokens
│   │   │   └── JwtAuthFilter.java           #    Extract & validate JWT per request
│   │   └── filter/
│   │       └── TraceFilter.java             #    Generate traceId (UUID) → MDC → logs
│   │
│   ├── common/                              # 🔧 Shared utilities
│   │   ├── dto/
│   │   │   ├── ApiResponse.java             #    Generic response: {success, message, data}
│   │   │   └── PageResponse.java            #    Paginated response wrapper
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java  #    @RestControllerAdvice (404, 400, 401...)
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── BadRequestException.java
│   │   │   ├── DuplicateResourceException.java
│   │   │   └── OtpVerificationException.java
│   │   └── util/
│   │       ├── OtpGenerator.java            #    Generate OTP 6 số (SecureRandom)
│   │       └── RoomCodeGenerator.java       #    Generate room code 6 ký tự
│   │
│   ├── feature/                             # 📦 Feature modules (mỗi feature độc lập)
│   │   │
│   │   ├── auth/                            # 🔑 Authentication & Authorization
│   │   │   ├── controller/
│   │   │   │   └── AuthController.java      #    /api/v1/auth/*
│   │   │   ├── dto/
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── VerifyOtpRequest.java
│   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   └── AuthResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── User.java                #    users table (STUDENT/TEACHER/ADMIN)
│   │   │   │   ├── Role.java                #    enum Role
│   │   │   │   ├── EmailVerification.java   #    email_verifications table (OTP)
│   │   │   │   └── RefreshToken.java        #    refresh_tokens table
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── EmailVerificationRepository.java
│   │   │   │   └── RefreshTokenRepository.java
│   │   │   └── service/
│   │   │       └── AuthService.java         #    Register, verify OTP, login, refresh, logout
│   │   │
│   │   ├── email/                           # 📧 Email Service (OTP)
│   │   │   └── service/
│   │   │       └── EmailService.java        #    Send OTP via Spring Mail + Thymeleaf
│   │   │
│   │   ├── user/                            # 👤 User Management (Admin)
│   │   │   ├── controller/
│   │   │   │   └── UserController.java      #    /api/v1/users/* (ADMIN only)
│   │   │   ├── dto/
│   │   │   │   └── UserResponse.java
│   │   │   └── service/
│   │   │       └── UserService.java
│   │   │
│   │   ├── subject/                         # 📚 Môn học
│   │   │   ├── controller/
│   │   │   │   └── SubjectController.java   #    /api/v1/subjects/*
│   │   │   ├── dto/
│   │   │   │   ├── SubjectRequest.java
│   │   │   │   └── SubjectResponse.java
│   │   │   ├── entity/
│   │   │   │   └── Subject.java             #    subjects table (có defaultDurationMinutes)
│   │   │   ├── repository/
│   │   │   │   └── SubjectRepository.java
│   │   │   └── service/
│   │   │       └── SubjectService.java
│   │   │
│   │   ├── chapter/                         # 📖 Chương
│   │   │   ├── controller/
│   │   │   │   └── ChapterController.java   #    /api/v1/subjects/{id}/chapters/*
│   │   │   ├── dto/
│   │   │   │   ├── ChapterRequest.java
│   │   │   │   └── ChapterResponse.java
│   │   │   ├── entity/
│   │   │   │   └── Chapter.java
│   │   │   ├── repository/
│   │   │   │   └── ChapterRepository.java
│   │   │   └── service/
│   │   │       └── ChapterService.java
│   │   │
│   │   ├── question/                        # ❓ Ngân hàng câu hỏi
│   │   │   ├── controller/
│   │   │   │   └── QuestionController.java  #    /api/v1/questions/* (TEACHER/ADMIN)
│   │   │   ├── dto/
│   │   │   │   ├── QuestionRequest.java     #    Content có thể chứa LaTeX
│   │   │   │   └── QuestionResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── Question.java            #    LaTeX lưu dạng plain text
│   │   │   │   └── Difficulty.java          #    enum: EASY, MEDIUM, HARD
│   │   │   ├── repository/
│   │   │   │   └── QuestionRepository.java  #    Có query random câu hỏi
│   │   │   └── service/
│   │   │       └── QuestionService.java
│   │   │
│   │   ├── exam/                            # 📝 Đề thi
│   │   │   ├── controller/
│   │   │   │   └── ExamController.java      #    /api/v1/exams/* (TEACHER)
│   │   │   ├── dto/
│   │   │   │   ├── CreateExamRequest.java
│   │   │   │   └── ExamResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── Exam.java                #    Có randomMode, durationMinutes
│   │   │   │   ├── ExamQuestion.java        #    Mapping exam ↔ question
│   │   │   │   └── RandomMode.java          #    enum: FULL_RANDOM, POOL_RANDOM, MANUAL
│   │   │   ├── repository/
│   │   │   │   ├── ExamRepository.java
│   │   │   │   └── ExamQuestionRepository.java
│   │   │   └── service/
│   │   │       └── ExamService.java         #    Tạo đề + random câu hỏi theo mode
│   │   │
│   │   ├── examroom/                        # 🏫 Phòng thi
│   │   │   ├── controller/
│   │   │   │   └── ExamRoomController.java  #    /api/v1/rooms/*
│   │   │   ├── dto/
│   │   │   │   ├── CreateRoomRequest.java
│   │   │   │   ├── JoinRoomRequest.java
│   │   │   │   ├── RoomResponse.java
│   │   │   │   └── RoomResultResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── ExamRoom.java            #    roomCode, startTime, endTime, status
│   │   │   │   ├── RoomParticipant.java     #    HS tham gia + đề được phát
│   │   │   │   └── RoomStatus.java          #    enum: SCHEDULED→OPEN→IN_PROGRESS→CLOSED
│   │   │   ├── repository/
│   │   │   │   ├── ExamRoomRepository.java
│   │   │   │   └── RoomParticipantRepository.java
│   │   │   └── service/
│   │   │       ├── ExamRoomService.java     #    Tạo phòng, join, phát đề random
│   │   │       └── RoomSchedulerService.java #   @Scheduled: tự động mở/đóng phòng
│   │   │
│   │   ├── examsession/                     # ✏️ Làm bài thi
│   │   │   ├── controller/
│   │   │   │   └── ExamSessionController.java #  /api/v1/exam-sessions/* (STUDENT)
│   │   │   ├── dto/
│   │   │   │   ├── StartExamRequest.java
│   │   │   │   ├── AnswerRequest.java
│   │   │   │   ├── ExamSessionResponse.java
│   │   │   │   └── ExamResultResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── ExamSession.java         #    room_id nullable (null = luyện tập)
│   │   │   │   ├── ExamAnswer.java
│   │   │   │   └── SessionStatus.java       #    enum: IN_PROGRESS, SUBMITTED, GRADED
│   │   │   ├── repository/
│   │   │   │   ├── ExamSessionRepository.java
│   │   │   │   └── ExamAnswerRepository.java
│   │   │   └── service/
│   │   │       └── ExamSessionService.java  #    Start, answer, submit → Kafka
│   │   │
│   │   ├── stats/                           # 📊 Thống kê
│   │   │   ├── controller/
│   │   │   │   └── StatsController.java     #    /api/v1/stats/*
│   │   │   ├── dto/
│   │   │   │   ├── StudentStatsResponse.java
│   │   │   │   ├── TeacherStatsResponse.java
│   │   │   │   └── AdminStatsResponse.java
│   │   │   └── service/
│   │   │       └── StatsService.java
│   │   │
│   │   └── leaderboard/                     # 🏆 Bảng xếp hạng
│   │       ├── controller/
│   │       │   └── LeaderboardController.java # /api/v1/stats/leaderboard
│   │       ├── dto/
│   │       │   └── LeaderboardEntry.java
│   │       └── service/
│   │           └── LeaderboardService.java  #    Cập nhật qua Kafka consumer
│   │
│   ├── kafka/                               # 📨 Kafka (Event-Driven)
│   │   ├── config/
│   │   │   ├── KafkaConfig.java             #    Producer/Consumer config
│   │   │   └── KafkaTopicConfig.java        #    Auto-create topics
│   │   ├── dto/
│   │   │   ├── ExamSubmissionEvent.java     #    Student nộp bài
│   │   │   ├── ExamGradedEvent.java         #    Chấm bài xong
│   │   │   └── AuditEvent.java              #    Ghi log hành động
│   │   ├── producer/
│   │   │   ├── ExamEventProducer.java       #    Publish exam events
│   │   │   └── AuditEventProducer.java      #    Publish audit events
│   │   └── consumer/
│   │       ├── GradingConsumer.java          #    Chấm bài tự động (async)
│   │       ├── LeaderboardConsumer.java      #    Cập nhật leaderboard
│   │       └── AuditConsumer.java            #    Ghi audit log vào DB
│   │
│   └── audit/                               # 📋 Audit Log
│       ├── entity/
│       │   └── AuditLog.java                #    audit_logs table (JSONB detail)
│       └── repository/
│           └── AuditLogRepository.java
│
├── src/main/resources/
│   ├── application.yml                      # Tất cả config (DB, Kafka, JWT, Mail, OTP...)
│   ├── logback-spring.xml                   # Logging config (Console + Logstash)
│   ├── db/migration/
│   │   └── V1__init_schema.sql              # Flyway: tạo tất cả tables
│   ├── logstash/
│   │   └── logstash.conf                    # Logstash pipeline (Phase 7)
│   └── templates/
│       └── otp-email.html                   # Thymeleaf email template (OTP)
│
├── src/test/java/com/eduquiz/               # Unit & Integration tests
│
├── .github/workflows/
│   └── backend-ci.yml                       # GitHub Actions CI pipeline
│
├── docker-compose.yml                       # PostgreSQL, Kafka, ELK (local dev)
├── Dockerfile                               # Multi-stage build (Gradle)
├── build.gradle                             # Dependencies & plugins
├── settings.gradle                          # Project name
├── gradle.properties                        # Build performance tuning
├── .gitignore
├── README.md
└── STRUCTURE.md                             # ← Bạn đang đọc file này
```

## Nguyên tắc tổ chức

1. **Feature-based**: Mỗi feature (auth, exam, examroom...) là 1 package độc lập chứa đủ
   controller/dto/entity/repository/service
2. **Tìm code dễ**: Muốn sửa phòng thi? → `feature/examroom/`. Muốn xem JWT? → `security/jwt/`
3. **Common dùng chung**: ApiResponse, PageResponse, exceptions, utilities
4. **Kafka tách riêng**: Producer/Consumer/DTO tách biệt khỏi business logic
5. **Config tập trung**: Tất cả trong `application.yml`, dùng env variables cho secrets
