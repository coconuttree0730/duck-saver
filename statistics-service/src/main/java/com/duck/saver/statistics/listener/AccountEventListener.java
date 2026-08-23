package com.duck.saver.statistics.listener;

import com.duck.saver.common.event.AccountEvent;
import com.duck.saver.common.event.MqTopology;
import com.duck.saver.statistics.service.AccountEventConsumer;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * statistics 事件监听：手动 ack；处理异常时拒绝并转入死信路径（重试策略在可靠性票中完善）。
 */
@Component
public class AccountEventListener {

	private static final Logger log = LoggerFactory.getLogger(AccountEventListener.class);

	@Autowired
	private AccountEventConsumer consumer;

	@RabbitListener(queues = MqTopology.STATISTICS_QUEUE, containerFactory = "statisticsListenerFactory")
	public void onAccountEvent(AccountEvent event, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
		try {
			consumer.consume(event);
			channel.basicAck(deliveryTag, false);
		} catch (Exception e) {
			log.error("event consumption failed, routing to DLQ: {} ({})", event == null ? "?" : event.getEventId(),
					e.getMessage(), e);
			channel.basicNack(deliveryTag, false, false);
		}
	}
}
