package com.duck.saver.notification.service;

import java.util.List;
import java.util.Map;
import com.duck.saver.common.api.ApiResponse;
import com.duck.saver.notification.client.AccountServiceClient;
import com.duck.saver.notification.domain.NotificationType;
import com.duck.saver.notification.domain.Recipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import jakarta.mail.MessagingException;
import java.io.IOException;

import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

	@InjectMocks
	private NotificationServiceImpl notificationService;

	@Mock
	private RecipientService recipientService;

	@Mock
	private AccountServiceClient client;

	@Mock
	private EmailService emailService;

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	@Test
	public void shouldSendBackupNotificationsEvenWhenErrorsOccursForSomeRecipients() throws IOException, MessagingException, InterruptedException {

		final String attachment = "json";

		Recipient withError = new Recipient();
		withError.setAccountName("with-error");

		Recipient withNoError = new Recipient();
		withNoError.setAccountName("with-no-error");

		when(client.getAccount(withError.getAccountName())).thenThrow(new RuntimeException());
		when(client.getAccount(withNoError.getAccountName())).thenReturn(ApiResponse.ok(attachment));

		when(recipientService.findReadyToNotify(NotificationType.BACKUP)).thenReturn(List.of(withNoError, withError));

		notificationService.sendBackupNotifications();

		// TODO test concurrent code in a right way

		verify(emailService, timeout(100)).send(NotificationType.BACKUP, withNoError, attachment);
		verify(recipientService, timeout(100)).markNotified(NotificationType.BACKUP, withNoError);

		verify(recipientService, never()).markNotified(NotificationType.BACKUP, withError);
	}

	@Test
	public void shouldSendRemindNotificationsEvenWhenErrorsOccursForSomeRecipients() throws IOException, MessagingException, InterruptedException {

		final String attachment = "json";

		Recipient withError = new Recipient();
		withError.setAccountName("with-error");

		Recipient withNoError = new Recipient();
		withNoError.setAccountName("with-no-error");

		when(recipientService.findReadyToNotify(NotificationType.REMIND)).thenReturn(List.of(withNoError, withError));
		doThrow(new RuntimeException()).when(emailService).send(NotificationType.REMIND, withError, null);

		notificationService.sendRemindNotifications();

		// TODO test concurrent code in a right way

		verify(emailService, timeout(100)).send(NotificationType.REMIND, withNoError, null);
		verify(recipientService, timeout(100)).markNotified(NotificationType.REMIND, withNoError);

		verify(recipientService, never()).markNotified(NotificationType.REMIND, withError);
	}
}