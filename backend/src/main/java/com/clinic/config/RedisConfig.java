package com.clinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类 —— 自定义 RedisTemplate 的序列化方式。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>注册自定义的 {@link RedisTemplate} Bean</li>
 *   <li>配置 Key 使用 String 序列化器（便于在 Redis 客户端中查看）</li>
 *   <li>配置 Value 使用 JSON 序列化器（支持复杂对象存储，无需手动序列化）</li>
 * </ul>
 *
 * <p>如果不自定义 RedisTemplate，Spring Boot 默认使用 JDK 序列化，
 * 生成的 Key 和 Value 在 Redis 中会显示为乱码，不便于调试和运维。</p>
 *
 * @author clinic
 */
@Configuration
public class RedisConfig {

    /**
     * 创建并配置 RedisTemplate。
     *
     * <p>序列化策略：</p>
     * <ul>
     *   <li><b>Key 序列化</b>：{@link StringRedisSerializer} —— Key 以可读字符串存储</li>
     *   <li><b>Value 序列化</b>：{@link GenericJackson2JsonRedisSerializer} —— Value 以 JSON 格式存储</li>
     *   <li><b>HashKey 序列化</b>：{@link StringRedisSerializer} —— Hash 的 Key 以字符串存储</li>
     *   <li><b>HashValue 序列化</b>：{@link GenericJackson2JsonRedisSerializer} —— Hash 的 Value 以 JSON 存储</li>
     * </ul>
     *
     * @param factory Redis 连接工厂，由 Spring Boot 自动注入
     * @return 配置完成的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 设置 Redis 连接工厂
        template.setConnectionFactory(factory);

        // 设置 Key 的序列化方式为 String（使 Redis 中 Key 可读）
        template.setKeySerializer(new StringRedisSerializer());
        // 设置 Value 的序列化方式为 JSON（支持复杂对象的自动序列化）
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 设置 Hash 类型的 Key 序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        // 设置 Hash 类型的 Value 序列化方式
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 调用 afterPropertiesSet 完成初始化
        template.afterPropertiesSet();

        return template;
    }
}
