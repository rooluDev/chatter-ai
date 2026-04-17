package com.chatterai.dm.service;

import com.chatterai.common.CursorPageResponse;
import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import com.chatterai.dm.dto.DmRoomDetailResponseDto;
import com.chatterai.dm.dto.DmRoomListResponseDto;
import com.chatterai.dm.entity.DmParticipant;
import com.chatterai.dm.entity.DmRoom;
import com.chatterai.dm.repository.DmParticipantRepository;
import com.chatterai.dm.repository.DmRoomRepository;
import com.chatterai.dm.repository.DmUnreadCountRepository;
import com.chatterai.message.dto.MessageResponseDto;
import com.chatterai.message.entity.Message;
import com.chatterai.message.repository.MessageRepository;
import com.chatterai.attachment.entity.Attachment;
import com.chatterai.attachment.repository.AttachmentRepository;
import com.chatterai.reaction.entity.Reaction;
import com.chatterai.reaction.repository.ReactionRepository;
import com.chatterai.user.entity.User;
import com.chatterai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DmService {

    private static final int MAX_SIZE = 50;
    private static final String PRESENCE_KEY_PREFIX = "presence:";

    private final DmRoomRepository dmRoomRepository;
    private final DmParticipantRepository dmParticipantRepository;
    private final DmUnreadCountRepository dmUnreadCountRepository;
    private final MessageRepository messageRepository;
    private final AttachmentRepository attachmentRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 내 DM 방 목록 조회.
     * 최근 메시지 기준 내림차순 정렬.
     */
    @Transactional(readOnly = true)
    public List<DmRoomListResponseDto> getMyDmRooms(Long userId) {
        List<DmParticipant> myParticipations = dmParticipantRepository.findByUserId(userId);

        List<DmRoomListResponseDto> result = new ArrayList<>();
        for (DmParticipant myPart : myParticipations) {
            Long dmRoomId = myPart.getDmRoomId();

            // 상대방 참여자 조회
            Optional<DmParticipant> opponentPartOpt = dmParticipantRepository.findByDmRoomId(dmRoomId)
                    .stream()
                    .filter(p -> !p.getUserId().equals(userId))
                    .findFirst();
            if (opponentPartOpt.isEmpty()) continue;

            User opponent = userRepository.findById(opponentPartOpt.get().getUserId()).orElse(null);
            if (opponent == null) continue;

            boolean isOnline = isUserOnline(opponent.getId());  // presence Redis 키 확인
            long unreadCount = dmUnreadCountRepository.getCount(userId, dmRoomId);
            Message lastMsg = messageRepository.findTopByDmRoomIdAndIsDeletedFalseOrderByIdDesc(dmRoomId).orElse(null);

            result.add(new DmRoomListResponseDto(dmRoomId, opponent, isOnline, unreadCount, lastMsg));
        }

        // 최근 메시지 시간 기준 내림차순 정렬
        result.sort((a, b) -> {
            var aTime = a.getLastMessage() != null ? a.getLastMessage().getCreatedAt() : null;
            var bTime = b.getLastMessage() != null ? b.getLastMessage().getCreatedAt() : null;
            if (aTime == null && bTime == null) return 0;
            if (aTime == null) return 1;
            if (bTime == null) return -1;
            return bTime.compareTo(aTime);
        });

        return result;
    }

    /**
     * DM 방 생성 또는 기존 방 반환.
     * @return [isNew, dmRoomId, opponentUser]
     */
    @Transactional
    public Object[] getOrCreateDmRoom(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.SELF_DM_NOT_ALLOWED);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 방 조회
        Optional<Long> existingRoomId = dmRoomRepository.findDmRoomIdBetweenUsers(userId, targetUserId);
        if (existingRoomId.isPresent()) {
            return new Object[]{false, existingRoomId.get(), targetUser};
        }

        // 새 방 생성
        DmRoom newRoom = dmRoomRepository.save(DmRoom.create());
        dmParticipantRepository.save(DmParticipant.builder()
                .dmRoomId(newRoom.getId()).userId(userId).build());
        dmParticipantRepository.save(DmParticipant.builder()
                .dmRoomId(newRoom.getId()).userId(targetUserId).build());

        return new Object[]{true, newRoom.getId(), targetUser};
    }

    /**
     * DM 메시지 목록 조회 (커서 기반 페이지네이션).
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<MessageResponseDto> getDmMessages(
            Long dmRoomId, Long userId, Long beforeId, int size) {

        dmRoomRepository.findById(dmRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.DM_ROOM_NOT_FOUND));

        if (!dmParticipantRepository.existsByDmRoomIdAndUserId(dmRoomId, userId)) {
            throw new CustomException(ErrorCode.DM_ACCESS_DENIED);
        }

        int fetchSize = Math.min(size, MAX_SIZE);
        PageRequest pageable = PageRequest.of(0, fetchSize + 1);

        List<Message> messages = beforeId == null
                ? messageRepository.findByDmRoomIdOrderByIdDesc(dmRoomId, pageable)
                : messageRepository.findByDmRoomIdBeforeIdOrderByIdDesc(dmRoomId, beforeId, pageable);

        boolean hasNext = messages.size() > fetchSize;
        if (hasNext) messages = messages.subList(0, fetchSize);

        List<Message> ordered = new ArrayList<>(messages);
        Collections.reverse(ordered);

        Long nextCursorId = hasNext ? messages.get(messages.size() - 1).getId() : null;

        return new CursorPageResponse<>(buildMessageDtos(ordered, userId), hasNext, nextCursorId);
    }

    /**
     * DM 읽음 처리 — 안읽은 메시지 수 초기화.
     */
    @Transactional
    public void markDmAsRead(Long dmRoomId, Long userId) {
        if (!dmParticipantRepository.existsByDmRoomIdAndUserId(dmRoomId, userId)) {
            throw new CustomException(ErrorCode.DM_ACCESS_DENIED);
        }
        dmUnreadCountRepository.reset(userId, dmRoomId);
    }

    /**
     * DM 방 참여 여부 확인 (DmMessageController에서 사용).
     */
    public boolean isParticipant(Long dmRoomId, Long userId) {
        return dmParticipantRepository.existsByDmRoomIdAndUserId(dmRoomId, userId);
    }

    /**
     * DM 방 참여자 목록 조회 (안읽은 수 INCR용).
     */
    public List<Long> getParticipantUserIds(Long dmRoomId) {
        return dmParticipantRepository.findByDmRoomId(dmRoomId)
                .stream()
                .map(DmParticipant::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * 안읽은 메시지 수 증가 (메시지 전송 시 상대방 카운터 올림).
     */
    public void incrementUnreadForOthers(Long dmRoomId, Long senderId) {
        getParticipantUserIds(dmRoomId).stream()
                .filter(uid -> !uid.equals(senderId))
                .forEach(uid -> dmUnreadCountRepository.increment(uid, dmRoomId));
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────

    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PRESENCE_KEY_PREFIX + userId));
    }

    private List<MessageResponseDto> buildMessageDtos(List<Message> messages, Long currentUserId) {
        if (messages.isEmpty()) return Collections.emptyList();

        List<Long> messageIds = messages.stream().map(Message::getId).collect(Collectors.toList());

        Map<Long, List<Attachment>> attachmentsByMsgId = attachmentRepository
                .findByMessageIdInOrderBySortOrderAsc(messageIds)
                .stream()
                .collect(Collectors.groupingBy(Attachment::getMessageId));

        Map<Long, List<Reaction>> reactionsByMsgId = reactionRepository
                .findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(Reaction::getMessageId));

        Set<Long> userIds = messages.stream()
                .filter(m -> m.getUserId() != null)
                .map(Message::getUserId)
                .collect(Collectors.toSet());

        Map<Long, String> nicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return messages.stream().map(message -> new MessageResponseDto(
                message,
                message.getUserId() != null ? nicknameMap.getOrDefault(message.getUserId(), "알 수 없음") : null,
                currentUserId,
                attachmentsByMsgId.getOrDefault(message.getId(), Collections.emptyList()),
                reactionsByMsgId.getOrDefault(message.getId(), Collections.emptyList())
        )).collect(Collectors.toList());
    }
}
