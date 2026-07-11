package com.clinic.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AES 加密工具单元测试
 * <p>验证病历加密/解密的正确性与安全性</p>
 */
@SpringBootTest
class AesUtilsTest {

    @Autowired private AesUtils aesUtils;

    @Test
    @DisplayName("加密后解密应还原原始明文")
    void testEncryptDecryptRoundTrip() {
        String plain = "患者高血压3级，建议低盐低脂饮食，定期监测血压";
        String encrypted = aesUtils.encrypt(plain);

        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted, "密文不应与明文相同");
        assertTrue(encrypted.length() > plain.length(), "Base64编码后长度应增加");

        String decrypted = aesUtils.decrypt(encrypted);
        assertEquals(plain, decrypted, "解密后应与原文完全一致");
    }

    @Test
    @DisplayName("相同明文加密结果一致（固定IV模式，开发环境特性）")
    void testEncryptProducesSameCiphertextForSameInput() {
        String plain = "测试病历内容";

        String encrypted1 = aesUtils.encrypt(plain);
        String encrypted2 = aesUtils.encrypt(plain);

        assertEquals(encrypted1, encrypted2,
                "固定IV模式下相同明文应产出相同密文（生产环境应改为随机IV）");
    }

    @Test
    @DisplayName("空字符串加密解密")
    void testEncryptDecryptEmptyString() {
        String encrypted = aesUtils.encrypt("");
        assertNotNull(encrypted);

        String decrypted = aesUtils.decrypt(encrypted);
        assertEquals("", decrypted);
    }

    @Test
    @DisplayName("长文本加密解密")
    void testLongTextEncryptDecrypt() {
        String longText = "主诉：反复头晕伴胸闷1月余。现病史：患者1月前无明显诱因出现头晕，"
                + "呈阵发性，每次持续约10-30分钟，伴胸闷、心悸，活动后加重。"
                + "既往有高血压病史5年，长期口服硝苯地平控释片30mg qd。"
                + "查体：BP 160/95mmHg，HR 88次/分，律齐。"
                + "诊断：1.高血压病2级 2.冠状动脉粥样硬化性心脏病待排。"
                + "处理：1.低盐低脂饮食 2.硝苯地平控释片30mg bid 3.建议心电图检查 4.2周后复诊";

        String encrypted = aesUtils.encrypt(longText);
        String decrypted = aesUtils.decrypt(encrypted);

        assertEquals(longText, decrypted, "长文本解密后应与原文完全一致");
    }

    @Test
    @DisplayName("篡改密文后解密应失败")
    void testTamperedCiphertextFails() {
        String plain = "重要诊断信息";
        String encrypted = aesUtils.encrypt(plain);

        // 篡改密文中间一个字符
        char[] chars = encrypted.toCharArray();
        chars[encrypted.length() / 2] = (char) (chars[encrypted.length() / 2] + 1);
        String tampered = new String(chars);

        assertThrows(RuntimeException.class, () -> aesUtils.decrypt(tampered),
                "篡改密文后解密应抛出异常");
    }
}
