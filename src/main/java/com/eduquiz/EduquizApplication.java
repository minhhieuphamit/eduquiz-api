package com.eduquiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Cần cho Scheduler tự động mở/đóng phòng thi
public class EduquizApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduquizApplication.class, args);
    }
}
