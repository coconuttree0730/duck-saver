package com.duck.saver.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.auth.entity.OauthClientEntity;
import com.duck.saver.auth.mapper.OauthClientMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 客户端凭证接口（契约既有承诺）：生成 client_id/client_secret 一次明文返回，
 * secret 以 BCrypt 落库。
 */
@RestController
public class OauthClientController {

	private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

	@Autowired
	private OauthClientMapper clientMapper;

	@PostMapping("/account/client")
	@Transactional
	public Map<String, String> createClient(@Valid @RequestBody CreateClientRequest request) {
		String clientId = "ds-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
		String clientSecret = UUID.randomUUID().toString().replace("-", "") + "-"
				+ UUID.randomUUID().toString().replace("-", "");

		OauthClientEntity entity = new OauthClientEntity();
		entity.setClientId(clientId);
		entity.setClientSecret(ENCODER.encode(clientSecret));
		entity.setName(request.name());
		if (clientMapper.selectCount(new LambdaQueryWrapper<OauthClientEntity>()
				.eq(OauthClientEntity::getClientId, clientId)) > 0) {
			throw new IllegalStateException("client id collision, retry");
		}
		clientMapper.insert(entity);

		return Map.of("client_id", clientId, "client_secret", clientSecret);
	}

	public record CreateClientRequest(@NotBlank @JsonProperty("name") String name) {
	}
}
