package com.chatterai.admin.service;

import com.chatterai.admin.dto.AdminChannelCreateRequestDto;
import com.chatterai.admin.dto.AdminChannelResponseDto;
import com.chatterai.admin.dto.AdminNoticeRequestDto;
import com.chatterai.admin.dto.AdminUserPageResponseDto;
import com.chatterai.auth.jwt.RefreshTokenRepository;
import com.chatterai.channel.entity.Channel;
import com.chatterai.channel.entity.ChannelParticipant;
import com.chatterai.channel.repository.ChannelParticipantRepository;
import com.chatterai.channel.repository.ChannelRepository;
import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import com.chatterai.config.RedisMessagePublisher;
import com.chatterai.message.dto.ChatEventDto;
import com.chatterai.message.entity.Message;
import com.chatterai.message.repository.MessageRepository;
import com.chatterai.user.entity.User;
import com.chatterai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String PRESENCE_KEY_PREFIX = "presence:";

    private final ChannelRepository channelRepository;
    private final ChannelParticipantRepository channelParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisMessagePublisher redisPublisher;
    private final RedisTemplate<String, String> redisTemplate;

    // ── 채널 관리 ──────────────────────────────────────────────

    /**
     * 채널 생성.
     */
    @Transactional
    public AdminChannelResponseDto createChannel(AdminChannelCreateRequestDto request, Long adminUserId) {
        if (channelRepository.existsByName(request.getName())) {
            throw new CustomException(ErrorCode.DUPLICATE_CHANNEL_NAME);
        }

        Channel channel = channelRepository.save(Channel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isPrivate(request.isPrivate())
                .createdBy(adminUserId)
                .build());

        // 생성한 관리자를 첫 번째 참여자로 추가
        channelParticipantRepository.save(ChannelParticipant.builder()
                .channelId(channel.getId())
                .userId(adminUserId)
                .build());

        return new AdminChannelResponseDto(channel);
    }

    /**
     * 채널 삭제.
     * 삭제 전 구독자에게 CHANNEL_DELETED 이벤트 브로드캐스트.
     */
    @Transactional
    public void deleteChannel(Long channelId) {
        channelRepository.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        // CHANNEL_DELETED 브로드캐스트 (DB 삭제 전에)
        ChatEventDto event = ChatEventDto.builder()
                .eventType("CHANNEL_DELETED")
                .channelId(channelId)
                .build();
        redisPublisher.publish("chat:channel:" + channelId, event);

        channelRepository.deleteById(channelId);
    }

    // ── 공지 관리 ──────────────────────────────────────────────

    /**
     * 공지 메시지 지정.
     * 지정 후 NOTICE_UPDATED 브로드캐스트.
     */
    @Transactional
    public void setNoticeMessage(Long channelId, AdminNoticeRequestDto request) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        if (message.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_MESSAGE);
        }

        channel.setNoticeMessageId(message.getId());
        channelRepository.save(channel);

        ChatEventDto.NoticeMessageInfo noticeInfo = ChatEventDto.NoticeMessageInfo.builder()
                .id(message.getId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();

        ChatEventDto event = ChatEventDto.builder()
                .eventType("NOTICE_UPDATED")
                .channelId(channelId)
                .noticeMessage(noticeInfo)
                .build();
        redisPublisher.publish("chat:channel:" + channelId, event);
    }

    /**
     * 공지 메시지 해제.
     * 해제 후 NOTICE_UPDATED 브로드캐스트 (noticeMessage: null).
     */
    @Transactional
    public void removeNoticeMessage(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        channel.setNoticeMessageId(null);
        channelRepository.save(channel);

        ChatEventDto event = ChatEventDto.builder()
                .eventType("NOTICE_UPDATED")
                .channelId(channelId)
                .build();
        redisPublisher.publish("chat:channel:" + channelId, event);
    }

    // ── 회원 관리 ──────────────────────────────────────────────

    /**
     * 회원 목록 조회 (페이지 기반, 닉네임 검색 가능).
     */
    @Transactional(readOnly = true)
    public AdminUserPageResponseDto getUsers(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<User> userPage;
        if (StringUtils.hasText(keyword)) {
            userPage = userRepository.findByNicknameContainingIgnoreCase(keyword, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<AdminUserPageResponseDto.UserItem> items = userPage.getContent().stream()
                .map(user -> {
                    boolean isOnline = Boolean.TRUE.equals(
                            redisTemplate.hasKey(PRESENCE_KEY_PREFIX + user.getId()));
                    return new AdminUserPageResponseDto.UserItem(user, isOnline);
                })
                .collect(Collectors.toList());

        return new AdminUserPageResponseDto(items, userPage.getTotalElements(), page, size);
    }

    /**
     * 회원 비활성화.
     * Redis Refresh Token 삭제 → 즉시 세션 만료.
     */
    @Transactional
    public void disableUser(Long targetUserId, Long adminUserId) {
        if (targetUserId.equals(adminUserId)) {
            throw new CustomException(ErrorCode.SELF_DISABLE_NOT_ALLOWED);
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.deactivate();
        userRepository.save(user);
        refreshTokenRepository.delete(targetUserId);
    }

    /**
     * 회원 활성화.
     */
    @Transactional
    public void enableUser(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.activate();
        userRepository.save(user);
    }
}
