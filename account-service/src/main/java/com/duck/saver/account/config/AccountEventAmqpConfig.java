package com.duck.saver.account.config;

import com.duck.saver.common.event.MqTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ 拓扑与消息转换（AGENTS.md 定稿）：fanout exchange → 两个队列 + 死信。
 * RabbitMQ 声明幂等，与 statistics-service 侧声明保持一致。
 */
@Configuration
public class AccountEventAmqpConfig {

	@Bean
	public Declarables accountEventTopology() {
		FanoutExchange exchange = new FanoutExchange(MqTopology.EXCHANGE, true, false);
		FanoutExchange dlx = new FanoutExchange(MqTopology.DLX_EXCHANGE, true, false);

		Queue accountQueue = QueueBuilder.durable(MqTopology.ACCOUNT_QUEUE)
				.deadLetterExchange(MqTopology.DLX_EXCHANGE)
				.build();
		Queue statisticsQueue = QueueBuilder.durable(MqTopology.STATISTICS_QUEUE)
				.deadLetterExchange(MqTopology.DLX_EXCHANGE)
				.build();
		Queue dlq = QueueBuilder.durable(MqTopology.DLQ).build();

		return new Declarables(
				exchange,
				dlx,
				accountQueue,
				statisticsQueue,
				dlq,
				BindingBuilder.bind(accountQueue).to(exchange),
				BindingBuilder.bind(statisticsQueue).to(exchange),
				BindingBuilder.bind(dlq).to(dlx));
	}

	@Bean
	public Jackson2JsonMessageConverter eventMessageConverter() {
		return new Jackson2JsonMessageConverter("com.duck.saver.common.event");
	}
}
