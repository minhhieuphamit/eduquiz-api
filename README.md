# EduQuiz API

> Backend REST API cho hệ thống thi trắc nghiệm THPT Quốc Gia.

## Tech Stack

- **Java 17** + **Spring Boot 3**
- **PostgreSQL 16** + Flyway migration
- **Apache Kafka** (async grading, leaderboard, audit)
- **JWT** (authentication + authorization)
- **Email OTP** (Spring Mail)
- **ELK Stack + Grafana** (logging & monitoring)
- **Docker** + GitHub Actions (CI/CD)

## Quick Start

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Run application
./gradlew bootRun

# 3. API docs
open http://localhost:8080/swagger-ui.html
```

## Project Structure

Xem `STRUCTURE.md` cho chi tiết cấu trúc project.
