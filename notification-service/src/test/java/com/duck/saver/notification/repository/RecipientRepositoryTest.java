package com.duck.saver.notification.repository;

import com.duck.saver.notification.domain.Frequency;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import com.duck.saver.notification.domain.NotificationSettings;
import com.duck.saver.notification.domain.NotificationType;
import com.duck.saver.notification.domain.Recipient;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@Testcontainers
class RecipientRepositoryTest {

	@Container
	@ServiceConnection
	static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

	@Autowired
	private RecipientRepository repository;

	@Test
	public void shouldFindByAccountName() {

		NotificationSettings remind = new NotificationSettings();
		remind.setActive(true);
		remind.setFrequency(Frequency.WEEKLY);
		remind.setLastNotified(new Date(0));

		NotificationSettings backup = new NotificationSettings();
		backup.setActive(false);
		backup.setFrequency(Frequency.MONTHLY);
		backup.setLastNotified(new Date());

		Recipient recipient = new Recipient();
		recipient.setAccountName("test");
		recipient.setEmail("test@test.com");
		recipient.setScheduledNotifications(Map.of(
				NotificationType.BACKUP, backup,
				NotificationType.REMIND, remind
		));

		repository.save(recipient);

		Recipient found = repository.findByAccountName(recipient.getAccountName());
		assertEquals(recipient.getAccountName(), found.getAccountName());
		assertEquals(recipient.getEmail(), found.getEmail());

		assertEquals(recipient.getScheduledNotifications().get(NotificationType.BACKUP).getActive(),
				found.getScheduledNotifications().get(NotificationType.BACKUP).getActive());
		assertEquals(recipient.getScheduledNotifications().get(NotificationType.BACKUP).getFrequency(),
				found.getScheduledNotifications().get(NotificationType.BACKUP).getFrequency());
		assertEquals(recipient.getScheduledNotifications().get(NotificationType.BACKUP).getLastNotified(),
				found.getScheduledNotifications().get(NotificationType.BACKUP).getLastNotified());

		assertEquals(recipient.getScheduledNotifications().get(NotificationType.REMIND).getActive(),
				found.getScheduledNotifications().get(NotificationType.REMIND).getActive());
		assertEquals(recipient.getScheduledNotifications().get(NotificationType.REMIND).getFrequency(),
				found.getScheduledNotifications().get(NotificationType.REMIND).getFrequency());
		assertEquals(recipient.getScheduledNotifications().get(NotificationType.REMIND).getLastNotified(),
				found.getScheduledNotifications().get(NotificationType.REMIND).getLastNotified());
	}

	@Test
	public void shouldFindReadyForRemindWhenFrequencyIsWeeklyAndLastNotifiedWas8DaysAgo() {

		NotificationSettings remind = new NotificationSettings();
		remind.setActive(true);
		remind.setFrequency(Frequency.WEEKLY);
		remind.setLastNotified(DateUtils.addDays(new Date(), -8));

		Recipient recipient = new Recipient();
		recipient.setAccountName("test");
		recipient.setEmail("test@test.com");
		recipient.setScheduledNotifications(Map.of(
				NotificationType.REMIND, remind
		));

		repository.save(recipient);

		List<Recipient> found = repository.findReadyForRemind();
		assertFalse(found.isEmpty());
	}

	@Test
	public void shouldNotFindReadyForRemindWhenFrequencyIsWeeklyAndLastNotifiedWasYesterday() {

		NotificationSettings remind = new NotificationSettings();
		remind.setActive(true);
		remind.setFrequency(Frequency.WEEKLY);
		remind.setLastNotified(DateUtils.addDays(new Date(), -1));

		Recipient recipient = new Recipient();
		recipient.setAccountName("test");
		recipient.setEmail("test@test.com");
		recipient.setScheduledNotifications(Map.of(
				NotificationType.REMIND, remind
		));

		repository.save(recipient);

		List<Recipient> found = repository.findReadyForRemind();
		assertTrue(found.isEmpty());
	}

	@Test
	public void shouldNotFindReadyForRemindWhenNotificationIsNotActive() {

		NotificationSettings remind = new NotificationSettings();
		remind.setActive(false);
		remind.setFrequency(Frequency.WEEKLY);
		remind.setLastNotified(DateUtils.addDays(new Date(), -30));

		Recipient recipient = new Recipient();
		recipient.setAccountName("test");
		recipient.setEmail("test@test.com");
		recipient.setScheduledNotifications(Map.of(
				NotificationType.REMIND, remind
		));

		repository.save(recipient);

		List<Recipient> found = repository.findReadyForRemind();
		assertTrue(found.isEmpty());
	}

	@Test
	public void shouldNotFindReadyForBackupWhenFrequencyIsQuaterly() {

		NotificationSettings remind = new NotificationSettings();
		remind.setActive(true);
		remind.setFrequency(Frequency.QUARTERLY);
		remind.setLastNotified(DateUtils.addDays(new Date(), -91));

		Recipient recipient = new Recipient();
		recipient.setAccountName("test");
		recipient.setEmail("test@test.com");
		recipient.setScheduledNotifications(Map.of(
				NotificationType.BACKUP, remind
		));

		repository.save(recipient);

		List<Recipient> found = repository.findReadyForBackup();
		assertFalse(found.isEmpty());
	}
}