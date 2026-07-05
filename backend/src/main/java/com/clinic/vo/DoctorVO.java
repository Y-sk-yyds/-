package com.clinic.vo;

import com.clinic.entity.Doctor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>医生信息展示对象（VO）</h2>
 *
 * <p>继承自实体类 {@link Doctor}，在医生基础字段之上
 * 额外补充登录账号相关的冗余字段，用于医生管理页面的综合展示。</p>
 *
 * <p>通过本 VO，前端可以在显示医生基本信息的同时，
 * 一并获取其关联的系统账号状态，无需额外请求用户服务。</p>
 *
 * <h3>继承字段（来自 Doctor 实体）</h3>
 * <ul>
 *   <li>基础字段：id、name、department、title、phone、introduction 等</li>
 *   <li>BaseEntity 公共字段：createTime、updateTime、deleted</li>
 * </ul>
 *
 * <h3>扩展字段</h3>
 * <ul>
 *   <li><b>username</b> — 系统登录用户名</li>
 *   <li><b>userRole</b> — 用户角色（通常为 DOCTOR）</li>
 *   <li><b>userStatus</b> — 账号状态（启用/禁用）</li>
 * </ul>
 *
 * <h3>典型使用场景</h3>
 * <ul>
 *   <li>管理员查看医生列表（含账号状态）</li>
 *   <li>医生管理页面的详情展示</li>
 *   <li>系统账号启用/禁用的状态查询</li>
 * </ul>
 *
 * @author clinic
 * @version 1.0
 * @see Doctor 父类医生实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DoctorVO extends Doctor {

    /**
     * 系统登录用户名
     * <p>从用户表（t_user）关联查询获取，对应医生的系统登录账号。
     * 可能为医生工号或自定义名，用于登录和系统日志追踪。</p>
     */
    private String username;

    /**
     * 用户角色标识
     * <p>从用户表关联查询获取，通常医生的角色为 "DOCTOR"。
     * 此字段与角色权限控制相关，前端可根据角色展示不同的功能入口。</p>
     */
    private String userRole;

    /**
     * 账号状态
     *
     * <p>从用户表关联查询获取，取值说明：</p>
     * <ul>
     *   <li>0 — 禁用状态，医生无法登录系统</li>
     *   <li>1 — 启用状态，医生可正常登录和工作</li>
     * </ul>
     * <p>管理员可通过此字段判断是否需要启用/禁用医生账号。</p>
     */
    private Integer userStatus;
}
