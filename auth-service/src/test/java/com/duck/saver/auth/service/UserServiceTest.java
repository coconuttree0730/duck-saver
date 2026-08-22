package com.duck.saver.auth.service;

import com.duck.saver.auth.domain.User;
import com.duck.saver.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

	@InjectMocks
	private UserServiceImpl userService;

	@Mock
	private UserRepository repository;

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@BeforeEach
	public void setup() throws Exception {
		org.mockito.MockitoAnnotations.openMocks(this).close();
	}

	@Test
	public void shouldCreateUserWithHashedPassword() {

		User user = new User();
		user.setUsername("test");
		user.setPassword("password");

		userService.create(user);

		verify(repository, times(1)).save(user);
		org.junit.jupiter.api.Assertions.assertNotEquals("password", user.getPassword());
		org.junit.jupiter.api.Assertions.assertTrue(encoder.matches("password", user.getPassword()));
	}

	@Test
	public void shouldFailToCreateDuplicateUser() {

		User user = new User();
		user.setUsername("test");
		user.setPassword("password");

		when(repository.findById(user.getUsername())).thenReturn(Optional.of(user));

		assertThrows(IllegalArgumentException.class, () -> userService.create(user));
	}

	@Test
	public void shouldAuthenticateWithCorrectPassword() {

		User stored = new User();
		stored.setUsername("test");
		stored.setPassword(encoder.encode("password"));

		when(repository.findById("test")).thenReturn(Optional.of(stored));

		User authenticated = userService.authenticate("test", "password");

		assertEquals("test", authenticated.getUsername());
	}

	@Test
	public void shouldFailAuthenticationWithWrongPassword() {

		User stored = new User();
		stored.setUsername("test");
		stored.setPassword(encoder.encode("password"));

		when(repository.findById("test")).thenReturn(Optional.of(stored));

		assertThrows(IllegalArgumentException.class, () -> userService.authenticate("test", "wrong"));
	}

	@Test
	public void shouldFailAuthenticationForUnknownUser() {

		when(repository.findById("ghost")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> userService.authenticate("ghost", "password"));
	}
}
