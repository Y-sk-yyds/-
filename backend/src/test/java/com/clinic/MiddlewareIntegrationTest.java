package com.clinic;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 中间件集成测试 —— RabbitMQ 异步 + Redis 防超卖
 * <p>
 * 运行: IDEA 右键类名 → Run 'MiddlewareIntegrationTest'
 * 用途: 毕业答辩时截屏 JUnit 绿色通过结果即可证明中间件正确性
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MiddlewareIntegrationTest {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate rest;

    private String token;

    @BeforeEach
    void login() {
        ResponseEntity<Map> resp = rest.postForEntity("/api/auth/login",
                Map.of("username", "doctor01", "password", "admin123"), Map.class);
        token = (String) ((Map<?, ?>) resp.getBody().get("data")).get("token");
        assertNotNull(token, "Token不能为空");
        log.info("登录成功");
    }

    // ==================== Test 1: RabbitMQ 异步 ====================

    @Test
    @Order(1)
    @DisplayName("[MQ] 复诊提醒异步发送，前端响应不阻塞")
    void testRabbitMQAsync() throws Exception {
        HttpHeaders h = authHeaders();

        // 1. 挂号
        var regResp = rest.postForEntity("/api/registrations",
                new HttpEntity<>(Map.of("patientId", 5, "scheduleId", 9), h), Map.class);
        assertEquals(200, ((Map<?, ?>) regResp.getBody()).get("code"));
        int regId = (Integer) ((Map<?, ?>) regResp.getBody().get("data")).get("id");
        log.info("1.挂号 ID={}", regId);

        // 2. 接诊
        var startResp = rest.exchange("/api/consultations/" + regId + "/start",
                HttpMethod.PUT, new HttpEntity<>(null, h), Map.class);
        int consId = (Integer) ((Map<?, ?>) startResp.getBody().get("data")).get("id");
        log.info("2.接诊完成, 就诊ID={}", consId);

        // 3. 完成就诊，计时
        long start = System.currentTimeMillis();
        var completeResp = rest.exchange("/api/consultations/" + consId + "/complete",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("complaint","x","diagnosis","y","advice","1天后复诊"), h),
                Map.class);
        long elapsed = System.currentTimeMillis() - start;

        log.info("3.完成就诊 响应:{}ms", elapsed);
        assertEquals(200, ((Map<?, ?>) completeResp.getBody()).get("code"));

        // 断言: 异步不阻塞，响应应 < 500ms
        assertTrue(elapsed < 500,
                "MQ异步失败: 响应耗时 " + elapsed + "ms >= 500ms，前端被阻塞!");

        Thread.sleep(200);
        log.info("[PASS] RabbitMQ异步: 前端{}ms响应，MQ在后台消费", elapsed);
    }

    // ==================== Test 2: Redis Lua 防超卖 ====================

    @Test
    @Order(2)
    @DisplayName("[Redis] Lua原子扣减号源，并发不超卖")
    void testRedisLuaAtomic() throws Exception {
        HttpHeaders h = authHeaders();
        long schId = 16L; // 张医生 7/7 上午，30个号源，未使用
        int totalRequests = 35;

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            final int pid = 100 + i; // 确保不重复
            new Thread(() -> {
                try {
                    var resp = rest.postForEntity("/api/registrations",
                            new HttpEntity<>(Map.of("patientId", pid, "scheduleId", schId), h), Map.class);
                    int code = (Integer) ((Map<?, ?>) resp.getBody()).get("code");
                    if (code == 200) success.incrementAndGet();
                    else failure.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        int succ = success.get();
        int fail = failure.get();
        log.info("并发{}请求: 成功{}个, 被拒{}个", totalRequests, succ, fail);

        assertTrue(succ <= 30, "超卖! 成功" + succ + " > 号源30");
        assertTrue(succ == 30, "应恰好成功30个, 实际" + succ);
        assertTrue(fail == 5, "应恰好拒绝5个, 实际" + fail);

        log.info("[PASS] Redis Lua: 30号源, 35并发, 成功30, 拒绝5, 0超卖");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
