package com.chatterai.message.dto;

import com.chatterai.attachment.entity.Attachment;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class ChatSendRequestDto {

    private String content;
    private String type;    // TEXT | FILE
    private String tempId;  // 클라이언트 optimistic ID, 브로드캐스트 시 echo
    private List<AttachmentRequest> attachments;

    public List<AttachmentRequest> getAttachments() {
        return attachments == null ? Collections.emptyList() : attachments;
    }

    @Getter
    public static class AttachmentRequest {
        private String fileUrl;
        private String fileName;
        private Long fileSize;
        private String fileType;  // IMAGE | FILE
    }

    public Attachment.FileType resolveFileType(AttachmentRequest req) {
        try {
            return Attachment.FileType.valueOf(req.getFileType());
        } catch (Exception e) {
            return Attachment.FileType.FILE;
        }
    }
}
