package com.duck.saver.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.common.event.AccountEvent;
import com.duck.saver.common.event.AccountSnapshot;
import com.duck.saver.statistics.domain.Account;
import com.duck.saver.statistics.entity.ProcessedEventEntity;
import com.duck.saver.statistics.mapper.ProcessedEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件消费逻辑：按 eventId 幂等去重，数据点落库与 processed_event 写入同事务。
 */
@Service
public class AccountEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

	@Autowired
	private StatisticsService statisticsService;

	@Autowired
	private ProcessedEventMapper processedEventMapper;

	/**
	 * @return true=已处理（含重复事件），false=处理失败需重试
	 */
	@Transactional
	public boolean consume(AccountEvent event) {
		if (event == null || event.getEventId() == null) {
			log.warn("discarding malformed event: {}", event);
			return true;
		}

		Long count = processedEventMapper.selectCount(
				new LambdaQueryWrapper<ProcessedEventEntity>().eq(ProcessedEventEntity::getEventId, event.getEventId()));
		if (count != null && count > 0) {
			log.info("duplicate event ignored: {} ({})", event.getEventId(), event.getEventType());
			return true;
		}

		Account account = toAccount(event.getData());
		statisticsService.save(event.getAccountName(), account);

		ProcessedEventEntity record = new ProcessedEventEntity();
		record.setEventId(event.getEventId());
		record.setEventType(event.getEventType() == null ? "UNKNOWN" : event.getEventType().name());
		record.setConsumedAt(LocalDateTime.now());
		processedEventMapper.insert(record);

		log.info("event consumed: {} for account {}", event.getEventId(), event.getAccountName());
		return true;
	}

	private Account toAccount(AccountSnapshot snapshot) {
		Account account = new Account();
		account.setIncomes(items(snapshot == null ? null : snapshot.getIncomes()));
		account.setExpenses(items(snapshot == null ? null : snapshot.getExpenses()));

		Account.Saving saving = new Account.Saving();
		saving.setAmount(snapshot != null && snapshot.getSaving() != null && snapshot.getSaving().getAmount() != null
				? snapshot.getSaving().getAmount()
				: BigDecimal.ZERO);
		account.setSaving(saving);
		return account;
	}

	private List<Account.Item> items(List<AccountSnapshot.Item> source) {
		if (source == null) {
			return List.of();
		}
		return source.stream().map(i -> {
			Account.Item item = new Account.Item();
			item.setTitle(i.getTitle());
			item.setAmount(i.getAmount());
			return item;
		}).toList();
	}
}
