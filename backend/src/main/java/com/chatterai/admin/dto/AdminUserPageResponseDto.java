package com.chatterai.admin.dto;

import com.chatterai.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AdminUserPageResponseDto {

    private final List<UserItem> content;
    private final long totalCount;
    private final int totalPages;
    private final int currentPage;
    private final int pageSize;

    public AdminUserPageResponseDto(List<UserItem> content, long totalCount, int page, int size) {
        this.content = content;
        this.totalCount = totalCount;
        this.totalPages = (int) Math.ceil((double) totalCount / size);
        this.currentPage = page;
        this.pageSize = size;
    }

    @Getter
    public static class UserItem {
        private final Long id;
        private final String username;
        private final String nickname;
        private final String role;
        private final boolean isActive;
        private final boolean isOnline;
        private final LocalDateTime createdAt;

        public UserItem(User user, boolean isOnline) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.role = user.getRole().name();
            this.isActive = user.isActive();
            this.isOnline = isOnline;
            this.createdAt = user.getCreatedAt();
        }
    }
}
