# Spec B1 · 可插拔 OAuth2 登录（GitHub 先行 · 微信预留）

> Issue 标题（发布时使用）：`feat: add pluggable OAuth2 login with GitHub`
> 标签：`ready-for-agent`　分支建议：`feature/phase2b1-oauth2`
> 阶段二第 2/4 个 spec（原 Spec B 拆分而来；实施顺序：004 B2-MQ → 本 spec → 005 C）

---

## Problem Statement

API-CONTRACT.md 已承诺 `POST /uaa/oauth2/github` 与 `POST /uaa/oauth2/wechat` 两个登录端点，前端登录页也画好了 GitHub 按钮，但 auth-service 没有任何 OAuth2 实现——点击即死链。数据库里为 Spec B 预留的 `oauth_binding`（provider+openid 唯一）与 `oauth_client` 两张空表迟迟没有主人。微信 OAuth2 需要企业资质与已备案域名，个人开发阶段无法申请，若按"两 provider 同时完整交付"立项会被外部依赖无限期卡死。

## Solution

在 auth-service 内实现**可插拔 OAuth2 Provider 抽象**：前端完成跳转与回调、持 code POST 给后端（沿用既有契约交互流），后端经 Provider 接口换取用户档案、查 `oauth_binding` 决定登录或自动注册。GitHub 作为首个激活的 provider 端到端打通；微信实现同一接口但由配置门控为 **disabled**（返回明确的业务错误码），不阻塞交付。同时落地契约已承诺的客户端凭证接口，并为老用户补充登录态下的账号绑定能力（先改契约再写代码）。

## User Stories

1. As a 用户, I want 点击 GitHub 登录按钮完成授权后直接进入系统, so that 我不用再记一套用户名密码
2. As a 新用户, I want 首次 GitHub 授权后自动创建账号并可立即使用, so that 不存在"先注册再绑定"的流失步骤
3. As an 已有密码账号的老用户, I want 在登录态下把我的 GitHub 身份绑定到现有账号, so that 两种方式登录的是同一个账本
4. As a 用户, I want 解除某个第三方绑定, so that 我能收回授权
5. As a 用户, I want 微信登录按钮返回"暂未开放"而不是报错白屏, so that 我知道功能边界而非怀疑系统坏了
6. As a 后端开发者, I want 新增第三方只需实现一个 Provider 接口加一份 Nacos 配置, so that 扩展不触碰登录主流程代码
7. As a 后端开发者, I want OAuth 回调携带一次性 state 且经 Redis 校验, so that 授权码注入/CSSF 风险被挡在门外
8. As a 后端开发者, I want 第三方凭证只从 Nacos 注入, so that 仓库里永远不出现 client_secret
9. As a 运维工程师, I want 通过配置开关启用/禁用任一 provider, so that 资质到位前微信保持关闭且随时可开
10. As a 前端开发者, I want OAuth 登录响应与密码登录完全同构（tokenName/tokenValue/refresh_token/expires_in）, so that 令牌管理逻辑零分叉

## Implementation Decisions

- **交互流确认（不改契约既有语义）**：前端发起 OAuth 跳转并在 callback 页拿到 code 后，`POST /uaa/oauth2/{provider}` 递交 `{code, state}`；后端换 access_token → 拉取用户档案 → 提取 openid → 查 `oauth_binding` → 命中则登录，未命中走自动注册。响应与密码登录同构（JWT + refresh_token 轮换）。
- **首次登录自动注册**：未命中绑定时自动创建 `sys_user`（无密码置空/随机占位，用户名取第三方昵称并做唯一化处理），复用密码注册路径的全部初始化逻辑（含 demo 种子数据）；随后写入 `oauth_binding` 并登录。不做"同邮箱合并询问"——个人记账应用体验优先。
- **可插拔抽象**：common 或 auth-service 内定义 `OAuth2Provider` 接口（`provider()` / `exchange(code): OAuth2Profile` / `enabled()`），每个第三方一个实现类注册为 Spring bean，按 provider 名路由。**微信完整实现同一接口但 `enabled()` 读配置默认 false**；disabled 时返回业务错误码（见契约修订），不抛 500。
- **state 校验**：前端生成随机 state，auth-service 经 Redis 做一次性校验（写入 TTL 5 分钟，消费即删）；缺失或不匹配一律 401。
- **凭证管理**：GitHub client_id/client_secret 经 Nacos 配置注入（`spring.config.import`），禁止硬编码；微信凭证位同样预留在 Nacos，值留空即可。
- **绑定/解绑（契约先行新增）**：API-CONTRACT.md 的 auth-service 小节新增 `POST /uaa/oauth2/{provider}/bind`（登录态，递交 code，绑定到当前用户）与 `DELETE /uaa/oauth2/{provider}/bind`（解绑；纯第三方账号且无密码时禁止解绑，避免变成无法登录的死号）。**先改契约、PR 中可见 diff，再实现。**
- **契约错误码修订（先行）**：全局错误码表补充认证业务段 **4xxx**：`4001` provider 未开放（disabled）、`4002` 第三方凭证无效、`4003` 该身份已绑定其他账号。
- **client 凭证接口**：落地契约已有的 `POST /uaa/account/client`——生成 client_id/client_secret（BCrypt 存储 secret）写入 `oauth_client` 表，供服务间调用场景使用；鉴权走 Sa-Token 网关既有规则。
- **安全基线**：openid 仅存 `oauth_binding`，不落日志；exchange 得到的第三方 access_token 用完即弃不持久化。

## Testing Decisions

- 只断言外部行为：响应结构与契约逐字一致、错误码语义正确；不断言内部 bean 路由细节。
- Seam 1：根 POM reactor `mvn clean verify` 全绿。
- Seam 2：Testcontainers `mysql:8` 验证 `oauth_binding` 唯一约束（同 provider+openid 重复绑定必须被拒）、解绑后的登录隔离。
- Seam 3：单测中用假 `OAuth2Provider` bean（固定返回档案）覆盖：首登自动注册 → 二次登录命中绑定 → 绑定到已有账号 → 解绑 → disabled provider 返回 4001；GitHub 真实 HTTP 用 WireMock 模拟（token exchange + profile 拉取），不依赖外网。
- 冒烟套件扩展：`-Dsmoke` 下用 stub provider 走通"OAuth 登录拿 JWT → 访问受保护资源"，确保网关鉴权链路对第三方会话一视同仁。

## Out of Scope

- 微信真实环境联调与资质申请（enabled 开关打开之日另行验证）
- 前端仓库任何改动（回调页/按钮接线由前端侧任务承接）
- 其他第三方平台（Google 等）
- MQ、缓存、可观测性（归 004 / 005）

## Further Notes

- **人工前置（今天就可做）**：在 GitHub 后台创建 OAuth App（callback 指向前端回调路由），把 client_id/client_secret 录入本地 Nacos——约 5 分钟，不阻塞 B2 先行开发。
- 分支 `feature/phase2b1-oauth2`，PR 目标 develop；契约 diff 必须出现在同一 PR 中供对照审查。
- EXECUTION-PLAN 对应任务：5-02（GitHub）、5-03（微信，降级为实现+disabled）。
- 完成判定：GitHub 端到端可登录；微信入口返回 4001；swagger 与契约逐字一致；冒烟全绿。
