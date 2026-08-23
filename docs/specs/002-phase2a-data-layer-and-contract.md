# Spec A · 数据层迁移 + 业务接口契约重写

> Issue 标题（发布时使用）：`feat: migrate data layer to MySQL and rewrite APIs to contract`
> 标签：`ready-for-agent`　分支建议：`feature/phase2a-mysql-contract`
> 阶段二第 1/4 个 spec（原"Spec B/C"已细化为：003 B1-OAuth2 · 004 B2-MQ 一致性 · 005 C-缓存锁+可观测性；实施顺序 004 → 005(C-1) → 003 → 005(C-2)）

---

## Problem Statement

后端框架已完成现代化升级，但业务层仍是 PiggyMetrics 遗留形态：数据存在 MongoDB、领域模型是 Item/incomes-expenses、没有收支分类概念；API-CONTRACT.md 定义的 Duck Saver 真实业务（Transaction 收支记录、九类消费分类、INCOME/EXPENSE 类型、储蓄概况 Saving）完全没有落地。同时登录响应还是临时形态而非 JWT，所有响应缺少统一的时间戳与规范化错误码。前端已就绪却无法对接——因为没有任何一个接口与契约一致。

## Solution

把四个业务服务的持久层从 MongoDB 迁移到 MySQL 8 + MyBatis Plus（四 Schema 四账号，手工 DDL + demo 种子），按 API-CONTRACT.md **先改契约、再写代码**全量重写业务接口；认证升级到 sa-token 1.40+ 并采用 sa-token-jwt Mixin 模式（JWT 验签 + Redis 吊销双重校验，原生 refresh_token）；全仓统一响应体替换为 `Result<T>`。完成后：前端可以照契约开发每一个页面，MongoDB 从架构中退场。

## User Stories

1. As a 用户, I want 用用户名密码登录拿到 access_token 与 refresh_token, so that 我能安全地访问我的记账数据并在会话过期后免密续期
2. As a 用户, I want access_token 过期时用 refresh_token 换取新 token 且旧 refresh 立即作废（轮换）, so that 长期使用中凭证被盗风险最低
3. As a 用户, I want 登出后我的 token 立即失效, so that 公共设备上不留安全隐患
4. As a 用户, I want 创建多个账户并指定币种, so that 我能把日常开销和旅游基金分开管理
5. As a 用户, I want 录入一笔交易记录（金额/分类/收支类型/日期/备注）, so that 我能精确追踪每笔消费
6. As a 用户, I want 从餐饮、交通、购物、娱乐、居住、通讯、医疗、教育、其他九类中选择消费分类, so that 月末统计有意义
7. As a 用户, I want 删除记错的交易记录, so that 我的账本保持准确
8. As a 用户, I want 访问预置的 demo 账户看演示数据, so that 注册前就能了解产品形态
9. As a 用户, I want 查看某账户的储蓄概况（总额/年利率/累计本金）, so that 我知道攒了多少钱
10. As a 用户, I want 看到本月支出相比上月的百分比变化, so that 直观感知消费趋势
11. As a 用户, I want 并发修改同一账户时不丢失更新（乐观锁保护）, so that 多端操作数据可靠
12. As a 后端开发者, I want 四个业务服务的持久层统一为 MyBatis Plus BaseMapper, so that 单表 CRUD 不再手写 SQL
13. As a 后端开发者, I want 所有表带统一的 id/created_at/updated_at/deleted/version 规范字段, so that 行为可预期、审计有据
14. As a 后端开发者, I want 每个服务只拥有自己 Schema 的数据库账号, so that 权限隔离符合最小授权
15. As a 后端开发者, I want 手工 DDL 脚本按库分文件并由容器初始化自动执行首建, so that 新环境一条命令起库
16. As a 后端开发者, I want 逻辑删除用 @TableLogic 而非物理删除交易记录, so that 误删可恢复
17. As a 后端开发者, I want 全仓统一 Result{code,message,data,timestamp} 响应体与 ResultCode 枚举, so that 错误码集中管理不再散落魔法数字
18. As a 后端开发者, I want 参数校验失败返回 400 + 具体字段信息, so that 前端能精准提示用户
19. As a 后端开发者, I want 乐观锁冲突返回 409, so that 前端可引导用户刷新重试
20. As a 前端开发者, I want API-CONTRACT.md 反映真实的线格式（含外层包装说明）, so that 照契约写代码不用猜
21. As a 前端开发者, I want 登录响应是 {tokenName, tokenValue(JWT), refresh_token, expires_in}, so that 我能标准地做令牌管理与自动续期
22. As a 前端开发者, I want 账户响应包含 items 数组（即交易记录）与 saving 对象且结构与契约逐字一致, so that 页面渲染零适配
23. As a 运维工程师, I want MySQL 容器加入 docker-compose 且四 Schema 四账号自动初始化, so that 一条命令拉起完整环境
24. As a 运维工程师, I want MongoDB 容器与依赖从架构中移除, so that 不再维护两套存储
25. As an AI agent 实现者, I want 契约先行且变更留痕（契约 diff 在 PR 中可见）, so that 我实现的接口有权威依据可对照验收
26. As an AI agent 实现者, I want 冒烟套件扩展覆盖全部新契约场景, so that 回归有自动化兜底
27. As a 项目负责人, I want 本 spec 不动 MQ/OAuth2/缓存（属 Spec B/C）, so that 变更范围可控、出问题好定位

## Implementation Decisions

- **切分前提**：本 spec 是阶段二三个 spec 中的第一个；GitHub/微信 OAuth2 与 client 凭证接口、MQ 最终一致性归 Spec B；缓存/锁/延迟消息/SkyWalking/Loki 归 Spec C。
- **数据库**：MySQL 8 容器入 compose；四 Schema（duck_saver_auth/account/statistics/notification）四账号（`*_user`，仅授权自己的库）；建库脚本与 dev-environment.md 对齐。
- **DDL 管理**：手工 SQL 脚本，docs/sql/ 按库分文件；MySQL 容器 init 目录挂载自动首建；后续变更手动维护（不上 Flyway）。
- **表清单**（一次建齐，oauth_binding/oauth_client 空表留给 Spec B）：duck_saver_auth：sys_user · oauth_binding · oauth_client；duck_saver_account：account · transaction · saving；duck_saver_statistics：data_point；duck_saver_notification：recipient · notification_config。
- **表规范**：统一 id（主键）/created_at/updated_at/deleted（@TableLogic 逻辑删除）/version（@Version 乐观锁）；账户名唯一约束；对外 JSON 中交易记录 id 以字符串呈现（契约示例为 uuid 风格）——具体主键类型由实现按 MyBatis Plus 规范定夺。
- **ORM**：spring-data-mongodb 依赖移除；实体迁入 entity/ 包、访问层迁入 mapper/ 包（对齐 AGENTS.md 包结构）；继承 BaseMapper，复杂查询用 LambdaQueryWrapper。
- **statistics 落库形态**：data_point 以（账户, 日期）为粒度，指标明细（收入集/支出集/统计量/汇率）用 MySQL 8 JSON 列存储——保持与契约 cashflow/percentChange 输出兼容，避免过度拆表。
- **认证升级**：sa-token 升级至 1.40+（原生 refresh_token 支持）；引入 sa-token-jwt **Mixin 模式**（StpLogicJwtForMixin：JWT 可离线验签 + Redis 会话可即时吊销，双重校验）；网关与 auth-service 配置同款 StpLogic 与签名密钥（密钥经 Nacos 注入，禁止硬编码）。access_token 默认 7 天、refresh_token 30 天且每次刷新轮换（旧的立即作废），数值可配置。
- **登录契约修正（先改 API-CONTRACT.md 再实现）**：请求 `{username, password}`；响应 data 含 `tokenName/tokenValue/refresh_token/expires_in`；保留 `GET /uaa/account/{username}`；删除 grant_type/JWT 旧示例描述。
- **契约全局修订**：文档头部增加统一包装说明——所有响应为 `{code, message, data, timestamp}`，`data` 内才是各节描述的业务结构；错误码表：200 成功 · 400 参数错误 · 401 未登录/凭证失效 · 403 无权限 · 404 不存在 · 409 冲突（乐观锁重试耗尽）· 500 系统异常；业务细分码预留段：1xxx 账户 / 2xxx 统计。
- **响应体重构**：common 模块新增 `Result<T>`（字段 code/message/data/timestamp；ResultCode 枚举；静态工厂 success()/success(data)/error(message)/error(ResultCode,message)/response(code,message,data)），全仓删除 ApiResponse；Feign 解包类型、控制器单测断言、冒烟断言同步更新；timestamp 为毫秒长整型。
- **业务重写范围**：account-service（账户 CRUD、交易记录增删、demo 种子）、statistics-service（当前/指定账户统计、数据点生成仍由 account 同步 Feign 触发——MQ 异步化留给 Spec B）、notification-service（通知设置读写）、auth-service（登录/登出/刷新/用户信息）。接口路径、方法、字段逐一对照 API-CONTRACT.md。
- **MongoDB 退场**：四服务 Mongo 依赖与配置移除；compose 中 mongodb 服务与 mongodb/ 目录删除；`.env` 清理相关变量。
- **AGENTS.md 同步**：技术栈行更新 sa-token 版本与 jwt 说明；鉴权小节补充 refresh_token 轮换规则。
- **明确排除**：Seata 分布式事务（MQ 最终一致性已覆盖其目标）；前端仓库一切改动。

## Testing Decisions

- 好测试只断言外部行为：HTTP 状态码、Result 包装的 code/message/data、契约字段逐字匹配；不断言内部实现（Mapper 是否被调用、SQL 形态等）。
- Seam 1（已有）：根 POM reactor `mvn clean verify` 全绿——构建与单测门禁。
- Seam 2（已有模式，换组件）：Testcontainers `mysql:8` 支撑切片与上下文测试（替代此前的 mongo 容器）；repository/mapper 测试验证唯一约束、乐观锁自增、逻辑删除过滤等数据库级行为。
- Seam 3（已有，扩展）：`-Dsmoke` Gateway 黑盒套件新增场景——注册→登录拿 JWT→刷新轮换→登出失效→账户 CRUD→录入/删除交易记录→统计读取→通知设置读写，全程经 gateway :4000 断言契约结构。
- Prior art：阶段一的 controller standalone MockMvc 测试（含包装断言）与服务层 Mockito 单测模式直接沿用；冒烟套件的 awaitAtMost 轮询模式沿用。

## Out of Scope

- GitHub / 微信 OAuth2 登录、client 凭证接口（Spec B；本 spec 仅预留空表）
- RabbitMQ 最终一致性：事件发布/幂等消费/DLQ/兜底任务（Spec B；statistics 同步触发维持现状）
- Caffeine+Redis 多级缓存、Redisson 分布式锁、延迟消息替代 @Scheduled（Spec C）
- SkyWalking 链路追踪、Loki/Promtail 日志聚合（Spec C）
- 前端仓库任何改动；AI 能力（阶段三）
- MongoDB→MySQL 存量数据迁移工具（现库为空，已验证）

## Further Notes

- 分支 `feature/phase2a-mysql-contract`，PR 目标 develop；契约文件的修改必须出现在同一个 PR 中供对照审查。
- EXECUTION-PLAN 对应任务：4-01～4-09、4-11（4-10 数据迁移工具确认取消）、5-01/5-04/5-05/5-06（认证与乐观锁部分提前并入本 spec，其余 Week5 任务归 Spec B）。
- 完成判定：swagger 与契约逐字一致；冒烟全绿；`docker compose down -v && up -d` 后全栈可用。
