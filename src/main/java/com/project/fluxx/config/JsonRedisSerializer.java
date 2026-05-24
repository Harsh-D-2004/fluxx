package com.project.fluxx.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.fasterxml.jackson.databind.ObjectMapper;

class JsonRedisSerializer<T> implements RedisSerializer<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> targetType;

    JsonRedisSerializer(ObjectMapper objectMapper, Class<T> targetType) {
        this.objectMapper = objectMapper;
        this.targetType = targetType;
    }

    @Override
    public byte[] serialize(T value) throws SerializationException {
        try {
            return value == null ? null : objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Redis serialization failed", e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        try {
            return bytes == null ? null : objectMapper.readValue(bytes, targetType);
        } catch (Exception e) {
            throw new SerializationException("Redis deserialization failed", e);
        }
    }
}
