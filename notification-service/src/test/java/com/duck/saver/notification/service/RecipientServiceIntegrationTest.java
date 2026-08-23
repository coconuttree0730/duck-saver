package com.duck.saver.notification.service;

import com.duck.saver.notification.dto.RecipientResponse;
import com.duck.saver.notification.dto.SaveRecipientRequest;
import com.duck.saver.notification.entity.NotificationConfigEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class RecipientServiceIntegrationTest {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("duck_saver_notification")
			.withInitScript("sql/notification_schema.sql");

	@Autowired
	private RecipientService recipientService;

	private SaveRecipientRequest request(String email, String frequency) {
		SaveRecipientRequest request = new SaveRecipientRequest();
		request.setEmail(email);
		request.setFrequency(frequency);
		request.setEnabled(true);
		return request;
	}

	@Test
	public void shouldSaveAndReadRecipientSettings() {

		recipientService.save("tester", request("tester@example.com", "MONTHLY"));

		RecipientResponse response = recipientService.findByAccountName("tester");

		assertEquals("tester", response.getRecipient().getName());
		assertEquals("tester@example.com", response.getRecipient().getEmail());
		assertEquals("MONTHLY", response.getRecipient().getFrequency());
		assertTrue(response.getRecipient().isEnabled());

		List<String> types = response.getNotificationConfig().stream()
				.map(com.duck.saver.notification.dto.NotificationConfigResponse::getType)
				.toList();
		assertEquals(2, types.size());
		assertTrue(types.contains(RecipientService.TYPE_BACKUP));
		assertTrue(types.contains(RecipientService.TYPE_BILL_REMINDER));
	}

	@Test
	public void shouldFindReadyToNotifyOnlyWhenStale() {

		SaveRecipientRequest first = request("fresh@example.com", "WEEKLY");
		first.setEnabled(true);
		recipientService.save("fresh", first);

		// 刚保存（lastNotified=now）→ 不应触发
		assertEquals(0, recipientService.findReadyToNotify(RecipientService.TYPE_BILL_REMINDER).size());
	}
}
