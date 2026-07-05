package com.clinic.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类 —— 注册插件拦截器，提供分页等增强功能。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>注册 {@link MybatisPlusInterceptor} 核心拦截器</li>
 *   <li>添加分页插件（PaginationInnerInterceptor），支持物理分页查询</li>
 * </ul>
 *
 * <p>MyBatis-Plus 的分页插件工作原理：</p>
 * <ol>
 *   <li>拦截 SQL 执行前的查询语句</li>
 *   <li>根据数据库类型（此处为 MySQL）自动在 SQL 后追加 LIMIT 子句</li>
 *   <li>同时执行 COUNT 查询获取总记录数，封装到 Page 对象中</li>
 * </ol>
 *
 * @author clinic
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 核心拦截器，并添加分页插件。
     *
     * <p>使用分页功能时，只需在 Mapper 方法中传入 {@code Page<T>} 参数，
     * 插件会自动拦截并生成分页 SQL，无需手动编写 LIMIT。</p>
     *
     * @return 配置了分页插件的 MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建核心拦截器实例
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页内部拦截器，指定数据库类型为 MySQL
        // PaginationInnerInterceptor 会自动生成 MySQL 分页方言的 SQL（LIMIT offset, size）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
