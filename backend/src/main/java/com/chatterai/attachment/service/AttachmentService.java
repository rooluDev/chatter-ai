package com.chatterai.attachment.service;

import com.chatterai.attachment.entity.Attachment;
import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class AttachmentService {

    private static final int MAX_FILE_COUNT = 5;
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;  // 20MB

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> FILE_EXTENSIONS  = Set.of("pdf", "zip", "txt", "md");

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public record UploadResult(String fileUrl, String fileName, long fileSize, Attachment.FileType fileType) {}

    /**
     * 파일 업로드 처리.
     * 1. 개수/크기/확장자 검증
     * 2. 로컬 저장
     * 3. 업로드 결과 반환
     */
    public List<UploadResult> upload(List<MultipartFile> files) {
        if (files.size() > MAX_FILE_COUNT) {
            throw new CustomException(ErrorCode.FILE_COUNT_EXCEEDED);
        }

        List<UploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(saveFile(file));
        }
        return results;
    }

    private UploadResult saveFile(MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String ext = extractExtension(originalName).toLowerCase();

        Attachment.FileType fileType = resolveFileType(ext);
        validateSize(file.getSize(), fileType);

        // 저장 경로: ./uploads/chat/{yyyy}/{MM}/
        LocalDate today = LocalDate.now();
        String datePath = String.format("chat/%d/%02d", today.getYear(), today.getMonthValue());
        Path dir = Paths.get(uploadDir, datePath);

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", dir, e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String savedName = UUID.randomUUID() + "-" + sanitizeFileName(originalName);
        Path dest = dir.resolve(savedName);

        try {
            file.transferTo(dest.toFile());
        } catch (IOException e) {
            log.error("Failed to save file: {}", dest, e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String fileUrl = "/api/files/uploads/" + datePath + "/" + savedName;
        return new UploadResult(fileUrl, originalName, file.getSize(), fileType);
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1) : "";
    }

    private Attachment.FileType resolveFileType(String ext) {
        if (IMAGE_EXTENSIONS.contains(ext)) return Attachment.FileType.IMAGE;
        if (FILE_EXTENSIONS.contains(ext))  return Attachment.FileType.FILE;
        throw new CustomException(ErrorCode.FILE_EXTENSION_INVALID);
    }

    private void validateSize(long size, Attachment.FileType fileType) {
        long maxSize = fileType == Attachment.FileType.IMAGE ? MAX_IMAGE_SIZE : MAX_FILE_SIZE;
        if (size > maxSize) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    private String sanitizeFileName(String name) {
        // 경로 순회 방지: 슬래시·백슬래시 제거
        return name.replaceAll("[/\\\\]", "_");
    }
}
