package com.duck.saver.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultTest {

	@Test
	void shouldWrapDataAsSuccessWithHttpAlignedCode() {
		Result<String> result = Result.success("payload");
		assertEquals(200, result.getCode());
		assertEquals("操作成功", result.getMessage());
		assertEquals("payload", result.getData());
		assertNotNull(result.getTimestamp());
	}

	@Test
	void shouldBuildSuccessWithoutData() {
		Result<Void> result = Result.success();
		assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
		assertNull(result.getData());
	}

	@Test
	void shouldBuildFailureFromResultCode() {
		Result<Void> result = Result.error(ResultCode.CONFLICT);
		assertEquals(409, result.getCode());
		assertEquals("冲突", result.getMessage());
		assertNull(result.getData());
	}

	@Test
	void shouldAllowCustomMessageOverridingDefault() {
		Result<Void> result = Result.error(ResultCode.PARAM_ERROR, "category 非法");
		assertEquals(400, result.getCode());
		assertEquals("category 非法", result.getMessage());
	}

	@Test
	void shouldSupportCustomResponse() {
		Result<String> result = Result.response(1001, "账户不存在", null);
		assertEquals(1001, result.getCode());
	}
}
