package com.duck.saver.account.event;

import com.duck.saver.account.entity.AccountEntity;
import com.duck.saver.account.entity.SavingEntity;
import com.duck.saver.account.entity.TransactionEntity;
import com.duck.saver.account.mapper.SavingMapper;
import com.duck.saver.account.mapper.TransactionMapper;
import com.duck.saver.common.event.AccountEvent;
import com.duck.saver.common.event.AccountSnapshot;
import com.duck.saver.common.event.EventType;
import com.duck.saver.common.event.MqTopology;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账户事件发布器：业务落库后发布胖事件（整户快照），statistics 消费零回调。
 */
@Component
public class AccountEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(AccountEventPublisher.class);

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private TransactionMapper transactionMapper;

	@Autowired
	private SavingMapper savingMapper;

	public void publish(EventType eventType, AccountEntity account) {
		AccountEvent event = AccountEvent.of(eventType, account.getName(), snapshotOf(account));
		rabbitTemplate.convertAndSend(MqTopology.EXCHANGE, "", event);
		log.info("event published: {} for account {} (eventId={})", eventType, account.getName(), event.getEventId());
	}

	public AccountSnapshot snapshotOf(AccountEntity account) {
		List<TransactionEntity> transactions = transactionMapper.selectList(
				new LambdaQueryWrapper<TransactionEntity>().eq(TransactionEntity::getAccountId, account.getId()));

		SavingEntity saving = savingMapper.selectOne(
				new LambdaQueryWrapper<SavingEntity>().eq(SavingEntity::getAccountId, account.getId()));

		AccountSnapshot snapshot = new AccountSnapshot();
		snapshot.setIncomes(transactions.stream()
				.filter(t -> TransactionEntity.TYPE_INCOME.equals(t.getType()))
				.map(t -> new AccountSnapshot.Item(t.getTitle(), t.getAmount()))
				.toList());
		snapshot.setExpenses(transactions.stream()
				.filter(t -> TransactionEntity.TYPE_EXPENSE.equals(t.getType()))
				.map(t -> new AccountSnapshot.Item(t.getTitle(), t.getAmount()))
				.toList());
		snapshot.setSaving(new AccountSnapshot.Saving(
				saving == null ? BigDecimal.ZERO : saving.getAmount()));
		return snapshot;
	}
}
