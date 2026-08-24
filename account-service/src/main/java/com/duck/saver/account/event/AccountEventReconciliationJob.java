package com.duck.saver.account.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.account.entity.AccountEntity;
import com.duck.saver.account.mapper.AccountMapper;
import com.duck.saver.common.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产侧定时兜底：扫描最近窗口内有变更的账户并重发整户快照事件。
 * 消费侧幂等 + 同日覆盖写，重复投递无副作用；用于弥补 broker 不可用窗口期的发布丢失。
 */
@Component
public class AccountEventReconciliationJob {

	private static final Logger log = LoggerFactory.getLogger(AccountEventReconciliationJob.class);

	@Autowired
	private AccountMapper accountMapper;

	@Autowired
	private AccountEventPublisher eventPublisher;

	@Value("${account.event.reconciliation.window-minutes:30}")
	private int windowMinutes;

	@Scheduled(fixedDelayString = "${account.event.reconciliation.interval-ms:600000}",
			initialDelayString = "${account.event.reconciliation.initial-delay-ms:120000}")
	public void reconcile() {
		LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
		List<AccountEntity> changed = accountMapper.selectList(
				new LambdaQueryWrapper<AccountEntity>().ge(AccountEntity::getUpdatedAt, since));
		for (AccountEntity account : changed) {
			try {
				eventPublisher.publish(EventType.ACCOUNT_UPDATED, account);
			} catch (Exception e) {
				log.warn("reconciliation publish failed for {}: {}", account.getName(), e.getMessage());
			}
		}
		if (!changed.isEmpty()) {
			log.info("reconciliation republished {} account snapshots", changed.size());
		}
	}
}
