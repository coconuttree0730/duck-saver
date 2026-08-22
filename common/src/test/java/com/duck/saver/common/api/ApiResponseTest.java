package com.duck.saver.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

	@Test
	void shouldWrapDataAsSuccess() {
		ApiResponse<String> response = ApiResponse.ok("payload");
		assertEquals(ApiResponse.SUCCESS, response.getCode());
		assertEquals("success", response.getMessage());
		assertEquals("payload", response.getData());
	}

	@Test
	void shouldBuildFailureWithoutData() {
		ApiResponse<Void> response = ApiResponse.fail(400, "bad request");
		assertEquals(400, response.getCode());
		assertEquals("bad request", response.getMessage());
		assertNull(response.getData());
	}
}
