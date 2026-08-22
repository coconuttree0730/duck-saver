package com.duck.saver.auth.controller;

import com.duck.saver.auth.domain.User;
import com.duck.saver.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.security.auth.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@InjectMocks
	private UserController userController;

	@Mock
	private UserService userService;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		this.mockMvc = MockMvcBuilders.standaloneSetup(userController)
				.setControllerAdvice(new com.duck.saver.common.web.GlobalExceptionHandler())
				.build();
	}

	@Test
	public void shouldCreateNewUser() throws Exception {

		final User user = new User();
		user.setUsername("test");
		user.setPassword("password");

		String json = mapper.writeValueAsString(user);

		mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());
	}

	@Test
	public void shouldFailWith400WhenUserAlreadyExists() throws Exception {

		final User user = new User();
		user.setUsername("test");
		user.setPassword("password");

		doThrow(new IllegalArgumentException("user already exists: test")).when(userService).create(any(User.class));

		String json = mapper.writeValueAsString(user);

		mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	public void shouldGetCurrentUser() throws Exception {

		mockMvc.perform(get("/users/current").principal(new UserPrincipal("test")))
				.andExpect(status().isOk());
	}
}
