package com.clinic.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * <h2>AES 对称加密工具类</h2>
 *
 * <p>基于 AES/CBC/PKCS5Padding 算法实现字符串的加密与解密。
 * 采用固定的初始化向量（IV）进行加解密操作，密钥通过配置文件注入。</p>
 *
 * <h3>安全注意事项</h3>
 * <ul>
 *   <li>当前使用固定 IV，仅适用于开发/测试环境</li>
 *   <li>生产环境建议为每条记录生成独立随机 IV 并随密文一同存储</li>
 *   <li>密钥应通过安全的配置管理（如环境变量、密钥管理服务）注入</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   @Autowired
 *   private AesUtils aesUtils;
 *
 *   // 加密
 *   String encrypted = aesUtils.encrypt("敏感数据");
 *
 *   // 解密
 *   String decrypted = aesUtils.decrypt(encrypted);
 * }</pre>
 *
 * @author clinic
 * @version 1.0
 */
@Slf4j
@Component
public class AesUtils {

    /**
     * 加密算法标识：AES 加密，CBC 工作模式，PKCS5 填充方式
     */
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * AES 密钥，从配置文件中注入（aes.secret-key）
     */
    @Value("${aes.secret-key}")
    private String secretKey;

    /**
     * 固定的 16 字节初始化向量（IV）
     * <p>注意：生产环境应改为每条记录存储独立的随机 IV</p>
     */
    // 16字节固定IV，实际生产环境每条记录存独立IV
    private static final byte[] FIXED_IV = "ClinicSystem-IV-".getBytes(StandardCharsets.UTF_8);

    /**
     * 根据配置的密钥字符串生成 AES 密钥规范对象
     *
     * <p>将配置文件中的密钥字符串截取/填充为恰好 32 字节，
     * 并封装为 {@link SecretKeySpec} 对象，供加解密使用。</p>
     *
     * @return 256 位（32 字节）的 AES 密钥规范
     */
    private SecretKeySpec getKey() {
        // 创建固定 32 字节的缓冲区
        byte[] keyBytes = new byte[32];
        // 将配置的密钥字符串转换为字节数组
        byte[] srcBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        // 复制源字节到目标缓冲区（最多 32 字节，不足部分补 0）
        System.arraycopy(srcBytes, 0, keyBytes, 0, Math.min(srcBytes.length, 32));
        // 构建 AES 密钥规范对象
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密明文文本
     *
     * <p>使用 AES/CBC/PKCS5Padding 模式对输入的明文字符串进行加密，
     * 并将加密结果以 Base64 编码字符串形式返回，便于传输和存储。</p>
     *
     * @param plainText 待加密的明文内容，不能为空
     * @return Base64 编码后的密文字符串
     * @throws RuntimeException 当加密过程发生异常时抛出
     */
    public String encrypt(String plainText) {
        try {
            // 获取 Cipher 实例，指定 AES/CBC/PKCS5Padding 算法
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            // 初始化为加密模式，使用密钥和固定 IV
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new IvParameterSpec(FIXED_IV));
            // 执行加密操作
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            // 将加密后的字节数组以 Base64 编码返回
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            // 记录加密失败日志，抛出运行时异常
            log.error("AES加密失败", e);
            throw new RuntimeException("加密失败");
        }
    }

    /**
     * 解密密文文本
     *
     * <p>对 Base64 编码的密文字符串进行 AES/CBC/PKCS5Padding 模式解密，
     * 还原为原始明文字符串。</p>
     *
     * @param cipherText Base64 编码的密文字符串
     * @return 解密后的明文字符串
     * @throws RuntimeException 当解密过程发生异常时抛出（如密文格式错误、密钥不匹配等）
     */
    public String decrypt(String cipherText) {
        try {
            // 获取 Cipher 实例
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            // 初始化为解密模式，使用密钥和固定 IV
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new IvParameterSpec(FIXED_IV));
            // 先将 Base64 密文解码为字节数组
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            // 执行解密操作，并将结果转为 UTF-8 字符串
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 记录解密失败日志，抛出运行时异常
            log.error("AES解密失败", e);
            throw new RuntimeException("解密失败");
        }
    }
}
