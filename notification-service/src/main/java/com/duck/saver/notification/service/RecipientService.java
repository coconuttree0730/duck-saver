package com.duck.saver.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.notification.domain.Frequency;
import com.duck.saver.notification.domain.NotificationType;
import com.duck.saver.notification.dto.NotificationConfigResponse;
import com.duck.saver.notification.dto.RecipientInfoResponse;
import com.duck.saver.notification.dto.RecipientResponse;
import com.duck.saver.notification.dto.SaveRecipientRequest;
import com.duck.saver.notification.entity.NotificationConfigEntity;
import com.duck.saver.notification.entity.RecipientEntity;
import com.duck.saver.notification.mapper.NotificationConfigMapper;
import com.duck.saver.notification.mapper.RecipientMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecipientService {

	private static final Logger log = LoggerFactory.getLogger(RecipientService.class);

	public static final String TYPE_BACKUP = "BACKUP";
	public static final String TYPE_BILL_REMINDER = "BILL_REMINDER";

	@Autowired
	private RecipientMapper recipientMapper;

	@Autowired
	private NotificationConfigMapper configMapper;

	public RecipientResponse findByAccountName(String accountName) {

		RecipientEntity recipient = loadRecipient(accountName);
		List<NotificationConfigEntity> configs = configsOf(recipient.getId());

		return assemble(recipient, configs);
	}

	public RecipientResponse save(String accountName, SaveRecipientRequest request) {

		String frequency = normalizeFrequency(request.getFrequency());

		RecipientEntity recipient = recipientMapper.selectOne(
				new LambdaQueryWrapper<RecipientEntity>().eq(RecipientEntity::getAccountName, accountName));

		if (recipient == null) {
			recipient = new RecipientEntity();
			recipient.setAccountName(accountName);
		}
		recipient.setEmail(request.getEmail());
		recipient.setFrequency(frequency);
		recipient.setEnabled(request.getEnabled() == null || request.getEnabled());
		if (recipient.getId() == null) {
			recipientMapper.insert(recipient);
		} else {
			recipientMapper.updateById(recipient);
		}

		upsertDefaultConfig(recipient.getId(), TYPE_BACKUP, "0 0 12 * * *");
		upsertDefaultConfig(recipient.getId(), TYPE_BILL_REMINDER, "0 0 10 1 * *");

		log.info("recipient {} settings has been updated", accountName);
		return findByAccountName(accountName);
	}

	public List<NotificationConfigEntity> findReadyToNotify(String type) {

		LocalDateTime now = LocalDateTime.now();
		List<NotificationConfigEntity> result = new ArrayList<>();

		for (RecipientEntity recipient : recipientMapper.selectList(
				new LambdaQueryWrapper<RecipientEntity>().eq(RecipientEntity::getEnabled, true))) {

			Frequency frequency = parseFrequency(recipient.getFrequency());
			for (NotificationConfigEntity config : configsOf(recipient.getId())) {
				if (!type.equals(config.getType()) || !Boolean.TRUE.equals(config.getActive())) {
					continue;
				}
				LocalDateTime lastNotified = config.getLastNotified();
				if (lastNotified == null
						|| lastNotified.isBefore(now.minusDays(frequency.getDays()))) {
					result.add(config);
				}
			}
		}
		return result;
	}

	public void markNotified(NotificationConfigEntity config) {
		config.setLastNotified(LocalDateTime.now());
		configMapper.updateById(config);
	}

	private void upsertDefaultConfig(Long recipientId, String type, String cron) {

		NotificationConfigEntity existing = configMapper.selectOne(
				new LambdaQueryWrapper<NotificationConfigEntity>()
						.eq(NotificationConfigEntity::getRecipientId, recipientId)
						.eq(NotificationConfigEntity::getType, type));

		if (existing == null) {
			NotificationConfigEntity config = new NotificationConfigEntity();
			config.setRecipientId(recipientId);
			config.setType(type);
			config.setCronExpression(cron);
			config.setActive(true);
			config.setLastNotified(LocalDateTime.now());
			configMapper.insert(config);
		} else if (existing.getCronExpression() == null) {
			existing.setCronExpression(cron);
			configMapper.updateById(existing);
		}
	}

	private RecipientEntity loadRecipient(String accountName) {

		RecipientEntity recipient = recipientMapper.selectOne(
				new LambdaQueryWrapper<RecipientEntity>().eq(RecipientEntity::getAccountName, accountName));
		if (recipient == null) {
			throw new com.duck.saver.common.api.NotFoundException("can't find recipient with name " + accountName);
		}
		return recipient;
	}

	private List<NotificationConfigEntity> configsOf(Long recipientId) {
		return configMapper.selectList(new LambdaQueryWrapper<NotificationConfigEntity>()
				.eq(NotificationConfigEntity::getRecipientId, recipientId));
	}

	private RecipientResponse assemble(RecipientEntity recipient, List<NotificationConfigEntity> configs) {

		RecipientInfoResponse info = new RecipientInfoResponse();
		info.setName(recipient.getAccountName());
		info.setEmail(recipient.getEmail());
		info.setFrequency(recipient.getFrequency() == null ? Frequency.WEEKLY.name() : recipient.getFrequency());
		info.setEnabled(!Boolean.FALSE.equals(recipient.getEnabled()));

		RecipientResponse response = new RecipientResponse();
		response.setRecipient(info);
		response.setNotificationConfig(configs.stream()
				.map(c -> new NotificationConfigResponse(c.getType(), c.getCronExpression()))
				.toList());
		return response;
	}

	private String normalizeFrequency(String raw) {
		if (raw == null || raw.isBlank()) {
			return Frequency.WEEKLY.name();
		}
		try {
			return Frequency.valueOf(raw).name();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid frequency: " + raw);
		}
	}

	private Frequency parseFrequency(String raw) {
		try {
			return Frequency.valueOf(raw == null ? Frequency.WEEKLY.name() : raw);
		} catch (IllegalArgumentException e) {
			return Frequency.WEEKLY;
		}
	}

	static String[] supportedTypes() {
		return new String[]{TYPE_BACKUP, TYPE_BILL_REMINDER};
	}
}
