package com.duck.saver.statistics.listener;

import com.duck.saver.common.event.AccountEvent;
import com.duck.saver.common.event.MqTopology;
import com.duck.saver.statistics.entity.ProcessedEventEntity;
import com.duck.saver.statistics.mapper.ProcessedEventMapper;
import com.duck.saver.statistics.service.AccountEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 事件监听与死信留痕：AUTO ack（成功即确认；重试拦截器在重试耗尽后抛出
 * AmqpRejectAndDontRequeueException，容器拒绝且不重回队列，经死信交换机进入 DLQ）。
 * 死信消费者仅记 ERROR 日志（告警标记）并落留痕记录，不自动重放——兜底对账另行处理。
 */
@Component
public class AccountEventListener {

	private static final Logger log = LoggerFactory.getLogger(AccountEventListener.class);
	private static final String ALERT_MARKER = "ACCOUNT_EVENT_DEAD_LETTER";

	@Autowired
	private AccountEventConsumer consumer;

	@Autowired
	private ProcessedEventMapper processedEventMapper;

	@RabbitListener(queues = MqTopology.STATISTICS_QUEUE, containerFactory = "statisticsListenerFactory")
	public void onAccountEvent(AccountEvent event) {
		consumer.consume(event);
	}

	@RabbitListener(queues = MqTopology.DLQ, containerFactory = "dlqListenerFactory")
	public void onDeadLetter(AccountEvent event) {
		log.error("[{}] event dead-lettered after retries: {} ({}) account={}", ALERT_MARKER,
				event == null ? "?" : event.getEventId(),
				event == null ? "?" : event.getEventType(),
				event == null ? "?" : event.getAccountName());
		if (event != null && event.getEventId() != null) {
			try {
				ProcessedEventEntity tombstone = new ProcessedEventEntity();
				tombstone.setEventId(event.getEventId());
				tombstone.setEventType(
						(event.getEventType() == null ? "UNKNOWN" : event.getEventType().name()) + "_DEAD");
				tombstone.setConsumedAt(LocalDateTime.now());
				processedEventMapper.insert(tombstone);
			} catch (Exception e) {
				log.warn("failed to persist dead-letter tombstone: {}", e.getMessage());
			}
		}
	}
}
