package com.chatterai.dm.dto;

import com.chatterai.user.entity.User;
import lombok.Getter;

@Getter
public class DmRoomDetailResponseDto {

    private final Long id;
    private final OpponentInfo opponent;

    public DmRoomDetailResponseDto(Long dmRoomId, User opponent, boolean isOnline) {
        this.id = dmRoomId;
        this.opponent = new OpponentInfo(opponent.getId(), opponent.getNickname(), isOnline);
    }

    @Getter
    public static class OpponentInfo {
        private final Long id;
        private final String nickname;
        private final boolean isOnline;

        public OpponentInfo(Long id, String nickname, boolean isOnline) {
            this.id = id;
            this.nickname = nickname;
            this.isOnline = isOnline;
        }
    }
}
