package com.chatterai.channel.service;

import com.chatterai.channel.dto.ChannelDetailResponseDto;
import com.chatterai.channel.dto.ChannelResponseDto;
import com.chatterai.channel.entity.Channel;
import com.chatterai.channel.entity.ChannelParticipant;
import com.chatterai.channel.repository.ChannelParticipantRepository;
import com.chatterai.channel.repository.ChannelRepository;
import com.chatterai.channel.repository.UnreadCountRepository;
import com.chatterai.common.CustomException;
import com.chatterai.common.ErrorCode;
import com.chatterai.message.entity.Message;
import com.chatterai.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelParticipantRepository channelParticipantRepository;
    private final UnreadCountRepository unreadCountRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<ChannelResponseDto> getMyChannels(Long userId) {
        List<Channel> channels = channelRepository.findByParticipantUserId(userId);

        return channels.stream().map(channel -> {
            long unreadCount = unreadCountRepository.getCount(userId, channel.getId());

            Optional<Message> lastMsg = messageRepository
                    .findTopByChannelIdAndIsDeletedFalseOrderByIdDesc(channel.getId());

            ChannelResponseDto.LastMessage lastMessage = lastMsg.map(m ->
                    new ChannelResponseDto.LastMessage(m.getContent(), m.getCreatedAt())
            ).orElse(null);

            return new ChannelResponseDto(channel, unreadCount, lastMessage);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChannelDetailResponseDto getChannelDetail(Long channelId, Long userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        if (channel.isPrivate() && !channelParticipantRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new CustomException(ErrorCode.CHANNEL_ACCESS_DENIED);
        }

        long participantCount = channelParticipantRepository.countByChannelId(channelId);

        ChannelDetailResponseDto.NoticeMessage noticeMessage = null;
        if (channel.getNoticeMessageId() != null) {
            noticeMessage = messageRepository.findById(channel.getNoticeMessageId())
                    .filter(m -> !m.isDeleted())
                    .map(m -> new ChannelDetailResponseDto.NoticeMessage(m.getId(), m.getContent(), m.getCreatedAt()))
                    .orElse(null);
        }

        return new ChannelDetailResponseDto(channel, participantCount, noticeMessage);
    }

    @Transactional
    public void joinChannel(Long channelId, Long userId) {
        channelRepository.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        if (!channelParticipantRepository.existsByChannelIdAndUserId(channelId, userId)) {
            channelParticipantRepository.save(
                    ChannelParticipant.builder()
                            .channelId(channelId)
                            .userId(userId)
                            .build()
            );
        }
    }

    @Transactional
    public void markAsRead(Long channelId, Long userId) {
        channelRepository.findById(channelId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHANNEL_NOT_FOUND));

        unreadCountRepository.reset(userId, channelId);
    }
}
