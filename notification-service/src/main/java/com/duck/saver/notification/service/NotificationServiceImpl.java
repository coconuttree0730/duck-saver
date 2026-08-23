package com.duck.saver.notification.service;

import com.duck.saver.notification.client.AccountServiceClient;
import com.duck.saver.notification.domain.NotificationType;
import com.duck.saver.notification.entity.NotificationConfigEntity;
import com.duck.saver.notification.entity.RecipientEntity;
import com.duck.saver.notification.mapper.RecipientMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationServiceImpl implements NotificationService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private AccountServiceClient client;

	@Autowired
	private RecipientService recipientService;

	@Autowired
	private EmailService emailService;

	@Autowired
	private RecipientMapper recipientMapper;

	@Override
	@Scheduled(cron = "${backup.cron}")
	public void sendBackupNotifications() {

		final NotificationType type = NotificationType.BACKUP;

		List<NotificationConfigEntity> configs = recipientService.findReadyToNotify(RecipientService.TYPE_BACKUP);
		log.info("found {} configs for backup notification", configs.size());

		configs.forEach(config -> CompletableFuture.runAsync(() -> {
			try {
				RecipientEntity recipient = recipientMapper.selectById(config.getRecipientId());
				if (recipient == null) {
					return;
				}
				com.duck.saver.common.api.Result<String> accountBackup =
						client.getAccount(recipient.getAccountName());
				String attachment = com.duck.saver.common.api.ResultCode.SUCCESS.getCode() == accountBackup.getCode()
						? accountBackup.getData()
						: null;

				emailService.send(type, toDomainRecipient(recipient), attachment);
				recipientService.markNotified(config);
			} catch (Throwable t) {
				log.error("an error during backup notification for config {}", config.getId(), t);
			}
		}));
	}

	@Override
	@Scheduled(cron = "${remind.cron}")
	public void sendRemindNotifications() {

		final NotificationType type = NotificationType.BILL_REMINDER;

		List<NotificationConfigEntity> configs = recipientService.findReadyToNotify(RecipientService.TYPE_BILL_REMINDER);
		log.info("found {} configs for remind notification", configs.size());

		configs.forEach(config -> CompletableFuture.runAsync(() -> {
			try {
				RecipientEntity recipient = recipientMapper.selectById(config.getRecipientId());
				if (recipient == null) {
					return;
				}
				emailService.send(type, toDomainRecipient(recipient), null);
				recipientService.markNotified(config);
			} catch (Throwable t) {
				log.error("an error during remind notification for config {}", config.getId(), t);
			}
		}));
	}

	private com.duck.saver.notification.domain.Recipient toDomainRecipient(RecipientEntity entity) {
		com.duck.saver.notification.domain.Recipient recipient = new com.duck.saver.notification.domain.Recipient();
		recipient.setAccountName(entity.getAccountName());
		recipient.setEmail(entity.getEmail());
		return recipient;
	}
}
