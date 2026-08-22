package com.duck.saver.auth.repository;

import com.duck.saver.auth.domain.User;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@Testcontainers
class UserRepositoryTest {

	@Container
	@ServiceConnection
	static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

	@Autowired
	private UserRepository repository;

	@Test
	public void shouldSaveAndFindUserByName() {

		User user = new User();
		user.setUsername("name");
		user.setPassword("password");
		repository.save(user);

		Optional<User> found = repository.findById(user.getUsername());
		assertTrue(found.isPresent());
		assertEquals(user.getUsername(), found.get().getUsername());
		assertEquals(user.getPassword(), found.get().getPassword());
	}
}
