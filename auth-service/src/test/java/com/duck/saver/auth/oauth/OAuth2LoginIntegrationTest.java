package com.duck.saver.auth.oauth;

import com.duck.saver.auth.entity.OauthBindingEntity;
import com.duck.saver.auth.mapper.OauthBindingMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OAuth2 登录主流程外部行为（Spec 003 Seam 3）：假 provider 覆盖首登自动注册、
 * 二次登录命中绑定、绑定冲突 4003、解绑保护 4004 与唯一约束。
 */
@SpringBootTest(properties = "sa-token.jwt-secret-key=test-secret")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OAuth2LoginIntegrationTest {

	/** 测试可变的假档案：openid 按用例切换。 */
	static class FakeProfileHolder {

		String openid = "fake-open-1";
	}

	@TestConfiguration
	static class FakeProviderConfig {

		@Bean
		FakeProfileHolder fakeProfileHolder() {
			return new FakeProfileHolder();
		}

		@Bean
		OAuth2Provider fakeProvider(FakeProfileHolder holder) {
			return new OAuth2Provider() {
				@Override
				public String provider() {
					return "fake";
				}

				@Override
				public boolean enabled() {
					return true;
				}

				@Override
				public OAuth2Profile exchange(String code) {
					return new OAuth2Profile("faker-" + holder.openid, "Faker", holder.openid);
				}
			};
		}
	}

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("duck_saver_auth")
			.withInitScript("sql/auth_schema.sql");

	@Container
	@ServiceConnection
	static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private FakeProfileHolder holder;

	@Autowired
	private OauthBindingMapper bindingMapper;

	@Autowired
	private com.duck.saver.auth.mapper.UserMapper userMapper;

	private String freshState() throws Exception {
		MvcResult result = mockMvc.perform(post("/oauth2/fake/state")).andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.path("data").path("state").asText();
	}

	private String login(String code) throws Exception {
		MvcResult result = mockMvc.perform(post("/oauth2/fake")
						.contentType(APPLICATION_JSON)
						.content("{\"code\":\"" + code + "\",\"state\":\"" + freshState() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tokenValue").isNotEmpty())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
				.get("data").get("tokenValue").asText();
	}

	/** MockMvc 无网关头，直接以“该 openid 当前绑定的用户名”作为登录身份断言依据。 */
	private String usernameForOpenid(String openid) {
		OauthBindingEntity binding = bindingMapper.selectOne(
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OauthBindingEntity>()
						.eq(OauthBindingEntity::getProvider, "fake")
						.eq(OauthBindingEntity::getOpenid, openid));
		org.junit.jupiter.api.Assertions.assertNotNull(binding, "binding should exist");
		return userMapper.selectById(binding.getUserId()).getUsername();
	}

	@Test
	public void disabledProviderReturns4001() throws Exception {
		mockMvc.perform(post("/oauth2/wechat")
						.contentType(APPLICATION_JSON)
						.content("{\"code\":\"c\",\"state\":\"" + freshState() + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(4001));
	}

	@Test
	public void invalidStateReturns401() throws Exception {
		mockMvc.perform(post("/oauth2/fake")
						.contentType(APPLICATION_JSON)
						.content("{\"code\":\"c\",\"state\":\"bogus\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void firstLoginAutoRegistersSecondLoginReusesBinding() throws Exception {
		holder.openid = "open-1";

		String token1 = login("code-a");
		String username1 = usernameForOpenid("open-1");
		org.junit.jupiter.api.Assertions.assertNotNull(username1);

		login("code-b");
		assertEquals(username1, usernameForOpenid("open-1"));

		// oauth-only（密码为空）账号禁止解绑唯一登录方式
		mockMvc.perform(delete("/oauth2/fake/bind")
						.header("satoken", token1)
						.header("X-User-Name", username1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(4004));
	}

	@Test
	public void bindConflictReturns4003() throws Exception {
		// 用户 A 登录并持有 open-2
		holder.openid = "open-2";
		login("code-c");

		// 用户 B（新 openid）登录后尝试把 open-2 绑到自己 → 4003
		holder.openid = "open-3";
		login("code-d");
		String userB = usernameForOpenid("open-3");
		holder.openid = "open-2"; // 绑定已被 A 持有的 openid
		mockMvc.perform(post("/oauth2/fake/bind")
						.header("satoken", "dummy-token")
						.header("X-User-Name", userB)
						.contentType(APPLICATION_JSON)
						.content("{\"code\":\"code-e\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(4003));
	}

	@Test
	public void duplicateBindingRejectedByUniqueConstraint() {
		OauthBindingEntity first = binding("dup-open");
		bindingMapper.insert(first);
		assertThrows(Exception.class, () -> bindingMapper.insert(binding("dup-open")));
	}

	private OauthBindingEntity binding(String openid) {
		OauthBindingEntity entity = new OauthBindingEntity();
		entity.setUserId(9999L); // 独立用户，避免影响其他用例的绑定计数
		entity.setProvider("github");
		entity.setOpenid(openid);
		return entity;
	}
}
