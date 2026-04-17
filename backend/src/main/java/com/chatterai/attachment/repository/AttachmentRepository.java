package com.chatterai.attachment.repository;

import com.chatterai.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByMessageIdInOrderBySortOrderAsc(List<Long> messageIds);
}
