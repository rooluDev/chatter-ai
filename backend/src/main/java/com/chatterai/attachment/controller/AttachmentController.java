package com.chatterai.attachment.controller;

import com.chatterai.attachment.entity.Attachment;
import com.chatterai.attachment.service.AttachmentService;
import com.chatterai.common.ApiResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * POST /api/files/upload
     * 파일 업로드 (최대 5개). 채팅 메시지 전송 전 URL 먼저 획득.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> upload(
            @RequestParam("files") List<MultipartFile> files) {

        List<AttachmentService.UploadResult> results = attachmentService.upload(files);
        List<FileUploadResponse> responses = results.stream()
                .map(FileUploadResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("업로드되었습니다.", responses));
    }

    @Getter
    public static class FileUploadResponse {
        private final String fileUrl;
        private final String fileName;
        private final long fileSize;
        private final String fileType;

        public FileUploadResponse(AttachmentService.UploadResult result) {
            this.fileUrl = result.fileUrl();
            this.fileName = result.fileName();
            this.fileSize = result.fileSize();
            this.fileType = result.fileType().name();
        }
    }
}
