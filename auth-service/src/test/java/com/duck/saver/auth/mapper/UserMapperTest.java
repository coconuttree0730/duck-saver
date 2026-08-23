package com.duck.saver.auth.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.auth.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Testcontainers
class UserMapperTest {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("duck_saver_auth")
			.withInitScript("sql/auth_schema.sql");

	@Autowired
	private UserMapper userMapper;

	private UserEntity insert(String username) {
		UserEntity entity = new UserEntity();
		entity.setUsername(username);
		entity.setPassword("hash");
		userMapper.insert(entity);
		return entity;
	}

	@Test
	public void shouldSaveAndFindByUsername() {
		insert("name");

		UserEntity found = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
				.eq(UserEntity::getUsername, "name"));
		assertNotNull(found);
		assertEquals("name", found.getUsername());
	}

	@Test
	public void shouldHideLogicallyDeletedUser() {
		UserEntity entity = insert("gone");
		userMapper.deleteById(entity.getId());
		assertNull(userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
				.eq(UserEntity::getUsername, "gone")));
	}
}
