package com.duck.saver.auth.oauth;

import com.duck.saver.auth.config.OAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * GitHub OAuth2 实现（POST-code 流）：code → access_token → /user 档案。
 * 凭证经 Nacos 注入；未配置 client_id 时视为未启用。
 */
@Component
public class GitHubProvider implements OAuth2Provider {

	private static final Logger log = LoggerFactory.getLogger(GitHubProvider.class);

	@Autowired
	private OAuthProperties properties;

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public String provider() {
		return "github";
	}

	@Override
	public boolean enabled() {
		String clientId = properties.getGithub().getClientId();
		return clientId != null && !clientId.isBlank()
				&& properties.getGithub().getClientSecret() != null
				&& !properties.getGithub().getClientSecret().isBlank();
	}

	@Override
	public OAuth2Profile exchange(String code) {
		String accessToken = exchangeToken(code);
		return fetchProfile(accessToken);
	}

	private String exchangeToken(String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", properties.getGithub().getClientId());
		form.add("client_secret", properties.getGithub().getClientSecret());
		form.add("code", code);

		ResponseEntity<Map> response = restTemplate.postForEntity(
				"https://github.com/login/oauth/access_token", new HttpEntity<>(form, headers), Map.class);
		Map body = response.getBody();
		if (body == null || body.get("access_token") == null) {
			log.warn("github token exchange failed: {}", body == null ? "empty" : body.get("error"));
			throw new IllegalStateException("github token exchange failed");
		}
		return (String) body.get("access_token");
	}

	private OAuth2Profile fetchProfile(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.set("Accept", "application/vnd.github+json");

		ResponseEntity<Map> response = restTemplate.postForEntity(
				"https://api.github.com/user", new HttpEntity<>(headers), Map.class);
		Map body = response.getBody();
		if (body == null || body.get("id") == null) {
			throw new IllegalStateException("github profile fetch failed");
		}
		String openid = String.valueOf(body.get("id"));
		String login = (String) body.getOrDefault("login", "gh-" + openid);
		String nickname = (String) body.getOrDefault("name", login);
		return new OAuth2Profile(login, nickname, openid);
	}
}
