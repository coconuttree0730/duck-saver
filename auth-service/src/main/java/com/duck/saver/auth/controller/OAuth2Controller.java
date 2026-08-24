package com.duck.saver.auth.controller;

import com.duck.saver.auth.dto.LoginResponse;
import com.duck.saver.auth.service.OAuth2LoginService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * OAuth2 登录/绑定端点（契约：POST /uaa/oauth2/{provider} 等）。
 * 登录态端点经网关 X-User-Name 还原当前用户（Principal 注入）。
 */
@RestController
public class OAuth2Controller {

	@Autowired
	private OAuth2LoginService loginService;

	@PostMapping("/oauth2/{provider}/state")
	public Map<String, String> issueState(@PathVariable String provider) {
		return Map.of("state", loginService.issueState(provider));
	}

	@PostMapping("/oauth2/{provider}")
	public LoginResponse login(@PathVariable String provider, @Valid @RequestBody CodeRequest request) {
		return loginService.login(provider, request.code(), request.state());
	}

	@PostMapping("/oauth2/{provider}/bind")
	public void bind(Principal principal, @PathVariable String provider,
			@Valid @RequestBody BindRequest request) {
		loginService.bind(principal.getName(), provider, request.code());
	}

	@DeleteMapping("/oauth2/{provider}/bind")
	public void unbind(Principal principal, @PathVariable String provider) {
		loginService.unbind(principal.getName(), provider);
	}

	public record CodeRequest(@NotBlank @JsonProperty("code") String code,
			@JsonProperty("state") String state) {
	}

	public record BindRequest(@NotBlank @JsonProperty("code") String code) {
	}
}
