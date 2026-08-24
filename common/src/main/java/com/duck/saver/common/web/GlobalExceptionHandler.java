package com.duck.saver.common.web;

import com.duck.saver.common.api.BusinessException;
import com.duck.saver.common.api.ConflictException;
import com.duck.saver.common.api.UnauthorizedException;
import com.duck.saver.common.api.NotFoundException;
import com.duck.saver.common.api.Result;
import com.duck.saver.common.api.ResultCode;
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
	public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
		log.info("Returning HTTP 400 Bad Request: {}", e.getMessage());
		return Result.error(ResultCode.PARAM_ERROR, e.getMessage());
	}

	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Result<Void> handleNotFound(NotFoundException e) {
		log.info("Returning HTTP 404 Not Found: {}", e.getMessage());
		return Result.error(ResultCode.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Result<Void> handleUnauthorized(UnauthorizedException e) {
		log.info("Returning HTTP 401 Unauthorized: {}", e.getMessage());
		return Result.error(ResultCode.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Result<Void> handleConflict(ConflictException e) {
		log.info("Returning HTTP 409 Conflict: {}", e.getMessage());
		return Result.error(ResultCode.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(BusinessException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Void> handleBusiness(BusinessException e) {
		log.info("Returning business error {}: {}", e.getCode(), e.getMessage());
		return Result.response(e.getCode(), e.getMessage(), null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Result<Void> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.findFirst()
				.orElse("validation failed");
		return Result.error(ResultCode.PARAM_ERROR, message);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Result<Void> handleUnexpected(Exception e) {
		log.error("Returning HTTP 500 Internal Server Error", e);
		return Result.error(ResultCode.INTERNAL_ERROR);
	}
}
