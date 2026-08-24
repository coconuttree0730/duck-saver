package com.duck.saver.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duck.saver.auth.config.OAuthProperties;
import com.duck.saver.auth.dto.LoginResponse;
import com.duck.saver.auth.entity.OauthBindingEntity;
import com.duck.saver.auth.entity.UserEntity;
import com.duck.saver.auth.mapper.OauthBindingMapper;
import com.duck.saver.auth.mapper.UserMapper;
import com.duck.saver.auth.oauth.OAuth2Profile;
import com.duck.saver.auth.oauth.OAuth2Provider;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.duck.saver.common.api.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OAuth2 登录主流程：state 一次性校验 → provider 路由 → 绑定匹配或自动注册。
 * 错误码：4001 provider 未开放 · 4002 第三方凭证无效 · 4003 该身份已绑定其他账号 ·
 * 4004 纯第三方账号（无密码）禁止解绑唯一登录方式。openid 不落日志。
 */
@Service
public class OAuth2LoginService {

	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginService.class);

	private static final String STATE_KEY_PREFIX = "oauth:state:";
	private static final long ACCESS_TOKEN_EXPIRES_IN_SECONDS = 7 * 24 * 3600L;

	@Autowired
	private List<OAuth2Provider> providers;

	@Autowired
	private OauthBindingMapper bindingMapper;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private Map<String, OAuth2Provider> byName() {
		return providers.stream()
				.collect(Collectors.toMap(p -> p.provider().toLowerCase(Locale.ROOT), Function.identity()));
	}

	/** 签发一次性 state（TTL 5 分钟，消费即删）。 */
	public String issueState(String providerName) {
		requireProvider(providerName);
		String state = UUID.randomUUID().toString().replace("-", "");
		redisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, "1", Duration.ofMinutes(5));
		return state;
	}

	public LoginResponse login(String providerName, String code, String state) {
		OAuth2Provider provider = requireEnabled(providerName);
		consumeState(state);

		OAuth2Profile profile = exchange(provider, code);
		OauthBindingEntity binding = findBinding(providerName, profile.openid());

		String username;
		if (binding != null) {
			UserEntity user = userMapper.selectById(binding.getUserId());
			if (user == null) {
				throw new BusinessException(4002, "bound account missing");
			}
			username = user.getUsername();
			log.info("oauth login via existing binding: {} -> {}", providerName, username);
		} else {
			username = autoRegister(profile);
			createBinding(userMapper.selectOne(
					new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username)).getId(),
					providerName, profile.openid());
			log.info("oauth first-login auto-registered: {} -> {}", providerName, username);
		}

		return buildSession(username);
	}

	@Transactional
	public void bind(String username, String providerName, String code) {
		OAuth2Provider provider = requireEnabled(providerName);
		OAuth2Profile profile = exchange(provider, code);

		OauthBindingEntity existing = findBinding(providerName, profile.openid());
		if (existing != null) {
			if (usernameOf(existing.getUserId()).equals(username)) {
				return; // 已绑定当前账号，幂等成功
			}
			throw new BusinessException(4003, "identity already bound to another account");
		}
		UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
				.eq(UserEntity::getUsername, username));
		createBinding(user.getId(), providerName, profile.openid());
		log.info("oauth identity bound: {} -> {}", providerName, username);
	}

	@Transactional
	public void unbind(String username, String providerName) {
		UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
				.eq(UserEntity::getUsername, username));
		OauthBindingEntity binding = bindingMapper.selectOne(new LambdaQueryWrapper<OauthBindingEntity>()
				.eq(OauthBindingEntity::getUserId, user.getId())
				.eq(OauthBindingEntity::getProvider, providerName.toLowerCase(Locale.ROOT)));
		if (binding == null) {
			return; // 本就未绑定，幂等成功
		}
		boolean hasPassword = user.getPassword() != null && !user.getPassword().isBlank();
		long bindings = bindingMapper.selectCount(new LambdaQueryWrapper<OauthBindingEntity>()
				.eq(OauthBindingEntity::getUserId, user.getId()));
		if (!hasPassword && bindings <= 1) {
			throw new BusinessException(4004,
					"cannot unbind the only login method of an oauth-only account");
		}
		bindingMapper.deleteById(binding.getId());
		log.info("oauth identity unbound: {} -X {}", username, providerName);
	}

	private OAuth2Provider requireEnabled(String providerName) {
		OAuth2Provider provider = requireProvider(providerName);
		if (!provider.enabled()) {
			throw new BusinessException(4001, "provider not available: " + providerName);
		}
		return provider;
	}

	private OAuth2Provider requireProvider(String providerName) {
		OAuth2Provider provider = byName().get(providerName == null ? "" : providerName.toLowerCase(Locale.ROOT));
		if (provider == null) {
			throw new BusinessException(4001, "unknown provider: " + providerName);
		}
		return provider;
	}

	private void consumeState(String state) {
		if (state == null || Boolean.FALSE.equals(redisTemplate.hasKey(STATE_KEY_PREFIX + state))) {
			throw new com.duck.saver.common.api.UnauthorizedException("invalid oauth state");
		}
		redisTemplate.delete(STATE_KEY_PREFIX + state);
	}

	private OAuth2Profile exchange(OAuth2Provider provider, String code) {
		try {
			return provider.exchange(code);
		} catch (Exception e) {
			log.warn("oauth exchange failed for {}: {}", provider.provider(), e.getMessage());
			throw new BusinessException(4002, "third-party credential invalid");
		}
	}

	private OauthBindingEntity findBinding(String providerName, String openid) {
		return bindingMapper.selectOne(new LambdaQueryWrapper<OauthBindingEntity>()
				.eq(OauthBindingEntity::getProvider, providerName.toLowerCase(Locale.ROOT))
				.eq(OauthBindingEntity::getOpenid, openid));
	}

	private String usernameOf(Long userId) {
		return userMapper.selectById(userId).getUsername();
	}

	/** 首登自动注册：昵称做用户名唯一化，密码置空（纯第三方账号）。 */
	private String autoRegister(OAuth2Profile profile) {
		String base = (profile.login() == null ? "user" : profile.login())
				.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase(Locale.ROOT);
		if (base.isBlank()) {
			base = "user";
		}
		String username = base;
		while (userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
				.eq(UserEntity::getUsername, username)) > 0) {
			username = base + "-" + UUID.randomUUID().toString().substring(0, 6);
		}

		UserEntity entity = new UserEntity();
		entity.setUsername(username);
		entity.setPassword("");
		userMapper.insert(entity);
		log.info("auto-registered oauth user: {}", username);
		return username;
	}

	private void createBinding(Long userId, String providerName, String openid) {
		OauthBindingEntity binding = new OauthBindingEntity();
		binding.setUserId(userId);
		binding.setProvider(providerName.toLowerCase(Locale.ROOT));
		binding.setOpenid(openid);
		bindingMapper.insert(binding);
	}

	private LoginResponse buildSession(String username) {
		StpUtil.login(username);
		SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
		return new LoginResponse(tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
				refreshTokenService.issue(username), ACCESS_TOKEN_EXPIRES_IN_SECONDS);
	}
}
