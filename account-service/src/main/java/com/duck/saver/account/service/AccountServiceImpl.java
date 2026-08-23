package com.duck.saver.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.account.client.StatisticsServiceClient;
import com.duck.saver.account.client.dto.StatisticsPayload;
import com.duck.saver.account.dto.AccountResponse;
import com.duck.saver.account.dto.CreateAccountRequest;
import com.duck.saver.account.dto.ItemResponse;
import com.duck.saver.account.dto.SavingResponse;
import com.duck.saver.account.dto.TransactionItemRequest;
import com.duck.saver.account.dto.UpdateAccountRequest;
import com.duck.saver.account.entity.AccountEntity;
import com.duck.saver.account.entity.SavingEntity;
import com.duck.saver.account.entity.TransactionEntity;
import com.duck.saver.account.mapper.AccountMapper;
import com.duck.saver.account.mapper.SavingMapper;
import com.duck.saver.account.mapper.TransactionMapper;
import com.duck.saver.common.api.ConflictException;
import com.duck.saver.common.api.NotFoundException;
import com.duck.saver.account.event.AccountEventPublisher;
import com.duck.saver.common.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class AccountServiceImpl implements AccountService {

	private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

	public static final Set<String> CATEGORIES = Set.of("餐饮", "交通", "购物", "娱乐", "居住", "通讯", "医疗", "教育", "其他");
	public static final Set<String> TYPES = Set.of(TransactionEntity.TYPE_INCOME, TransactionEntity.TYPE_EXPENSE);

	@Autowired
	private AccountMapper accountMapper;

	@Autowired
	private TransactionMapper transactionMapper;

	@Autowired
	private SavingMapper savingMapper;

	@Autowired
	private StatisticsServiceClient statisticsClient;

	@Autowired
	private AccountEventPublisher eventPublisher;

	@Override
	public AccountResponse findByName(String accountName) {
		return assemble(loadAccount(accountName));
	}

	@Override
	public AccountResponse create(CreateAccountRequest request) {
		if (accountMapper.selectOne(new LambdaQueryWrapper<AccountEntity>().eq(AccountEntity::getName, request.getName())) != null) {
			throw new ConflictException("account already exists: " + request.getName());
		}

		AccountEntity account = new AccountEntity();
		account.setName(request.getName());
		account.setCurrency(request.getCurrency());
		accountMapper.insert(account);

		SavingEntity saving = new SavingEntity();
		saving.setAccountId(account.getId());
		saving.setAmount(BigDecimal.ZERO);
		saving.setInterest(BigDecimal.ZERO);
		saving.setDeposit(BigDecimal.ZERO);
		saving.setCurrency(request.getCurrency());
		savingMapper.insert(saving);

		log.info("new account has been created: {}", account.getName());
		return assemble(account);
	}

	@Override
	public void update(String name, UpdateAccountRequest request) {
		AccountEntity account = loadAccount(name);
		if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
			account.setCurrency(request.getCurrency());
		}
		if (accountMapper.updateById(account) == 0) {
			throw new ConflictException("concurrent modification on account: " + name);
		}
		log.debug("account {} changes has been saved", name);
	}

	@Override
	public void delete(String name) {
		AccountEntity account = loadAccount(name);
		accountMapper.deleteById(account.getId());
		transactionMapper.delete(new LambdaQueryWrapper<TransactionEntity>().eq(TransactionEntity::getAccountId, account.getId()));
		savingMapper.delete(new LambdaQueryWrapper<SavingEntity>().eq(SavingEntity::getAccountId, account.getId()));
		log.info("account has been deleted: {}", name);
	}

	@Override
	public void addItem(String name, TransactionItemRequest request) {
		validate(request.getCategory(), request.getType());

		AccountEntity account = loadAccount(name);

		TransactionEntity transaction = new TransactionEntity();
		transaction.setAccountId(account.getId());
		transaction.setTitle(request.getTitle());
		transaction.setAmount(request.getAmount());
		transaction.setCurrency(request.getCurrency());
		transaction.setCategory(request.getCategory());
		transaction.setType(request.getType());
		transaction.setDate(request.getDate());
		transactionMapper.insert(transaction);

		eventPublisher.publish(EventType.ITEM_ADDED, account);

		pushStatistics(account);
	}

	@Override
	public void deleteItem(String name, String itemId) {
		AccountEntity account = loadAccount(name);
		TransactionEntity transaction = transactionMapper.selectById(itemId);
		if (transaction == null || !transaction.getAccountId().equals(account.getId())) {
			throw new NotFoundException("transaction not found: " + itemId);
		}
		transactionMapper.deleteById(transaction.getId());
		pushStatistics(account);
	}

	@Override
	public AccountResponse demo() {
		return findByName("demo");
	}

	private AccountEntity loadAccount(String name) {
		AccountEntity account = accountMapper.selectOne(
				new LambdaQueryWrapper<AccountEntity>().eq(AccountEntity::getName, name));
		if (account == null) {
			throw new NotFoundException("can't find account with name " + name);
		}
		return account;
	}

	private AccountResponse assemble(AccountEntity account) {
		List<TransactionEntity> transactions = transactionMapper.selectList(
				new LambdaQueryWrapper<TransactionEntity>()
						.eq(TransactionEntity::getAccountId, account.getId())
						.orderByAsc(TransactionEntity::getDate));

		SavingEntity saving = savingMapper.selectOne(
				new LambdaQueryWrapper<SavingEntity>().eq(SavingEntity::getAccountId, account.getId()));

		AccountResponse response = new AccountResponse();
		response.setName(account.getName());
		response.setCurrency(account.getCurrency());
		response.setLastUpdate(account.getUpdatedAt());
		response.setItems(transactions.stream()
				.map(t -> new ItemResponse(t.getId(), t.getTitle(), t.getAmount(), t.getCurrency(),
						t.getCategory(), t.getType(), t.getDate()))
				.toList());
		response.setSaving(saving == null ? null
				: new SavingResponse(saving.getAmount(), saving.getInterest(), saving.getDeposit(), saving.getCurrency()));
		return response;
	}

	private void pushStatistics(AccountEntity account) {
		List<TransactionEntity> transactions = transactionMapper.selectList(
				new LambdaQueryWrapper<TransactionEntity>().eq(TransactionEntity::getAccountId, account.getId()));

		SavingEntity saving = savingMapper.selectOne(
				new LambdaQueryWrapper<SavingEntity>().eq(SavingEntity::getAccountId, account.getId()));

		StatisticsPayload payload = new StatisticsPayload();
		payload.setIncomes(transactions.stream()
				.filter(t -> TransactionEntity.TYPE_INCOME.equals(t.getType()))
				.map(t -> new StatisticsPayload.ItemPayload(t.getTitle(), t.getAmount()))
				.toList());
		payload.setExpenses(transactions.stream()
				.filter(t -> TransactionEntity.TYPE_EXPENSE.equals(t.getType()))
				.map(t -> new StatisticsPayload.ItemPayload(t.getTitle(), t.getAmount()))
				.toList());
		payload.setSaving(new StatisticsPayload.SavingPayload(
				saving == null ? BigDecimal.ZERO : saving.getAmount()));

		statisticsClient.updateStatistics(account.getName(), payload);
	}

	private void validate(String category, String type) {
		if (!CATEGORIES.contains(category)) {
			throw new IllegalArgumentException("invalid category: " + category);
		}
		if (!TYPES.contains(type)) {
			throw new IllegalArgumentException("invalid type: " + type);
		}
	}
}
