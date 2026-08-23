package com.duck.saver.common.api;

public enum ResultCode {

	SUCCESS(200, "操作成功"),
	PARAM_ERROR(400, "参数错误"),
	UNAUTHORIZED(401, "未登录或凭证失效"),
	FORBIDDEN(403, "无权限"),
	NOT_FOUND(404, "资源不存在"),
	CONFLICT(409, "冲突"),
	INTERNAL_ERROR(500, "系统异常");

	private final int code;
	private final String message;

	ResultCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
