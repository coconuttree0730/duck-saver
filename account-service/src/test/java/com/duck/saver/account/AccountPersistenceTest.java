package com.duck.saver.account;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.account.entity.AccountEntity;
import com.duck.saver.account.entity.TransactionEntity;
import com.duck.saver.account.mapper.AccountMapper;
import com.duck.saver.account.mapper.TransactionMapper;
import com.duck.saver.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 数据库级行为验证：唯一约束、乐观锁、逻辑删除（Seam 2 · Testcontainers mysql:8）。
 */
@SpringBootTest
@Testcontainers
class AccountPersistenceTest {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
				.withDatabaseName("duck_saver_account")
				.withInitScript("sql/account_schema.sql");

	@Container
	@ServiceConnection
	static org.testcontainers.containers.GenericContainer<?> redis =
			new org.testcontainers.containers.GenericContainer<>("redis:7").withExposedPorts(6379);

	@Autowired
	private AccountMapper accountMapper;

	@Autowired
	private TransactionMapper transactionMapper;

	@Test
	public void shouldRejectDuplicateAccountName() {

		AccountEntity first = insertAccount("dup-name");

		AccountEntity second = new AccountEntity();
		second.setName("dup-name");
		second.setCurrency("CNY");

		assertThrows(Exception.class, () -> accountMapper.insert(second));
		assertNotNull(accountMapper.selectOne(
				new LambdaQueryWrapper<AccountEntity>().eq(AccountEntity::getName, "dup-name")));
	}

	@Test
	public void shouldBumpVersionOnOptimisticUpdate() {

		AccountEntity account = insertAccount("opt-lock");

		// 模拟真实流程：先查出带版本号的实体，再修改更新
		account = accountMapper.selectById(account.getId());
		assertNotNull(account.getVersion());
		account.setCurrency("USD");
		assertEquals(1, accountMapper.updateById(account));

		AccountEntity reloaded = accountMapper.selectById(account.getId());
		assertEquals(1L, reloaded.getVersion());

		// 携带过期版本再次更新应失败（影响行数 0）
		AccountEntity stale = new AccountEntity();
		stale.setId(reloaded.getId());
		stale.setVersion(0L);
		stale.setCurrency("EUR");
		assertEquals(0, accountMapper.updateById(stale));
	}

	@Test
	public void shouldHideLogicallyDeletedTransaction() {

		AccountEntity account = insertAccount("logic-del");

		TransactionEntity transaction = new TransactionEntity();
		transaction.setAccountId(account.getId());
		transaction.setTitle("午餐");
		transaction.setAmount(new BigDecimal("48.00"));
		transaction.setCurrency("CNY");
		transaction.setCategory("餐饮");
		transaction.setType(TransactionEntity.TYPE_EXPENSE);
		transaction.setDate(LocalDate.now());
		transactionMapper.insert(transaction);

		transactionMapper.deleteById(transaction.getId());

		assertNull(transactionMapper.selectById(transaction.getId()));
	}

	private AccountEntity insertAccount(String name) {
		AccountEntity account = new AccountEntity();
		account.setName(name);
		account.setCurrency("CNY");
		accountMapper.insert(account);
		return account;
	}
}
