package com.duck.saver.common.web;

import com.duck.saver.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
		log.info("Returning HTTP 400 Bad Request: {}", e.getMessage());
		return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.findFirst()
				.orElse("validation failed");
		return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), message);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiResponse<Void> handleUnexpected(Exception e) {
		log.error("Returning HTTP 500 Internal Server Error", e);
		return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "internal server error");
	}
}
