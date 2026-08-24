package com.duck.saver.statistics.config;

import com.duck.saver.common.event.MqTopology;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * MQ 拓扑与监听容器（AGENTS.md 定稿）：手动 ack；消费异常经重试（默认 3 次指数退避）
 * 后拒绝进入死信队列。拓扑声明与 account-service 侧保持一致。
 */
@Configuration
public class StatisticsAmqpConfig {

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

	@Bean
	public SimpleRabbitListenerContainerFactory dlqListenerFactory(
			ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(converter);
		factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
		return factory;
	}

	@Bean
	public RetryOperationsInterceptor eventRetryInterceptor(
			@Value("${account.event.retry.max-attempts:3}") int maxAttempts,
			@Value("${account.event.retry.initial-interval-ms:1000}") long initialIntervalMs,
			@Value("${account.event.retry.multiplier:2.0}") double multiplier) {
		return RetryInterceptorBuilder.stateless()
				.maxAttempts(maxAttempts)
				.backOffOptions(initialIntervalMs, multiplier, 60_000)
				.recoverer(new RejectAndDontRequeueRecoverer())
				.build();
	}

	@Bean
	public SimpleRabbitListenerContainerFactory statisticsListenerFactory(
			ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter,
			RetryOperationsInterceptor eventRetryInterceptor) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(converter);
		factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
		factory.setAdviceChain(eventRetryInterceptor);
		return factory;
	}
}
