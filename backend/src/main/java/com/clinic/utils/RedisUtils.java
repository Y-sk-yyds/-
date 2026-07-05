package com.clinic.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * <h2>Redis 缓存操作工具类</h2>
 *
 * <p>封装了 Spring Data Redis 的 {@link RedisTemplate} 常用操作方法，
 * 提供简化的键值对存取、过期设置、计数器增减等功能。</p>
 *
 * <p>所有方法均操作字符串键（String key）与通用对象值（Object value），
 * 通过 {@code RedisTemplate<String, Object>} 实现序列化/反序列化。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   @Autowired
 *   private RedisUtils redisUtils;
 *
 *   // 存储缓存
 *   redisUtils.set("user:1001", userObj, 30, TimeUnit.MINUTES);
 *
 *   // 读取缓存
 *   User user = redisUtils.get("user:1001");
 *
 *   // 判断键是否存在
 *   boolean exists = redisUtils.hasKey("user:1001");
 *
 *   // 删除缓存
 *   redisUtils.delete("user:1001");
 * }</pre>
 *
 * @author clinic
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    /**
     * Redis 操作模板，由 Spring 容器自动注入
     * <p>键类型为 String，值类型为 Object（依赖序列化器处理实际类型）</p>
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 存储键值对（永不过期）
     *
     * @param key   缓存键
     * @param value 缓存值（任意对象，需可序列化）
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 存储带过期时间的键值对
     *
     * @param key     缓存键
     * @param value   缓存值（任意对象，需可序列化）
     * @param timeout 过期时长
     * @param unit    时间单位（如 {@link TimeUnit#MINUTES}）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存值
     *
     * @param key  缓存键
     * @param <T>  期望的返回值类型
     * @return 键对应的值，若键不存在则返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存键
     *
     * @param key 要删除的缓存键
     * @return 删除成功返回 {@code true}，键不存在返回 {@code false}
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 判断缓存键是否存在
     *
     * @param key 要检查的缓存键
     * @return 键存在返回 {@code true}，不存在返回 {@code false}
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 对指定键的整数值执行自增操作（+1）
     *
     * <p>常用于计数器、限流、序列号生成等场景。
     * 若键不存在，Redis 会先将其置为 0 再执行自增。</p>
     *
     * @param key 要自增的缓存键
     * @return 自增后的值，若操作失败返回 0
     */
    public long increment(String key) {
        Long result = redisTemplate.opsForValue().increment(key);
        return result != null ? result : 0;
    }

    /**
     * 对指定键的整数值执行自减操作（-1）
     *
     * <p>常用于库存扣减、配额消耗等场景。
     * 若键不存在，Redis 会先将其置为 0 再执行自减。</p>
     *
     * @param key 要自减的缓存键
     * @return 自减后的值，若操作失败返回 0
     */
    public long decrement(String key) {
        Long result = redisTemplate.opsForValue().decrement(key);
        return result != null ? result : 0;
    }

    /**
     * 设置缓存键的过期时间
     *
     * <p>只在键已存在时设置过期时间，键不存在时返回 {@code false}。</p>
     *
     * @param key     目标缓存键
     * @param timeout 过期时长
     * @param unit    时间单位（如 {@link TimeUnit#SECONDS}）
     * @return 设置成功返回 {@code true}，键不存在或操作失败返回 {@code false}
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }
}
