package com.duck.saver.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.cache.annotation.Cacheable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.cache.annotation.CacheEvict;
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
	private AccountEventPublisher eventPublisher;

	@Autowired
	private RedissonClient redissonClient;

	private static final int MAX_OPTIMISTIC_RETRIES = 3;

	private <T> T withAccountLock(AccountEntity account, Supplier<T> action) {
		RLock lock = redissonClient.getLock("lock:account:" + account.getId());
		boolean acquired;
		try {
			acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ConflictException("account lock interrupted: " + account.getName());
		}
		if (!acquired) {
			throw new ConflictException("failed to acquire account lock: " + account.getName());
		}
		try {
			return action.get();
		} finally {
			lock.unlock();
		}
	}

	@Override
	@Cacheable(cacheNames = "accounts", key = "#accountName")
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

		eventPublisher.publish(EventType.ACCOUNT_CREATED, account);
		log.info("new account has been created: {}", account.getName());
		return assemble(account);
	}

	@Override
	@CacheEvict(cacheNames = "accounts", key = "#name")
	public void update(String name, UpdateAccountRequest request) {
		withAccountLock(loadAccount(name), () -> {
			for (int attempt = 1; attempt <= MAX_OPTIMISTIC_RETRIES; attempt++) {
				AccountEntity current = loadAccount(name);
				if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
					current.setCurrency(request.getCurrency());
				}
				if (accountMapper.updateById(current) > 0) {
					eventPublisher.publish(EventType.ACCOUNT_UPDATED, current);
					log.debug("account {} changes has been saved", name);
					return null;
				}
				log.info("optimistic conflict on account {}, retry {}/{}", name, attempt, MAX_OPTIMISTIC_RETRIES);
			}
			throw new ConflictException("concurrent modification on account: " + name);
		});
	}

	@Override
	@CacheEvict(cacheNames = "accounts", key = "#name")
	public void delete(String name) {
		AccountEntity account = loadAccount(name);
		accountMapper.deleteById(account.getId());
		transactionMapper.delete(new LambdaQueryWrapper<TransactionEntity>().eq(TransactionEntity::getAccountId, account.getId()));
		savingMapper.delete(new LambdaQueryWrapper<SavingEntity>().eq(SavingEntity::getAccountId, account.getId()));
		eventPublisher.publish(EventType.ACCOUNT_DELETED, account);
		log.info("account has been deleted: {}", name);
	}

	@Override
	@CacheEvict(cacheNames = "accounts", key = "#name")
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
	}

	@Override
	@CacheEvict(cacheNames = "accounts", key = "#name")
	public void deleteItem(String name, String itemId) {
		AccountEntity account = loadAccount(name);
		TransactionEntity transaction = transactionMapper.selectById(itemId);
		if (transaction == null || !transaction.getAccountId().equals(account.getId())) {
			throw new NotFoundException("transaction not found: " + itemId);
		}
		transactionMapper.deleteById(transaction.getId());
		eventPublisher.publish(EventType.ITEM_DELETED, account);
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

	private void validate(String category, String type) {
		if (!CATEGORIES.contains(category)) {
			throw new IllegalArgumentException("invalid category: " + category);
		}
		if (!TYPES.contains(type)) {
			throw new IllegalArgumentException("invalid type: " + type);
		}
	}
}
