package com.chatterai.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증
    INVALID_INPUT(400, "입력값이 올바르지 않습니다."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    REFRESH_TOKEN_EXPIRED(401, "세션이 만료되었습니다. 다시 로그인해 주세요."),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 인증 정보입니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    ACCOUNT_DISABLED(403, "비활성화된 계정입니다. 관리자에게 문의해 주세요."),

    // 사용자
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(409, "이미 사용 중인 아이디입니다."),
    DUPLICATE_NICKNAME(409, "이미 사용 중인 닉네임입니다."),
    SELF_DM_NOT_ALLOWED(400, "본인에게 DM을 보낼 수 없습니다."),
    SELF_DISABLE_NOT_ALLOWED(400, "본인 계정은 비활성화할 수 없습니다."),

    // 채널
    CHANNEL_NOT_FOUND(404, "채널을 찾을 수 없습니다."),
    DUPLICATE_CHANNEL_NAME(409, "이미 사용 중인 채널 이름입니다."),
    CHANNEL_ACCESS_DENIED(403, "채널에 접근할 권한이 없습니다."),

    // 메시지
    MESSAGE_NOT_FOUND(404, "메시지를 찾을 수 없습니다."),
    DELETED_MESSAGE(400, "이미 삭제된 메시지입니다."),
    AI_MESSAGE_NOT_EDITABLE(403, "AI 응답 메시지는 수정할 수 없습니다."),

    // DM
    DM_ROOM_NOT_FOUND(404, "DM 방을 찾을 수 없습니다."),
    DM_ACCESS_DENIED(403, "DM 방에 접근할 권한이 없습니다."),

    // 파일
    FILE_COUNT_EXCEEDED(400, "파일은 최대 5개까지 첨부할 수 있습니다."),
    FILE_SIZE_EXCEEDED(400, "파일 크기는 허용 용량을 초과할 수 없습니다."),
    FILE_EXTENSION_INVALID(400, "허용되지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(500, "파일 업로드 중 오류가 발생했습니다."),

    // 이모지 반응
    REACTION_ALREADY_EXISTS(409, "이미 반응한 이모지입니다."),
    REACTION_NOT_FOUND(404, "반응을 찾을 수 없습니다."),

    // AI
    AI_USAGE_EXCEEDED(429, "오늘의 AI 사용 횟수(20회)를 모두 사용했습니다."),
    AI_API_ERROR(502, "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    AI_API_TIMEOUT(504, "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."),

    // 공통
    SERVER_ERROR(500, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final int status;
    private final String message;
}
