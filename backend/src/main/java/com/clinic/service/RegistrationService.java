package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.Registration;
import com.clinic.entity.Schedule;
import com.clinic.mapper.RegistrationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 挂号服务类
 * <p>
 * 负责处理患者挂号的核心业务流程，包括：
 * <ul>
 *   <li>分页查询挂号记录（支持按患者、状态、医生筛选）</li>
 *   <li>创建挂号（包含 Redis 原子扣减号源、排队号计算）</li>
 *   <li>取消挂号（包含号源回退）</li>
 *   <li>按排班ID查询挂号列表</li>
 * </ul>
 * </p>
 * <p>
 * <b>号源扣减策略：</b>使用 Redis Lua 脚本实现原子操作，保证高并发场景下号源不超卖。
 * 同时同步更新 MySQL 中的号源余量，确保 Redis 异常时数据可恢复。
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class RegistrationService extends ServiceImpl<RegistrationMapper, Registration> {

    /** 操作挂号表的 Mapper */
    private final RegistrationMapper registrationMapper;

    /** 排班服务，用于查询和更新排班号源 */
    private final ScheduleService scheduleService;

    /** Redis 模板，用于号源缓存和原子扣减操作 */
    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis 中号源库存数据的 Key 前缀，实际 Key 格式为 schedule:stock:{scheduleId} */
    private static final String STOCK_KEY_PREFIX = "schedule:stock:";

    /** Redis 中号源分布式锁的 Key 前缀（备用） */
    private static final String LOCK_KEY_PREFIX = "schedule:lock:";

    /**
     * Lua 脚本：原子扣减号源库存
     * <p>
     * 脚本逻辑：
     * <ul>
     *   <li>GET 获取当前库存值</li>
     *   <li>库存不存在 → 返回 -2，表示缓存异常</li>
     *   <li>库存 ≤ 0 → 返回 -1，表示号源已满</li>
     *   <li>库存 > 0 → DECR 减 1，返回扣减后的剩余数量</li>
     * </ul>
     * 使用 Lua 脚本确保 GET + DECR 两步操作在 Redis 内原子执行，避免并发竞争。
     * </p>
     */
    private static final String LUA_DECR_STOCK =
            "local stock = redis.call('GET', KEYS[1]);" +
            "if not stock then return -2 end;" +
            "stock = tonumber(stock);" +
            "if stock <= 0 then return -1 end;" +
            "redis.call('DECR', KEYS[1]);" +
            "return stock - 1;";

    /**
     * 分页查询挂号记录
     * <p>
     * 委托 Mapper 层的自定义 SQL（关联查询排班、医生、患者信息），
     * 支持按患者ID、挂号状态、排班医生ID进行筛选。
     * </p>
     *
     * @param page             页码
     * @param pageSize         每页大小
     * @param patientId        患者ID（可选，用于筛选特定患者的挂号）
     * @param status           挂号状态（可选，1=已挂号, 0=已取消, 2=已就诊）
     * @param scheduleDoctorId 排班医生ID（可选，用于筛选某医生的挂号）
     * @return 分页的挂号视图对象列表，包含关联的排班、医生、患者信息
     */
    public IPage<com.clinic.vo.RegistrationVO> pageQuery(Integer page, Integer pageSize, Long patientId, Integer status, Long scheduleDoctorId) {
        return registrationMapper.selectRegistrationPage(new Page<>(page, pageSize), patientId, status, scheduleDoctorId);
    }

    /**
     * 创建挂号记录（患者挂号）
     * <p>
     * 业务执行流程：
     * <ol>
     *   <li>校验排班是否存在且状态正常（status=1 表示可挂号）</li>
     *   <li>检查当前患者是否已挂过同一排班号源，防止重复挂号</li>
     *   <li>通过 Redis Lua 脚本原子扣减号源库存，避免超卖</li>
     *   <li>同步更新 MySQL 中的排班剩余号源数</li>
     *   <li>计算排队号（当前排班下已挂号人数 + 1）</li>
     *   <li>保存挂号记录到数据库</li>
     * </ol>
     * </p>
     *
     * @param patientId  患者ID
     * @param scheduleId 排班ID
     * @return 创建的挂号记录
     * @throws IllegalArgumentException 排班不存在/已停诊、重复挂号、号源已满时抛出
     */
    @Transactional
    public Registration createRegistration(Long patientId, Long scheduleId) {
        // ===== 第1步：校验排班信息 =====
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null || schedule.getStatus() != 1) {
            throw new IllegalArgumentException("排班不存在或已停诊");
        }

        // ===== 第2步：防止重复挂号 =====
        // 查询该患者在该排班下是否存在状态为"已挂号"的记录
        long count = count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getPatientId, patientId)
                .eq(Registration::getScheduleId, scheduleId)
                .eq(Registration::getStatus, 1));
        if (count > 0) {
            throw new IllegalArgumentException("您已挂过该号源");
        }

        // ===== 第3步：Redis 原子扣减号源 =====
        // 构建 Redis 库存 Key：schedule:stock:{scheduleId}
        String stockKey = STOCK_KEY_PREFIX + scheduleId;

        // 如果 Redis 中还没有该排班的号源数据（如首次挂号或缓存过期），
        // 从 MySQL 中初始化到 Redis，SETNX 保证只初始化一次
        redisTemplate.opsForValue().setIfAbsent(stockKey, schedule.getRemainNum());

        // 构建并执行 Lua 脚本来原子扣减库存
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_DECR_STOCK);
        script.setResultType(Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(stockKey));

        // 解析 Lua 脚本返回值
        if (result == null || result == -1) {
            // 返回 -1：号源已满
            throw new IllegalArgumentException("号源已满");
        }
        if (result == -2) {
            // 返回 -2：Redis 缓存中不存在该 Key（异常情况）
            throw new IllegalArgumentException("号源缓存异常");
        }

        // ===== 第4步：同步更新 MySQL 号源数据 =====
        // 使用乐观条件更新，gt("remain_num", 0) 防止库存减为负数
        scheduleService.update()
                .setSql("remain_num = remain_num - 1")
                .eq("id", scheduleId)
                .gt("remain_num", 0)
                .update();

        // ===== 第5步：计算排队号 =====
        // 排队号 = 当前排班下所有非取消状态的挂号数 + 1
        Long queueCount = count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getScheduleId, scheduleId)
                .ne(Registration::getStatus, 0));

        // ===== 第6步：保存挂号记录 =====
        Registration registration = new Registration();
        registration.setPatientId(patientId);
        registration.setScheduleId(scheduleId);
        registration.setQueueNum((int) (queueCount + 1));
        registration.setStatus(1);                      // 状态：1 = 已挂号
        registration.setRegistrationDate(LocalDate.now()); // 记录挂号日期
        save(registration);

        return registration;
    }

    /**
     * 取消挂号
     * <p>
     * 业务执行流程：
     * <ol>
     *   <li>校验挂号记录是否存在且状态为"已挂号"</li>
     *   <li>将挂号状态更新为 0（已取消）</li>
     *   <li>回退 Redis 中的号源库存（INCR 加1）</li>
     *   <li>回退 MySQL 中的号源余量</li>
     * </ol>
     * </p>
     *
     * @param id 挂号记录ID
     * @throws IllegalArgumentException 挂号不存在或已取消时抛出
     */
    @Transactional
    public void cancelRegistration(Long id) {
        // 校验挂号记录状态
        Registration registration = getById(id);
        if (registration == null || registration.getStatus() != 1) {
            throw new IllegalArgumentException("挂号不存在或已取消");
        }

        // 更新挂号状态为"已取消"
        registration.setStatus(0);
        updateById(registration);

        // 回退 Redis 号源库存（原子自增）
        String stockKey = STOCK_KEY_PREFIX + registration.getScheduleId();
        redisTemplate.opsForValue().increment(stockKey);

        // 回退 MySQL 号源余量
        scheduleService.update()
                .setSql("remain_num = remain_num + 1")
                .eq("id", registration.getScheduleId())
                .update();
    }

    /**
     * 根据排班ID查询挂号列表
     * <p>查询指定排班下所有非取消状态的挂号记录，用于排队叫号等场景。</p>
     *
     * @param scheduleId 排班ID
     * @return 挂号记录列表（排除已取消的记录）
     */
    public List<Registration> getByScheduleId(Long scheduleId) {
        return list(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getScheduleId, scheduleId)
                .ne(Registration::getStatus, 0));
    }
}
