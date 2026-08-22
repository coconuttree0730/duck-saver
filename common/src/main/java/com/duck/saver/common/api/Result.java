package com.duck.saver.common.api;

public class Result<T> {

	private Integer code;
	private String message;
	private T data;
	private Long timestamp;

	public Result() {
		this.timestamp = System.currentTimeMillis();
	}

	public static <T> Result<T> success() {
		return response(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
	}

	public static <T> Result<T> success(T data) {
		Result<T> result = success();
		result.setData(data);
		return result;
	}

	public static <T> Result<T> error(String message) {
		return error(ResultCode.INTERNAL_ERROR, message);
	}

	public static <T> Result<T> error(ResultCode code) {
		return error(code, code.getMessage());
	}

	public static <T> Result<T> error(ResultCode code, String message) {
		return response(code.getCode(), message, null);
	}

	public static <T> Result<T> response(Integer code, String message, T data) {
		Result<T> result = new Result<>();
		result.setCode(code);
		result.setMessage(message);
		result.setData(data);
		return result;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
}
