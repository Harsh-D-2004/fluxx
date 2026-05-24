package com.project.fluxx.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.fluxx.PubSubService.service.FlagEventSubscriber;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    public static final String FLAG_EVENTS_CHANNEL = "flag-events";

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    ChannelTopic flagEventsTopic() {
        return new ChannelTopic(FLAG_EVENTS_CHANNEL);
    }

    @Bean
    MessageListenerAdapter flagEventsListenerAdapter(FlagEventSubscriber subscriber, ObjectMapper objectMapper) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "handleMessage");
        adapter.setSerializer(new JsonRedisSerializer<>(objectMapper, Map.class));
        return adapter;
    }

    @Bean
    RedisMessageListenerContainer redisListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter flagEventsListenerAdapter,
            ChannelTopic flagEventsTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(flagEventsListenerAdapter, flagEventsTopic);
        container.setErrorHandler(e -> log.error("Redis listener error: {}", e.getMessage()));
        return container;
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        JsonRedisSerializer<Object> jsonSerializer = new JsonRedisSerializer<>(objectMapper, Object.class);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        return template;
    }

    @EventListener
    void verifyRedisConnection(ApplicationReadyEvent event) {
        try {
            RedisTemplate<?, ?> redisTemplate = event.getApplicationContext().getBean("redisTemplate", RedisTemplate.class);

            String pong = redisTemplate.execute((RedisCallback<String>) RedisConnectionCommands::ping);
            if (!"PONG".equalsIgnoreCase(pong)) {
                log.error("Redis ping failed — server did not respond on channel '{}'", FLAG_EVENTS_CHANNEL);
                return;
            }
            log.info("Redis ping successful — server is reachable");

            RedisMessageListenerContainer container = event.getApplicationContext()
                    .getBean(RedisMessageListenerContainer.class);
            if (container.isRunning()) {
                log.info("Redis subscription verified — channel '{}' is active and ready to receive events", FLAG_EVENTS_CHANNEL);
            } else {
                log.warn("Redis reachable but listener container not running — channel '{}' may not be subscribed", FLAG_EVENTS_CHANNEL);
            }
        } catch (Exception e) {
            log.error("Redis connection failed — channel '{}' not available: {}", FLAG_EVENTS_CHANNEL, e.getMessage());
        }
    }
}
