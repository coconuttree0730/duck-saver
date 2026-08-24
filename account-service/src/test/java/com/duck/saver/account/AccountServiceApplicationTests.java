package com.duck.saver.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class AccountServiceApplicationTests {

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
	org.mybatis.spring.SqlSessionTemplate sqlSessionTemplate;

	@Test
	public void contextLoads() {
	}
}
