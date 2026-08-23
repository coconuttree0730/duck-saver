package com.duck.saver.auth.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.duck.saver.auth.dto.LoginRequest;
import com.duck.saver.auth.dto.LoginResponse;
import com.duck.saver.auth.service.RefreshTokenService;
import com.duck.saver.auth.service.UserService;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AuthController {

	private static final long ACCESS_TOKEN_EXPIRES_IN_SECONDS = 7 * 24 * 3600L;

	@Autowired
	private UserService userService;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {

		userService.authenticate(request.getUsername(), request.getPassword());

		StpUtil.login(request.getUsername());
		SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

		return new LoginResponse(tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
				refreshTokenService.issue(request.getUsername()), ACCESS_TOKEN_EXPIRES_IN_SECONDS);
	}

	@PostMapping("/token/refresh")
	public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {

		String username = refreshTokenService.rotate(request.refreshToken());

		StpUtil.login(username);
		SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

		return new LoginResponse(tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
				refreshTokenService.issue(username), ACCESS_TOKEN_EXPIRES_IN_SECONDS);
	}

	@PostMapping("/logout")
	public ResultHolder logout() {
		StpUtil.logout();
		return new ResultHolder("logged out");
	}

	public record RefreshRequest(@NotBlank @JsonProperty("refresh_token") String refreshToken) {
	}

	public record ResultHolder(String status) {
	}
}
