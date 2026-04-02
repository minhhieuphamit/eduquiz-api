package com.eduquiz.common.util;

import java.security.SecureRandom;

/**
 * Room Code Generator - tạo mã phòng 6 ký tự.
 * Uppercase + digits, loại ký tự dễ nhầm (0/O, 1/I/L)
 */
public class RoomCodeGenerator {

    private static final String CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRoomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}
