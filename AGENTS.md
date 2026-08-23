# AGENTS.md — duck-saver-backend（后端）

> 本文档描述**目标架构**。仓库中部分目录是 PiggyMetrics 迁移遗留（见下），与目标状态不符；写代码一律以本文档为准。

## 工作流程

1. 动手前读 [../CONTEXT.md](../CONTEXT.md)：领域术语以它为准（收支记录叫 Transaction，数据库表名 `transaction`）。
2. 实现或修改对外接口前，读 [../API-CONTRACT.md](../API-CONTRACT.md) 对应小节；改接口先改契约，再改代码。
3. 提 Issue、建分支、写 Commit 前，读 [../SHARED-AGENT-RULES.md](../SHARED-AGENT-RULES.md)；scope 取该文件「后端」一栏。
4. 搭建或排查本地环境（容器、端口、MySQL 初始化）时，读 [docs/dev-environment.md](docs/dev-environment.md)。
5. 完成判定：改动服务编译通过；接口行为与契约一致，`/swagger-ui.html` 可访问；满足文末「非功能需求」。

## 遗留目录（迁移参考，勿作范本）

`config/`、`registry/`、`monitoring/`、`turbine-stream-service/`、`mongodb/` 是 PiggyMetrics 遗留物（Java 8 + Eureka + MongoDB 技术栈）。所有新代码只写在五个业务服务和 gateway 中。

## 技术栈（目标版本）

- Java 17 · Spring Boot 3.2.x · Spring Cloud 2023.0.x
- 注册/配置 Nacos 2.3.x · 网关 Spring Cloud Gateway · 熔断限流 Sentinel 1.8.x
- 认证 Sa-Token 1.44.x（sa-token-jwt Mixin + 自实现 refresh_token 轮换）· ORM MyBatis Plus 3.5.x · MySQL 8
- 缓存 Caffeine 3（L1）+ Redis 7（L2）· 分布式锁 Redisson
- 消息 RabbitMQ 3（含 delayed_message_exchange 延迟消息插件）
- 可观测 SkyWalking 9 · Prometheus · Grafana · Loki/Promtail · 文档 springdoc-openapi 2.x
- AI 通义千问 API

---

## 包结构与命名（每个服务）

```
com.duck.saver.{service}
├── controller/  service/(impl)/  mapper/  entity/
├── dto/  config/  constant/  util/
```

| 类型 | 命名 | 示例 |
|------|------|------|
| Controller | `{Entity}Controller` | `AccountController` |
| Service | `{Entity}Service` / `{Entity}ServiceImpl` | `AccountService` |
| Mapper | `{Entity}Mapper` | `AccountMapper` |
| Entity | `{Entity}`（`@TableName`） | `Account` |
| DTO | `{Action}Request` / `{Action}Response` | `CreateAccountRequest` |

RESTful 约定：GET 查询、POST 创建、PUT 更新、DELETE 删除；方法名 `getXxx/createXxx/updateXxx/deleteXxx`；返回统一响应体包装。Sentinel fallback 类命名 `{Service}Fallback`。

## 网关路由（服务端口唯一来源）

```
/uaa/**           → auth-service:5000
/accounts/**      → account-service:6000
/statistics/**    → statistics-service:7000
/notifications/** → notification-service:8000
/ai/**            → ai-service:9000
```

gateway 本身 :4000。Nginx 将上述 API 路径转发 gateway:4000，其余路径转发 frontend:3000。

## 数据库 Schema

1 个 MySQL 实例、4 个 Schema；每服务独立账号，仅访问自己的 Schema：

```
duck_saver_auth          sys_user · oauth_binding · oauth_client
duck_saver_account       account(version 乐观锁) · transaction · saving
duck_saver_statistics    data_point
duck_saver_notification  recipient · notification_config
```

字段明细见根目录 API-CONTRACT.md 的请求/响应结构；建库建用户脚本见 docs/dev-environment.md。

---

## MyBatis Plus 约定

- Entity 用 `@TableName`/`@TableField` 映射，继承 `BaseMapper<T>` 获得基础 CRUD
- 乐观锁：account 表 `version` 字段 + `@Version`，冲突重试至多 3 次
- 逻辑删除用 `@TableLogic`，分页用内置插件，按需启用

## Sa-Token 鉴权

- 过滤器挂在 Gateway：放行 `/uaa/login` 与 `/uaa/oauth2/**`，其余 `/accounts` `/statistics` `/notifications` `/ai` 要求登录态；另有两类放行：开放注册 `POST /uaa/users`（仅此方法）与各服务 Swagger/API 文档路径（开发期查契约用）
- 登录后 Gateway 以 `X-User-Name` 头向下游透传用户名，下游服务据此还原 Principal；该头仅信任来自内网的请求，服务端口一律只绑回环（见 docker-compose.yml），不得对公网暴露
- 服务内角色校验用 `@SaCheckRole`（替代 `@PreAuthorize`）
- GitHub / 微信 OAuth2 客户端凭证经 Nacos 配置注入，禁止硬编码
- Sentinel 限流规则经 Nacos 持久化（group `SENTINEL_GROUP`，dataId `{service}-flow-rules` / `{service}-system-rules`），种子文件见 docs/nacos-seed/，导入步骤见 docs/dev-environment.md

## 多级缓存与分布式锁

| 层 | 存储 | TTL | 场景 |
|----|------|-----|------|
| L1 | Caffeine（JVM） | AI 分类 24h · 汇率 1h | 输入重复率高且变更频率低才启用 |
| L2 | Redis | 账户 5min · 统计 1min | 分布式共享 |

统一走 Spring Cache `@Cacheable` 抽象。分布式锁用 Redisson，粒度为 account 级，配合乐观锁重试。

## MQ 最终一致性

拓扑：Exchange `account.event.exchange`（fanout）→ `account.event.queue`、`statistics.event.queue`；死信 `*.dlx`。

消息体必含：`eventId`(uuid)、`eventType`、`timestamp`、`data`。
eventType ∈ `ACCOUNT_CREATED` | `ACCOUNT_UPDATED` | `ACCOUNT_DELETED` | `ITEM_ADDED` | `ITEM_DELETED`

消费规则：按 `eventId` 幂等去重 → 处理 → 落库记录；失败重试 3 次进死信；生产/消费双方各配定时兜底任务。

通知类定时触发（预算提醒、月末洞察报告）用延迟消息精确触发，替代 `@Scheduled` 全表扫描。

## 配置中心与限流

- Nacos：server-addr `nacos-server:8848`；Spring Boot 3.x 用 `spring.config.import` 引入远端配置；控制台修改自动热更新
- Sentinel QPS 上限：`/accounts/**` 100 · `/statistics/**` 50 · `/notifications/**` 30；全局线程 500

## 可观测性

| 能力 | 方案 |
|------|------|
| 链路追踪 | SkyWalking JVM Agent 零侵入接入，配置服务名与采样率 |
| 指标 | Actuator + Micrometer → Prometheus → Grafana；自定义业务指标；Grafana Alerting 多渠道告警 |
| 日志 | Promtail 采集 → Loki 存储，Grafana 统一面板 |

---

## 非功能需求（验收标准）

| 指标 | 目标值 |
|------|--------|
| API 平均响应 | < 200ms |
| API P99 响应 | < 1s |
| 网关吞吐 | ≥ 1000 QPS |
| 服务可用性 | ≥ 99.9% |
| 故障恢复 | < 5min |
| 密码存储 | BCrypt |
| 传输加密 | HTTPS / TLS 1.3 |
