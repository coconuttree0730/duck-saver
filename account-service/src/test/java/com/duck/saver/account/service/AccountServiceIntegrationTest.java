package com.duck.saver.account.service;

import com.duck.saver.account.client.StatisticsServiceClient;
import com.duck.saver.account.client.dto.StatisticsPayload;
import com.duck.saver.account.dto.AccountResponse;
import com.duck.saver.account.dto.CreateAccountRequest;
import com.duck.saver.account.dto.TransactionItemRequest;
import com.duck.saver.account.dto.UpdateAccountRequest;
import com.duck.saver.account.entity.AccountEntity;
import com.duck.saver.account.mapper.AccountMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 服务层集成测试：真实 MySQL（Testcontainers）+ Mockito 隔离外部 Feign。
 */
@SpringBootTest
@Testcontainers
class AccountServiceIntegrationTest {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
				.withDatabaseName("duck_saver_account")
				.withInitScript("sql/account_schema.sql");

	@Container
	@ServiceConnection
	static org.testcontainers.containers.GenericContainer<?> redis =
			new org.testcontainers.containers.GenericContainer<>("redis:7").withExposedPorts(6379);

	@Container
	@ServiceConnection
	static org.testcontainers.containers.RabbitMQContainer rabbit =
			new org.testcontainers.containers.RabbitMQContainer("rabbitmq:3.13-management");

	@Autowired
	private AccountServiceImpl accountService;

	@Autowired
	private AccountMapper accountMapper;

	@org.springframework.boot.test.mock.mockito.MockBean
	private StatisticsServiceClient statisticsClient;

	@Test
	public void shouldCreateAccountWithDefaultSaving() {

		CreateAccountRequest request = new CreateAccountRequest();
		request.setName("alice");
		request.setCurrency("CNY");

		AccountResponse response = accountService.create(request);

		assertEquals("alice", response.getName());
		assertEquals("CNY", response.getCurrency());
		assertNotNull(response.getSaving());
		assertEquals(0, BigDecimal.ZERO.compareTo(response.getSaving().getAmount()));
		assertEquals(0, response.getItems().size());

		verify(statisticsClient, times(0)).updateStatistics(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any(StatisticsPayload.class));
	}

	@Test
	public void shouldRejectDuplicateAccountName() {

		CreateAccountRequest request = new CreateAccountRequest();
		request.setName("bob");
		request.setCurrency("CNY");
		accountService.create(request);

		assertThrows(com.duck.saver.common.api.ConflictException.class, () -> accountService.create(request));
	}

	@Test
	public void shouldAddAndDeleteTransactionWithStatisticsPush() {

		CreateAccountRequest request = new CreateAccountRequest();
		request.setName("carol");
		request.setCurrency("CNY");
		accountService.create(request);

		TransactionItemRequest item = new TransactionItemRequest();
		item.setTitle("午餐");
		item.setAmount(new BigDecimal("48.00"));
		item.setCurrency("CNY");
		item.setCategory("餐饮");
		item.setType("EXPENSE");
		item.setDate(LocalDate.now());

		accountService.addItem("carol", item);

		ArgumentCaptor<StatisticsPayload> captor = ArgumentCaptor.forClass(StatisticsPayload.class);
		verify(statisticsClient, times(1)).updateStatistics(org.mockito.ArgumentMatchers.eq("carol"),
				captor.capture());
		assertEquals(1, captor.getValue().getExpenses().size());

		AccountResponse response = accountService.findByName("carol");
		assertEquals(1, response.getItems().size());
		String itemId = response.getItems().get(0).getId();

		accountService.deleteItem("carol", itemId);
		assertEquals(0, accountService.findByName("carol").getItems().size());
	}

	@Test
	public void shouldFailOnInvalidCategory() {

		CreateAccountRequest request = new CreateAccountRequest();
		request.setName("dave");
		request.setCurrency("CNY");
		accountService.create(request);

		TransactionItemRequest item = new TransactionItemRequest();
		item.setTitle("神秘消费");
		item.setAmount(new BigDecimal("10.00"));
		item.setCurrency("CNY");
		item.setCategory("神秘分类");
		item.setType("EXPENSE");
		item.setDate(LocalDate.now());

		assertThrows(IllegalArgumentException.class, () -> accountService.addItem("dave", item));
	}

	@Test
	public void shouldSerializeConcurrentUpdatesWithoutLostUpdate() throws Exception {
		CreateAccountRequest request = new CreateAccountRequest();
		request.setName("erin");
		request.setCurrency("CNY");
		accountService.create(request);

		int threads = 4;
		int rounds = 5;
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
		java.util.List<java.util.concurrent.Callable<Void>> tasks = new java.util.ArrayList<>();
		for (int t = 0; t < threads; t++) {
			final String currency = "C" + t;
			tasks.add(() -> {
				for (int r = 0; r < rounds; r++) {
					UpdateAccountRequest update = new UpdateAccountRequest();
					update.setCurrency(currency);
					accountService.update("erin", update);
				}
				return null;
			});
		}
		pool.invokeAll(tasks).forEach(future -> {
			try {
				future.get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		pool.shutdown();

		String finalCurrency = accountService.findByName("erin").getCurrency();
		assertNotNull(finalCurrency);
		AccountEntity row = accountMapper.selectOne(
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AccountEntity>()
						.eq(AccountEntity::getName, "erin"));
		// 20 次更新全部成功且每次成功都递增乐观锁版本 → 无丢失更新
		assertEquals(threads * rounds, row.getVersion().intValue());
	}
}
