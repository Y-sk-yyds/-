package com.clinic.service;

import com.clinic.entity.Drug;
import com.clinic.entity.DrugStock;
import com.clinic.mapper.DrugMapper;
import com.clinic.mapper.DrugStockMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 药品库存 SETNX 分布式锁 集成测试
 * <p>
 * 测试策略：直接操作 Redis SETNX + MySQL 库存，绕过 Spring 代理，
 * 验证分布式锁在高并发下的正确性。
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DrugStockLockTest {

    @Autowired private DrugMapper drugMapper;
    @Autowired private DrugStockMapper drugStockMapper;
    @Autowired private StringRedisTemplate redis;

    private Long drugId;
    private Long stockId;

    @BeforeEach
    void setUp() {
        Drug drug = new Drug();
        drug.setName("测试药品-" + System.currentTimeMillis());
        drug.setSpecification("0.2g×24片");
        drug.setUnit("盒");
        drug.setUnitPrice(new BigDecimal("15.00"));
        drug.setCategory("解热镇痛");
        drug.setStatus(1);
        drugMapper.insert(drug);
        drugId = drug.getId();

        DrugStock stock = new DrugStock();
        stock.setDrugId(drugId);
        stock.setCurrentStock(5);
        stock.setWarningThreshold(3);
        drugStockMapper.insert(stock);
        stockId = stock.getId();
        log.info("准备药品 ID={}, 库存=5", drugId);
    }

    @Test
    @Order(1)
    @DisplayName("[Redis SETNX] 串行验证 — 锁+扣减+释放")
    void testSequential() {
        String lockKey = "drug:lock:" + drugId;
        int success = 0, failure = 0;

        for (int i = 0; i < 6; i++) {
            // ====== 以下代码与 PrescriptionService.deductStock 完全一致 ======
            Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(3));
            if (Boolean.TRUE.equals(locked)) {
                try {
                    DrugStock s = drugStockMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DrugStock>()
                                    .eq(DrugStock::getDrugId, drugId));
                    if (s != null && s.getCurrentStock() >= 1) {
                        drugStockMapper.update(null,
                                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DrugStock>()
                                        .setSql("current_stock = current_stock - 1")
                                        .eq(DrugStock::getDrugId, drugId)
                                        .ge(DrugStock::getCurrentStock, 1));
                        success++;
                    } else {
                        failure++;
                    }
                } finally {
                    redis.delete(lockKey);
                }
            } else {
                failure++;
            }
        }

        log.info("串行6次: 成功={}, 失败={}", success, failure);
        assertEquals(5, success);
        assertEquals(1, failure);
        assertEquals(0, drugStockMapper.selectById(stockId).getCurrentStock());
        assertNull(redis.opsForValue().get(lockKey));
        log.info("[PASS] 串行: 锁正常, 库存5→0");
    }

    @Test
    @Order(2)
    @DisplayName("[Redis SETNX] 并发30线程抢5库存 — 无超扣无丢失")
    void testConcurrent() throws Exception {
        String lockKey = "drug:lock:" + drugId;
        int threads = 30;
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startGate.await();
                    // 自旋重试：直到获取锁成功或库存耗尽
                    while (true) {
                        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(3));
                        if (Boolean.TRUE.equals(locked)) {
                            try {
                                DrugStock s = drugStockMapper.selectOne(
                                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DrugStock>()
                                                .eq(DrugStock::getDrugId, drugId));
                                if (s != null && s.getCurrentStock() >= 1) {
                                    drugStockMapper.update(null,
                                            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DrugStock>()
                                                    .setSql("current_stock = current_stock - 1")
                                                    .eq(DrugStock::getDrugId, drugId)
                                                    .ge(DrugStock::getCurrentStock, 1));
                                    success.incrementAndGet();
                                    return;
                                } else {
                                    failure.incrementAndGet();
                                    return;
                                }
                            } finally {
                                redis.delete(lockKey);
                            }
                        }
                        // 锁被占用，等 5ms 后重试
                        Thread.sleep(5);
                    }
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            }).start();
        }

        startGate.countDown(); // 同时释放所有线程
        doneGate.await();

        int succ = success.get();
        int fail = failure.get();
        log.info("并发{}线程: 成功={}, 失败={}", threads, succ, fail);

        assertEquals(5, succ, "库存5盒，应恰好成功5次");
        assertEquals(25, fail, "30线程减5次=25次失败");
        assertEquals(0, drugStockMapper.selectById(stockId).getCurrentStock(), "库存应为0");
        assertNull(redis.opsForValue().get(lockKey), "锁应已释放");

        log.info("[PASS] 并发30抢5: 成功5, 失败25, 无超扣无丢失");
    }
}
