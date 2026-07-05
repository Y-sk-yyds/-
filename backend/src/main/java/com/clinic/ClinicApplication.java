package com.clinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 诊所管理系统 —— Spring Boot 应用程序主入口。
 *
 * <p>
 * 本类是整个诊所管理系统的启动类，基于 Spring Boot 框架构建。
 * 系统涵盖患者管理、医生管理、排班管理、挂号、就诊、处方、药品库存、
 * 账单收费、病历管理等完整的诊所业务流程。
 * </p>
 *
 * <h3>核心注解说明</h3>
 * <ul>
 *   <li>{@link SpringBootApplication @SpringBootApplication} —
 *     复合注解，等价于 {@code @Configuration + @EnableAutoConfiguration + @ComponentScan}，
 *     启用 Spring Boot 自动配置和组件扫描</li>
 *   <li>{@link EnableScheduling @EnableScheduling} —
 *     启用 Spring 的定时任务调度功能，用于执行后台周期性任务
 *     （例如：过期排班清理、库存预警检查等）</li>
 * </ul>
 *
 * <h3>启动方式</h3>
 * <pre>{@code
 * // 方式一：IDE 中直接运行 main 方法
 * // 方式二：命令行执行打包后的 JAR
 * java -jar clinic-backend.jar
 * }</pre>
 *
 * <h3>技术栈</h3>
 * <ul>
 *   <li>Spring Boot 3.x</li>
 *   <li>Spring Security（认证与授权）</li>
 *   <li>MyBatis-Plus（ORM 数据访问层）</li>
 *   <li>JWT（无状态身份令牌）</li>
 *   <li>Lombok（简化 Java 代码）</li>
 * </ul>
 *
 * @see SpringApplication Spring Boot 应用启动器
 */
@SpringBootApplication
@EnableScheduling
public class ClinicApplication {

    /**
     * 应用程序主入口方法。
     *
     * <p>
     * 调用 {@link SpringApplication#run(Class, String...)} 启动
     * 内嵌的 Tomcat 服务器，初始化 Spring 容器，加载所有 Bean 和配置，
     * 最终对外暴露 RESTful API 服务。
     * </p>
     *
     * @param args 命令行参数，可传递 Spring Boot 配置参数（如 --server.port=8080）
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用，扫描并初始化所有组件
        SpringApplication.run(ClinicApplication.class, args);
    }
}
