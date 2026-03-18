package com.eduquiz.common.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.max-size-mb}")
    private int maxSizeMb;

    @Value("${file.allowed-types}")
    private String allowedTypes;

    @Value("${file.base-url}")
    private String baseUrl;

    private List<String> allowedTypeList;

    @PostConstruct
    public void init() {
        allowedTypeList = Arrays.asList(allowedTypes.split(","));
        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath.resolve("subjects"));
            Files.createDirectories(uploadPath.resolve("avatars"));
            Files.createDirectories(uploadPath.resolve("questions"));
            log.info("[FileStorageService.init] Upload directories created at: {}", uploadPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[FileStorageService.init] FAILED - cannot create upload directories", e);
            throw new RuntimeException("Không thể tạo thư mục upload", e);
        }
    }

    /**
     * Upload file vào subfolder (vd: "subjects", "avatars", "questions")
     * @return đường dẫn tương đối: "subjects/uuid-filename.png"
     */
    public String store(MultipartFile file, String subfolder) {
        log.info("[FileStorageService.store] START - subfolder={}, originalName={}, size={}KB, type={}",
                subfolder, file.getOriginalFilename(), file.getSize() / 1024, file.getContentType());

        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String newFilename = UUID.randomUUID() + extension;
        String relativePath = subfolder + "/" + newFilename;

        try {
            Path targetPath = Paths.get(uploadDir).resolve(relativePath);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[FileStorageService.store] SUCCESS - path={}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("[FileStorageService.store] FAILED - cannot save file: {}", relativePath, e);
            throw new RuntimeException("Không thể lưu file", e);
        }
    }

    /**
     * Xoá file theo đường dẫn tương đối
     */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        try {
            Path filePath = Paths.get(uploadDir).resolve(relativePath);
            if (Files.deleteIfExists(filePath)) {
                log.info("[FileStorageService.delete] SUCCESS - deleted: {}", relativePath);
            } else {
                log.warn("[FileStorageService.delete] File not found: {}", relativePath);
            }
        } catch (IOException e) {
            log.error("[FileStorageService.delete] FAILED - cannot delete: {}", relativePath, e);
        }
    }

    /**
     * Tạo full URL từ relative path
     * vd: "subjects/abc.png" → "http://localhost:8080/uploads/subjects/abc.png"
     */
    public String toFullUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        return baseUrl + "/" + relativePath;
    }

    /**
     * Trích xuất relative path từ full URL
     * vd: "http://localhost:8080/uploads/subjects/abc.png" → "subjects/abc.png"
     */
    public String toRelativePath(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) return null;
        if (fullUrl.startsWith(baseUrl)) {
            return fullUrl.substring(baseUrl.length() + 1);
        }
        return fullUrl;
    }

    // ── Private helpers ──

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("[FileStorageService.validateFile] FAILED - file is empty");
            throw new BadRequestException(ResponseCode.BAD_REQUEST, "File không được để trống");
        }

        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            log.warn("[FileStorageService.validateFile] FAILED - file too large: {}MB, max={}MB", file.getSize() / (1024 * 1024), maxSizeMb);
            throw new BadRequestException(ResponseCode.BAD_REQUEST,
                    "File không được vượt quá " + maxSizeMb + "MB");
        }

        if (!allowedTypeList.contains(file.getContentType())) {
            log.warn("[FileStorageService.validateFile] FAILED - invalid type: {}, allowed={}", file.getContentType(), allowedTypeList);
            throw new BadRequestException(ResponseCode.BAD_REQUEST,
                    "Chỉ chấp nhận file ảnh: JPEG, PNG, GIF, WebP");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".png";
        int dotIndex = filename.lastIndexOf(".");
        return dotIndex >= 0 ? filename.substring(dotIndex) : ".png";
    }
}
