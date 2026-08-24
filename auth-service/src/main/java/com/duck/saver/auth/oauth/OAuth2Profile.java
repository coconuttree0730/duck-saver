package com.duck.saver.auth.oauth;

/**
 * 第三方授权后换取到的用户档案。openid 仅用于绑定匹配，禁止落日志。
 */
public record OAuth2Profile(String login, String nickname, String openid) {
}
