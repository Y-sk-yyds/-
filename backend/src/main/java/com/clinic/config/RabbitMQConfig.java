package com.clinic.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置类 —— 定义诊所提醒消息的队列、交换器和绑定关系。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>声明提醒消息队列（持久化）</li>
 *   <li>声明直接（Direct）类型交换器</li>
 *   <li>建立队列与交换器的绑定关系（通过路由键）</li>
 *   <li>配置消息转换器为 JSON 格式</li>
 * </ul>
 *
 * <p>消息路由模型：</p>
 * <pre>
 *   生产者 → 交换器（Direct）→ [路由键匹配] → 队列 → 消费者
 * </pre>
 *
 * @author clinic
 */
@Configuration
public class RabbitMQConfig {

    /** 提醒消息队列名称 */
    public static final String REMINDER_QUEUE = "clinic.reminder.queue";

    /** 提醒消息交换器名称 */
    public static final String REMINDER_EXCHANGE = "clinic.reminder.exchange";

    /** 提醒消息路由键 —— 交换器通过此键将消息路由到匹配的队列 */
    public static final String REMINDER_ROUTING_KEY = "clinic.reminder";

    /**
     * 声明提醒消息队列。
     *
     * <p>队列特性：</p>
     * <ul>
     *   <li><b>持久化（Durable）</b>：队列在 RabbitMQ 重启后不会丢失</li>
     *   <li><b>非排他</b>：可被多个连接共享</li>
     *   <li><b>非自动删除</b>：即使没有消费者也不会自动删除</li>
     * </ul>
     *
     * @return 配置好的 Queue 对象
     */
    @Bean
    public Queue reminderQueue() {
        // 使用 QueueBuilder 创建持久化队列
        return QueueBuilder.durable(REMINDER_QUEUE).build();
    }

    /**
     * 声明提醒消息交换器。
     *
     * <p>交换器类型为 <b>Direct</b>（直连），消息通过完全匹配的路由键从交换器路由到队列。</p>
     *
     * @return DirectExchange 实例
     */
    @Bean
    public DirectExchange reminderExchange() {
        return new DirectExchange(REMINDER_EXCHANGE);
    }

    /**
     * 声明队列与交换器的绑定关系。
     *
     * <p>绑定规则：将提醒队列绑定到提醒交换器，使用指定的路由键进行匹配。</p>
     *
     * @return Binding 对象，表示队列与交换器的绑定关系
     */
    @Bean
    public Binding reminderBinding() {
        // 将队列绑定到交换器，使用 REMINDER_ROUTING_KEY 作为路由键
        return BindingBuilder.bind(reminderQueue()).to(reminderExchange()).with(REMINDER_ROUTING_KEY);
    }

    /**
     * 配置消息转换器 —— 使用 Jackson2Json 将 Java 对象与 JSON 格式互相转换。
     *
     * <p>使用 JSON 消息转换器的优势：</p>
     * <ul>
     *   <li>发送消息时自动将 Java 对象序列化为 JSON 字符串</li>
     *   <li>接收消息时自动将 JSON 字符串反序列化为 Java 对象</li>
     *   <li>消息格式通用，便于跨语言消费和运维调试</li>
     * </ul>
     *
     * @return Jackson2JsonMessageConverter 实例
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
