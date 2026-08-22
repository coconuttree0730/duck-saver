package com.duck.saver.auth.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.duck.saver.auth.dto.LoginRequest;
import com.duck.saver.auth.dto.LoginResponse;
import com.duck.saver.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AuthController {

	@Autowired
	private UserService userService;

	@PostMapping
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {

		userService.authenticate(request.getUsername(), request.getPassword());

		StpUtil.login(request.getUsername());

		SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

		return new LoginResponse(tokenInfo.getTokenName(), tokenInfo.getTokenValue());
	}
}
