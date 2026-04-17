package com.chatterai.auth.dto;

import com.chatterai.user.entity.User;
import lombok.Getter;

@Getter
public class LoginResponseDto {

    private final String accessToken;
    private final UserInfo user;

    public LoginResponseDto(String accessToken, User user) {
        this.accessToken = accessToken;
        this.user = new UserInfo(user);
    }

    @Getter
    public static class UserInfo {
        private final Long id;
        private final String username;
        private final String nickname;
        private final String role;

        public UserInfo(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.role = user.getRole().name();
        }
    }
}
