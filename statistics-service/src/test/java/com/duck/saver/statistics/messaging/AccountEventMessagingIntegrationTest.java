package com.duck.saver.statistics.messaging;

import com.duck.saver.common.event.AccountEvent;
import com.duck.saver.common.event.AccountSnapshot;
import com.duck.saver.common.event.EventType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.statistics.entity.DataPointEntity;
import com.duck.saver.statistics.entity.ProcessedEventEntity;
import com.duck.saver.statistics.mapper.DataPointMapper;
import com.duck.saver.statistics.mapper.ProcessedEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.awaitility.Awaitility;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class AccountEventMessagingIntegrationTest {

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("duck_saver_statistics")
			.withInitScript("sql/statistics_schema.sql");

	@Container
	@ServiceConnection
	static org.testcontainers.containers.GenericContainer<?> redis =
			new org.testcontainers.containers.GenericContainer<>("redis:7").withExposedPorts(6379);

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private DataPointMapper dataPointMapper;

	@Autowired
	private ProcessedEventMapper processedEventMapper;

	private AccountEvent event(String eventId, String accountName, String expenseTitle, String amount) {
		AccountSnapshot snapshot = new AccountSnapshot();
		snapshot.setExpenses(List.of(new AccountSnapshot.Item(expenseTitle, new BigDecimal(amount))));
		snapshot.setIncomes(List.of());
		AccountSnapshot.Saving saving = new AccountSnapshot.Saving(new BigDecimal("5000"));
		snapshot.setSaving(saving);
		return new AccountEvent(eventId, EventType.ITEM_ADDED, System.currentTimeMillis(), accountName, snapshot);
	}

	@Test
	void shouldConsumeEventIntoDataPoint() {
		rabbitTemplate.convertAndSend(com.duck.saver.common.event.MqTopology.EXCHANGE, "",
				event("evt-consume-1", "mq-account", "午餐", "42"));

		Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			DataPointEntity point = dataPointMapper.selectOne(new LambdaQueryWrapper<DataPointEntity>()
					.eq(DataPointEntity::getAccountName, "mq-account"));
			assertThat(point).isNotNull();
			assertThat(point.getExpenses()).contains("午餐").contains("42");
			assertThat(point.getStatistics()).contains("EXPENSES_AMOUNT");
		});

		Long processed = processedEventMapper.selectCount(new LambdaQueryWrapper<ProcessedEventEntity>()
				.eq(ProcessedEventEntity::getEventId, "evt-consume-1"));
		assertThat(processed).isEqualTo(1);
	}

	@Test
	void shouldIgnoreDuplicateEvent() {
		rabbitTemplate.convertAndSend(com.duck.saver.common.event.MqTopology.EXCHANGE, "",
				event("evt-dup-1", "dup-account", "咖啡", "9"));
		rabbitTemplate.convertAndSend(com.duck.saver.common.event.MqTopology.EXCHANGE, "",
				event("evt-dup-1", "dup-account", "咖啡", "9"));

		Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			Long processed = processedEventMapper.selectCount(new LambdaQueryWrapper<ProcessedEventEntity>()
					.eq(ProcessedEventEntity::getEventId, "evt-dup-1"));
			assertThat(processed).isEqualTo(1);
		});

		List<DataPointEntity> points = dataPointMapper.selectList(new LambdaQueryWrapper<DataPointEntity>()
				.eq(DataPointEntity::getAccountName, "dup-account"));
		assertThat(points).hasSize(1);
	}
}
