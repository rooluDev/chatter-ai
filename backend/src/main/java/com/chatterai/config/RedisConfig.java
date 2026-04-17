package com.chatterai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Jackson ObjectMapper — LocalDateTime 직렬화 설정 포함.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Redis Pub/Sub 메시지 리스너 컨테이너.
     * chat:channel:* 와 chat:dm:* 패턴을 구독한다.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter chatMessageListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 채널 메시지 패턴 구독
        container.addMessageListener(chatMessageListenerAdapter, new PatternTopic("chat:channel:*"));
        // DM 메시지 패턴 구독
        container.addMessageListener(chatMessageListenerAdapter, new PatternTopic("chat:dm:*"));
        return container;
    }

    /**
     * RedisMessageSubscriber.handleMessage(String message, String channel) 를
     * Redis MessageListener 인터페이스에 위임하는 어댑터.
     */
    @Bean
    public MessageListenerAdapter chatMessageListenerAdapter(RedisMessageSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handleMessage");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }
}
