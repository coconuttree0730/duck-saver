package com.duck.saver.notification.service;

import com.duck.saver.notification.domain.Frequency;
import com.duck.saver.notification.domain.NotificationSettings;
import com.duck.saver.notification.domain.NotificationType;
import com.duck.saver.notification.domain.Recipient;
import com.duck.saver.notification.repository.RecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipientServiceImplTest {

	@InjectMocks
	private RecipientServiceImpl recipientService;

	@Mock
	private RecipientRepository repository;

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	@Test
	public void shouldFindByAccountName() {
		Recipient recipient = new Recipient();
		recipient.setAccountName("test");

		when(repository.findByAccountName(recipient.getAccountName())).thenReturn(recipient);
		Recipient found = recipientService.findByAccountName(recipient.getAccountName());

		assertEquals(recipient, found);
	}

	@Test
	public void shouldFailToFindRecipientWhenAccountNameIsEmpty() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> recipientService.findByAccountName(""));
	}

	@Test
	public void shouldSaveRecipient() {

		NotificationSettings remind = new NotificationSettings();
		remind.setActive(true);
		remind.setFrequency(Frequency.WEEKLY);
		remind.setLastNotified(null);

		NotificationSettings backup = new NotificationSettings();
		backup.setActive(false);
		backup.setFrequency(Frequency.MONTHLY);
		backup.setLastNotified(new Date());

		Recipient recipient = new Recipient();
		recipient.setEmail("test@test.com");
		recipient.setScheduledNotifications(Map.of(
				NotificationType.BACKUP, backup,
				NotificationType.REMIND, remind
		));

		Recipient saved = recipientService.save("test", recipient);

		verify(repository).save(recipient);
		assertNotNull(saved.getScheduledNotifications().get(NotificationType.REMIND).getLastNotified());
		assertEquals("test", saved.getAccountName());
	}

	@Test
	public void shouldFindReadyToNotifyWhenNotificationTypeIsBackup() {
		final List<Recipient> recipients = List.of(new Recipient());
		when(repository.findReadyForBackup()).thenReturn(recipients);

		List<Recipient> found = recipientService.findReadyToNotify(NotificationType.BACKUP);
		assertEquals(recipients, found);
	}

	@Test
	public void shouldFindReadyToNotifyWhenNotificationTypeIsRemind() {
		final List<Recipient> recipients = List.of(new Recipient());
		when(repository.findReadyForRemind()).thenReturn(recipients);

		List<Recipient> found = recipientService.findReadyToNotify(NotificationType.REMIND);
		assertEquals(recipients, found);
	}

	@Test
	public void shouldMarkAsNotified() {

		NotificationSettings remind = new NotificationSettings();
		remind.setActive(true);
		remind.setFrequency(Frequency.WEEKLY);
		remind.setLastNotified(null);

		Recipient recipient = new Recipient();
		recipient.setAccountName("test");
		recipient.setEmail("test@test.com");
		recipient.setScheduledNotifications(Map.of(
				NotificationType.REMIND, remind
		));

		recipientService.markNotified(NotificationType.REMIND, recipient);
		assertNotNull(recipient.getScheduledNotifications().get(NotificationType.REMIND).getLastNotified());
		verify(repository).save(recipient);
	}
}