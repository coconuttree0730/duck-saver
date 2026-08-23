package com.duck.saver.ai.controller;

import com.duck.saver.common.web.ResultWrapAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PingControllerTest {

	private final ObjectMapper mapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(new PingController())
				.setControllerAdvice(new ResultWrapAdvice(mapper))
				.build();
	}

	@Test
	public void shouldPingPong() throws Exception {
		mockMvc.perform(get("/ping"))
				.andExpect(jsonPath("$.data.status").value("pong"))
				.andExpect(status().isOk());
	}
}
