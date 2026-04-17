package com.chatterai.message.service;

import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import com.chatterai.config.AiProperties;
import com.chatterai.config.RedisMessagePublisher;
import com.chatterai.message.dto.ChatEventDto;
import com.chatterai.message.dto.ChatMessageResponseDto;
import com.chatterai.message.entity.Message;
import com.chatterai.message.entity.MessageType;
import com.chatterai.message.repository.MessageRepository;
import com.chatterai.user.entity.User;
import com.chatterai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiUsageRepository aiUsageRepository;
    private final AiProperties aiProperties;
    private final RedisMessagePublisher redisPublisher;
    private final RestClient claudeRestClient;

    /**
     * @AI 멘션 비동기 처리.
     * 1. 사용량 검증
     * 2. AI_LOADING 브로드캐스트
     * 3. Claude API 호출
     * 4. AI 응답 저장 + AI_RESPONSE 브로드캐스트
     * 5. 사용량 카운터 증가
     */
    @Async
    public void processAiMentionAsync(Long channelId, Long userId, String userContent, String tempId) {
        // 1. 서버 2차 사용량 검증
        int usedCount = aiUsageRepository.getUsageCount(userId);
        if (usedCount >= aiProperties.getDailyUsageLimit()) {
            broadcastAiError(channelId, tempId, "AI 일일 사용량을 초과했습니다. (한도: " + aiProperties.getDailyUsageLimit() + "회/일)");
            return;
        }

        // 2. AI_LOADING 브로드캐스트
        broadcastAiLoading(channelId, tempId);

        // 3. 컨텍스트 메시지 수집
        List<Message> recentMessages = messageRepository.findRecentByChannelId(
                channelId, PageRequest.of(0, aiProperties.getContextMessageCount()));
        Collections.reverse(recentMessages);

        // 4. Claude API 호출
        String aiResponse;
        boolean isError = false;
        try {
            aiResponse = callClaudeApi(userContent, recentMessages);
        } catch (ResourceAccessException e) {
            log.error("Claude API timeout: channelId={}, userId={}", channelId, userId, e);
            aiResponse = "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
            isError = true;
        } catch (Exception e) {
            log.error("Claude API error: channelId={}, userId={}", channelId, userId, e);
            aiResponse = "AI 응답 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            isError = true;
        }

        // 5. AI 응답 메시지 저장
        MessageType messageType = isError ? MessageType.AI_ERROR : MessageType.AI_RESPONSE;
        Message aiMessage = Message.builder()
                .channelId(channelId)
                .userId(null)
                .content(aiResponse)
                .type(messageType)
                .isAiMessage(true)
                .build();
        messageRepository.save(aiMessage);

        // 6. AI_RESPONSE / AI_ERROR 브로드캐스트
        ChatMessageResponseDto msgDto = ChatMessageResponseDto.of(
                aiMessage, "ChatterAI", Collections.emptyList(), Collections.emptyList());
        ChatEventDto event = ChatEventDto.builder()
                .eventType(isError ? "AI_ERROR" : "AI_RESPONSE")
                .tempId(tempId)
                .message(msgDto)
                .build();
        redisPublisher.publish("chat:channel:" + channelId, event);

        // 7. 사용량 카운터 증가 (오류여도 1회 차감)
        if (!isError) {
            aiUsageRepository.increment(userId);
        }
    }

    /**
     * Claude Messages API 호출.
     * 최근 메시지를 컨텍스트로 포함한다.
     */
    @SuppressWarnings("unchecked")
    private String callClaudeApi(String userQuestion, List<Message> contextMessages) {
        // 컨텍스트 메시지 → Claude messages 형식 변환
        List<Map<String, String>> messages = contextMessages.stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> {
                    String role = m.isAiMessage() ? "assistant" : "user";
                    return Map.of("role", role, "content", m.getContent());
                })
                .collect(Collectors.toList());

        // 현재 질문 추가 (컨텍스트 마지막이 user이면 별도 추가 불필요하지만 명시적으로 추가)
        // userQuestion은 @AI를 제거한 실제 질문
        String question = userQuestion.replace("@AI", "").trim();
        if (question.isEmpty()) {
            question = "안녕하세요!";
        }

        // 마지막 메시지가 이미 동일 질문이면 중복 방지
        if (messages.isEmpty() || !messages.get(messages.size() - 1).get("content").equals(question)) {
            messages.add(Map.of("role", "user", "content", question));
        }

        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getClaude().getModel(),
                "max_tokens", 1024,
                "system", "당신은 ChatterAI 채팅 플랫폼의 AI 어시스턴트입니다. 사용자의 질문에 친절하고 간결하게 답변해 주세요.",
                "messages", messages
        );

        Map<String, Object> response = claudeRestClient.post()
                .uri("/v1/messages")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new CustomException(ErrorCode.AI_API_ERROR);
        }

        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new CustomException(ErrorCode.AI_API_ERROR);
        }

        return (String) content.get(0).get("text");
    }

    /**
     * DM에서 @AI 멘션 비동기 처리.
     */
    @Async
    public void processDmAiMentionAsync(Long dmRoomId, Long userId, String userContent, String tempId) {
        // 서버 2차 사용량 검증
        int usedCount = aiUsageRepository.getUsageCount(userId);
        if (usedCount >= aiProperties.getDailyUsageLimit()) {
            broadcastDmAiError(dmRoomId, tempId, "AI 일일 사용량을 초과했습니다. (한도: " + aiProperties.getDailyUsageLimit() + "회/일)");
            return;
        }

        // AI_LOADING 브로드캐스트
        ChatEventDto loadingEvent = ChatEventDto.builder()
                .eventType("AI_LOADING")
                .tempId(tempId)
                .content("AI가 응답을 생성 중입니다...")
                .build();
        redisPublisher.publish("chat:dm:" + dmRoomId, loadingEvent);

        // DM 컨텍스트 메시지 수집
        List<Message> recentMessages = messageRepository.findRecentByDmRoomId(
                dmRoomId, PageRequest.of(0, aiProperties.getContextMessageCount()));
        Collections.reverse(recentMessages);

        // Claude API 호출
        String aiResponse;
        boolean isError = false;
        try {
            aiResponse = callClaudeApi(userContent, recentMessages);
        } catch (ResourceAccessException e) {
            log.error("Claude API timeout: dmRoomId={}, userId={}", dmRoomId, userId, e);
            aiResponse = "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
            isError = true;
        } catch (Exception e) {
            log.error("Claude API error: dmRoomId={}, userId={}", dmRoomId, userId, e);
            aiResponse = "AI 응답 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            isError = true;
        }

        // AI 응답 메시지 저장
        MessageType messageType = isError ? MessageType.AI_ERROR : MessageType.AI_RESPONSE;
        Message aiMessage = Message.builder()
                .dmRoomId(dmRoomId)
                .userId(null)
                .content(aiResponse)
                .type(messageType)
                .isAiMessage(true)
                .build();
        messageRepository.save(aiMessage);

        ChatMessageResponseDto msgDto = ChatMessageResponseDto.of(
                aiMessage, "ChatterAI", Collections.emptyList(), Collections.emptyList());
        ChatEventDto event = ChatEventDto.builder()
                .eventType(isError ? "AI_ERROR" : "AI_RESPONSE")
                .tempId(tempId)
                .message(msgDto)
                .build();
        redisPublisher.publish("chat:dm:" + dmRoomId, event);

        if (!isError) {
            aiUsageRepository.increment(userId);
        }
    }

    private void broadcastAiLoading(Long channelId, String tempId) {
        ChatEventDto event = ChatEventDto.builder()
                .eventType("AI_LOADING")
                .tempId(tempId)
                .channelId(channelId)
                .content("AI가 응답을 생성 중입니다...")
                .build();
        redisPublisher.publish("chat:channel:" + channelId, event);
    }

    private void broadcastAiError(Long channelId, String tempId, String errorMessage) {
        Message errorMsg = Message.builder()
                .channelId(channelId)
                .userId(null)
                .content(errorMessage)
                .type(MessageType.AI_ERROR)
                .isAiMessage(true)
                .build();
        messageRepository.save(errorMsg);

        ChatMessageResponseDto msgDto = ChatMessageResponseDto.of(
                errorMsg, "ChatterAI", Collections.emptyList(), Collections.emptyList());
        ChatEventDto event = ChatEventDto.builder()
                .eventType("AI_ERROR")
                .tempId(tempId)
                .message(msgDto)
                .build();
        redisPublisher.publish("chat:channel:" + channelId, event);
    }

    private void broadcastDmAiError(Long dmRoomId, String tempId, String errorMessage) {
        Message errorMsg = Message.builder()
                .dmRoomId(dmRoomId)
                .userId(null)
                .content(errorMessage)
                .type(MessageType.AI_ERROR)
                .isAiMessage(true)
                .build();
        messageRepository.save(errorMsg);

        ChatMessageResponseDto msgDto = ChatMessageResponseDto.of(
                errorMsg, "ChatterAI", Collections.emptyList(), Collections.emptyList());
        ChatEventDto event = ChatEventDto.builder()
                .eventType("AI_ERROR")
                .tempId(tempId)
                .message(msgDto)
                .build();
        redisPublisher.publish("chat:dm:" + dmRoomId, event);
    }
}
