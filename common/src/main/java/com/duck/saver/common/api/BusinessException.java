package com.duck.saver.common.api;

/**
 * 携带业务错误码的运行时异常（如认证段 4xxx：4001 provider 未开放、
 * 4002 第三方凭证无效、4003 该身份已绑定其他账号）。由全局异常处理器统一转响应体。
 */
public class BusinessException extends RuntimeException {

	private final int code;

	public BusinessException(int code, String message) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
