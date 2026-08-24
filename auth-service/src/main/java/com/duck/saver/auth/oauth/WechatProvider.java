package com.duck.saver.auth.oauth;

import com.duck.saver.auth.config.OAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 微信 OAuth2：实现同一 Provider 接口但默认 disabled（等待企业资质与备案域名）。
 * enabled 打开前调用方在路由层即被拦截返回 4001，不会走到 exchange。
 */
@Component
public class WechatProvider implements OAuth2Provider {

	@Autowired
	private OAuthProperties properties;

	@Override
	public String provider() {
		return "wechat";
	}

	@Override
	public boolean enabled() {
		return properties.getWechat().isEnabled();
	}

	@Override
	public OAuth2Profile exchange(String code) {
		throw new UnsupportedOperationException("wechat oauth is not available yet");
	}
}
