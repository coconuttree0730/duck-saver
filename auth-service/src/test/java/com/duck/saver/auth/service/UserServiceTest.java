package com.duck.saver.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.auth.domain.User;
import com.duck.saver.auth.entity.UserEntity;
import com.duck.saver.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class UserServiceTest {

	@InjectMocks
	private UserServiceImpl userService;

	@Mock
	private UserMapper userMapper;

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@BeforeEach
	public void setup() throws Exception {
		openMocks(this).close();
	}

	private User user(String username, String password) {
		User user = new User();
		user.setUsername(username);
		user.setPassword(password);
		return user;
	}

	@Test
	public void shouldCreateUserWithHashedPassword() {

		when(userMapper.selectList(any())).thenReturn(List.of());

		userService.create(user("test", "password"));

		ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
		verify(userMapper, times(1)).insert(captor.capture());
		assertNotEqualsRaw(captor.getValue().getPassword());
		assertTrue(encoder.matches("password", captor.getValue().getPassword()));
		assertEquals("test", captor.getValue().getUsername());
	}

	@Test
	public void shouldFailToCreateDuplicateUser() {

		UserEntity existing = new UserEntity();
		existing.setUsername("dup");
		existing.setPassword(encoder.encode("x"));

		when(userMapper.selectList(any())).thenReturn(List.of(existing));

		assertThrows(IllegalArgumentException.class, () -> userService.create(user("dup", "password")));
	}

	@Test
	public void shouldAuthenticateWithCorrectPassword() {

		UserEntity stored = new UserEntity();
		stored.setUsername("test");
		stored.setPassword(encoder.encode("password"));

		when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stored);

		User authenticated = userService.authenticate("test", "password");

		assertEquals("test", authenticated.getUsername());
	}

	@Test
	public void shouldFailAuthenticationWithWrongPassword() {

		UserEntity stored = new UserEntity();
		stored.setUsername("test");
		stored.setPassword(encoder.encode("password"));

		when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stored);

		assertThrows(IllegalArgumentException.class, () -> userService.authenticate("test", "wrong"));
	}

	@Test
	public void shouldFailAuthenticationForUnknownUser() {

		when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

		assertThrows(IllegalArgumentException.class, () -> userService.authenticate("ghost", "password"));
	}

	private void assertNotEqualsRaw(String password) {
		if ("password".equals(password)) {
			throw new AssertionError("password must be hashed");
		}
	}
}
