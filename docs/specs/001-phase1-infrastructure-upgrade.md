# Spec：阶段一 · 后端基础设施升级框架

> Issue 标题（发布时使用）：`feat: upgrade backend infrastructure to Java 17 + Spring Boot 3.2 stack`
> 标签：`ready-for-agent`　分支建议：`feature/phase1-infrastructure`

---

## Problem Statement

后端目前跑在 PiggyMetrics 遗留技术栈上（Java 8 · Spring Boot 2.0.3 · Spring Cloud Finchley），核心组件 Eureka、Zuul、Hystrix 均已停止维护或进入维护模式，拿不到安全补丁，也无法与目标技术栈（Spring Boot 3.x、Sa-Token、通义千问 SDK）集成。开发者在这个底座上无法继续推进数据层迁移、AI 能力等后续阶段的工作；接口没有限流保护，服务没有统一的可观测性基线，一个慢调用就可能拖垮整条链路。

## Solution

对五个业务服务做**原地升级**（业务逻辑与 API 契约不变），把框架底座整体替换为 Java 17 + Spring Boot 3.2.x + Spring Cloud 2023.0.x（Alibaba）：

- **Nacos** 替代 Eureka + Spring Cloud Config（注册发现 + 配置中心二合一）
- **Spring Cloud Gateway** 重写替代 Zuul，只做 API 路由
- **Sentinel** 替代 Hystrix，提供 QPS 限流与熔断降级
- **Sa-Token** 网关鉴权先行接入（密码登录可用，第三方 OAuth2 留待后续阶段）
- Redis、RabbitMQ（含延迟消息插件）、Swagger 文档、Prometheus + Grafana 就位
- 删除四个纯基础设施遗留模块，Nginx 承担静态资源与反向代理

升级完成后：六个服务在 Nacos 注册、经 Gateway 五条路由可达、每个服务 Swagger UI 可访问、限流规则在 Dashboard 可见——为下一阶段数据层迁移和 AI 能力打好地基。

## User Stories

1. As a 后端开发者, I want 在 JDK 17 上一条命令完成全仓库构建（reactor `mvn clean verify`），so that 升级后的代码有可靠的编译门禁，不再依赖 Java 8
2. As a 后端开发者, I want 五个业务服务原地迁移到 Spring Boot 3.2.x 而不改动业务逻辑，so that 账户、交易记录（Transaction）、统计、通知的行为与升级前完全一致
3. As a 后端开发者, I want 服务通过 Nacos 注册发现，so that 我不用再维护独立的 Eureka 注册中心服务
4. As a 后端开发者, I want 配置从 Nacos 配置中心经 `spring.config.import` 注入并支持控制台热更新，so that 改配置不需要重新打包或重启所有服务
5. As a 后端开发者, I want OAuth 客户端凭证等敏感配置统一走 Nacos 注入，so that 代码里没有任何硬编码密钥
6. As a 后端开发者, I want `javax.*` 依赖全部替换为 `jakarta.*` 并清理不兼容的旧依赖，so that 代码符合 Boot 3.x 规范且无编译警告堆积
7. As a 后端开发者, I want Gateway 上挂 Sa-Token 过滤器放行 `/uaa/login` 与 `/uaa/oauth2/**`、拦截其余路径，so that 未登录请求进不了业务服务
8. As a 用户, I want 能用密码登录拿到会话凭证后访问自己的账户数据，so that 升级期间基本的认证链路始终可用
9. As a 后端开发者, I want 密码继续以 BCrypt 存储，so that 认证方式切换不降低安全水位
10. As a 后端开发者, I want Hystrix 的 `@HystrixCommand` 全部换成 `@SentinelResource` 且 Fallback 类命名为 `{Service}Fallback`，so that 降级逻辑在新技术栈下语义不变
11. As a 运维工程师, I want 在 Sentinel Dashboard（:8858）看到限流规则并调整 QPS（accounts 100 · statistics 50 · notifications 30 · 全局线程 500），so that 流量治理可视化、规则经 Nacos 持久化不丢失
12. As a 后端开发者, I want 下游服务故障时请求被熔断并走 Fallback 而不是超时堆积，so that 单个服务抖动不会雪崩整个系统
13. As a 前端开发者, I want 经 Gateway 访问五条路由 `/uaa/**` `/accounts/**` `/statistics/**` `/notifications/**` `/ai/**`，so that 前端只需要记住一个入口和一套端口约定
14. As a 前端开发者, I want 每个服务的 `/swagger-ui.html` 经网关可访问，so that 我能随时查到最新的接口契约实现进度
15. As a 后端开发者, I want 所有接口返回统一响应体包装并有全局异常处理，so that 前端可以用同一套解析逻辑处理成功与失败
16. As a 后端开发者, I want 新增 ai-service 骨架模块（注册 + 路由 + Swagger 可达），so that 阶段三接入 AI 分类/查询/洞察时框架已就绪，Gateway 路由表完整
17. As a 后端开发者, I want 各服务接入 springdoc-openapi 2.x，so that 接口文档随代码自动生成而非手工维护
18. As a 运维工程师, I want Redis 与 RabbitMQ（含 delayed_message_exchange 插件）容器就绪且服务连通验证通过，so that 下一阶段的缓存、分布式锁与最终一致性开箱即用
19. As a 运维工程师, I want Actuator + Micrometer 指标被 Prometheus 抓取并在 Grafana 出图，so that 替换掉过时的 Hystrix Dashboard + Turbine 监控
20. As a 运维工程师, I want 一条 `docker compose up -d` 拉起全部基础设施（MongoDB·Redis·RabbitMQ·Nacos·Nginx·Sentinel Dashboard·Prometheus·Grafana），so that 新环境搭建不超过十分钟
21. As a 前端开发者, I want Nginx 把 `/` 转发前端、API 路径转发 Gateway :4000，so that 浏览器侧没有跨域问题，静态资源不再由网关托管
22. As a 项目负责人, I want `config/` `registry/` `monitoring/` `turbine-stream-service/` 四个纯基础设施模块从仓库删除，so that 仓库里不再有会误导新人的死代码
23. As a 新加入的 agent 或贡献者, I want 包结构统一为 `com.duck.saver.{service}` 并遵循 AGENTS.md 的命名约定，so that 我在任何服务里都能预测到代码的位置
24. As a 后端开发者, I want demo 账户相关接口（`/accounts/demo`、`/statistics/demo`）行为保持不变，so that 升级前后可以对照验证业务等价性
25. As a 项目负责人, I want 本阶段不引入任何业务功能变更，so that 升级风险被隔离在框架层，出问题容易定位回滚

## Implementation Decisions

- **就地升级，非重写**：auth-service、account-service、statistics-service、notification-service 在现有源码上迁移（改 POM、`javax`→`jakarta`、换客户端依赖）；gateway 因 Zuul 与 Spring Cloud Gateway 编程模型完全不同而重写。业务逻辑与 API-CONTRACT.md 契约不变。
- **版本矩阵**：Java 17 · Spring Boot 3.2.x · Spring Cloud 2023.0.x · Spring Cloud Alibaba 2023.0.x · Nacos 2.3.x · Sentinel 1.8.x · Sa-Token 1.37.x · springdoc-openapi 2.x。
- **根 POM 与包名**：父工程改为 `com.duck.saver`，各服务包名 `com.piggymetrics.{service}` → `com.duck.saver.{service}`，遵循 AGENTS.md 的分层（controller/service/mapper…本期 mapper 层暂不出现，数据层未迁移）。
- **新增两个 Maven 模块**：`ai-service` 骨架（启动类 + Nacos 注册 + Swagger + 健康检查，无业务实现）；共享模块 `common`（统一响应体包装 + 全局异常处理），避免六处复制粘贴。
- **数据层不动**：本期仍用 MongoDB（spring-data-mongodb 升级到与 Boot 3.2 兼容的版本）；MySQL + MyBatis Plus 是下一阶段的事，`mongodb/` 目录随之保留到数据迁移完成后删除。
- **Nacos 替代 Eureka + Config（ADR 0001）**：移除 bootstrap.yml，改用 `spring.config.import: nacos:`；移除 spring-cloud-starter-bus-amqp；server-addr `nacos-server:8848`。
- **Gateway 重写（ADR 0002）**：仅 API 路由不含静态资源，路由表以 AGENTS.md 为唯一来源（/uaa→5000 · /accounts→6000 · /statistics→7000 · /notifications→8000 · /ai→9000，gateway 自身 :4000）。
- **认证最小可用（ADR 0005）**：本阶段 Sa-Token 只做两件事——auth-service 提供 BCrypt 密码登录接口签发会话、Gateway 过滤器做登录态校验；GitHub/微信 OAuth2 登录与 `@SaCheckRole` 角色校验留待后续阶段。旧的 spring-security-oauth2 依赖及 `CustomUserInfoTokenServices` 一并移除。
- **Sentinel 替代 Hystrix（ADR 0003）**：QPS 规则经 Nacos 持久化；Fallback 类命名 `{Service}Fallback`；Dashboard 以容器运行于 ：8858。
- **基础设施容器**：docker-compose 增加 nacos-server、sentinel-dashboard、redis、rabbitmq（含延迟消息插件）、prometheus、grafana、nginx；保留 mongodb。本期只验证连通性，不做缓存/MQ 的业务用法。
- **可观测基线**：Actuator + Micrometer → Prometheus 抓取 → Grafana 面板；SkyWalking 与 Loki/Promtail 属于阶段二，不在本期。
- **遗留模块删除**：`config/`、`registry/`、`monitoring/`、`turbine-stream-service/` 从仓库删除（均为零业务代码的纯基础设施服务器，git 历史可找回）；根 POM `<modules>` 收敛为 common + gateway + 五个业务服务。
- **CI**：现有 `.travis.yml` 构建环境升到 JDK 17 保证门禁可用；CI 平台整体迁移不在本 spec 范围。

## Testing Decisions

- **好测试只断言外部可见行为**：HTTP 状态码、路由可达性、Nacos 里能看到注册实例、Swagger 页面 200、无凭证 401 / 有凭证 200；不断言内部类结构或实现细节。
- **Seam 一（已有）：构建门禁** —— 根 POM reactor `mvn clean verify` 在 JDK 17 全绿，是每次提交的最低标准。
- **Seam 二（唯一新增）：Gateway 黑盒冒烟套件** —— 一个独立集成测试模块，针对 docker-compose 拉起的基础设施 + 六个服务，从 Gateway :4000 入口断言：五条路由全部转发可达、各服务 `/swagger-ui.html` 可访问、`/uaa/login` 放行而受保护路径无凭证返回 401、登录后携带凭证可访问、Sentinel 规则触发时返回降级响应。这是本期唯一的黑盒验收面。
- **辅助低成本测试**：每个服务保留一个上下文加载冒烟测试（`@SpringBootTest` 能起 context 即通过），防止配置漂移导致服务起不来。
- **既有测试的取舍**：纯逻辑单测（如 statistics 的 ServiceImpl 测试模式）随包名迁移保留，作为先例参考；绑定 Eureka/OAuth2/Hystrix 的旧测试随依赖一起删除，由上述冒烟套件接管。

## Out of Scope

- MySQL + MyBatis Plus 数据层迁移、DDL 设计、mongodump→SQL 数据迁移脚本（阶段二 Week 4）
- Sa-Token 完整能力：GitHub/微信 OAuth2 登录、`@SaCheckRole` 角色、会话集群方案（阶段二/三）
- RabbitMQ 业务化使用：账户事件最终一致性、幂等消费、死信队列、延迟消息触发通知（阶段二 Week 5–6）；本期仅装插件验连通
- Redis 缓存策略（@Cacheable TTL）与 Redisson 分布式锁（阶段二 Week 6）
- SkyWalking 链路追踪、Loki/Promtail 日志聚合（阶段二 Week 7）
- 全部 AI 能力（AI 分类/AI Query/支出洞察/预算预警，阶段三）
- 前端仓库的一切改动（Nginx 转发规则属本仓库，页面本身不属于）
- Seata 分布式事务、性能压测调优（上线准备阶段）

## Further Notes

- 分支与提交规范遵循 SHARED-AGENT-RULES.md：分支 `feature/phase1-infrastructure`，提交 `<type>(<scope>): <description>`，scope 取后端一栏。
- 阶段集成检查点需对照 AGENTS.md「非功能需求」抽查（API 平均响应 <200ms 等），完整压测验收留给上线准备阶段。
- `docs/EXECUTION-PLAN.md` 与 `docs/IMPROVEMENT-PLAN.md` 是已归档的规划产物，与本 spec 冲突时以 AGENTS.md + docs/adr/ + 本 spec 为准。
- 对应执行计划任务编号：1-01～1-11、2-01～2-09、2-10～2-12、3-01～3-07（Spike S-01～S-08 的结论已被本 spec 吸收，不再单独执行）。
