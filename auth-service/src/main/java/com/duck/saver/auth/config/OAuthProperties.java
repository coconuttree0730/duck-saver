package com.duck.saver.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth2 凭证与开关，全部经 Nacos/compose 环境注入，禁止硬编码。
 */
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

	private final GitHub github = new GitHub();
	private final Wechat wechat = new Wechat();

	public GitHub getGithub() {
		return github;
	}

	public Wechat getWechat() {
		return wechat;
	}

	public static class GitHub {

		private String clientId;

		private String clientSecret;

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}
	}

	public static class Wechat {

		private boolean enabled;

		private String clientId;

		private String clientSecret;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}
	}
}
