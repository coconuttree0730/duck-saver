# Spec B2 · RabbitMQ 最终一致性（账户事件 → 统计异步化）

> Issue 标题（发布时使用）：`feat: add RabbitMQ eventual consistency for account events`
> 标签：`ready-for-agent`　分支建议：`feature/phase2b2-mq-consistency`
> 阶段二第 3/4 个 spec（原 Spec B 拆分而来；三份新 spec 中**最先实施**——零外部依赖）

---

## Problem Statement

account-service 目前在交易记录增删时通过同步 Feign（`StatisticsServiceClient`）触发 statistics-service 落数据点——写路径被同步耦合：statistics 抖动或重启，用户记账请求直接失败或超时；服务间任一端宕机窗口内的变更永久丢失，没有补偿手段。AGENTS.md 已定稿 MQ 拓扑与消息规范，RabbitMQ 容器与延迟消息插件也已就位，但代码里没有一行生产/消费逻辑。

## Solution

按 AGENTS.md 定稿的拓扑落地事件驱动：account-service 在业务落库后发布 fanout 事件到 `account.event.exchange`，statistics-service 幂等消费并落 `data_point`；写路径的同步 Feign 调用退场。消息体带全量业务字段（胖事件），消费方零回调；失败重试 3 次进死信，双侧定时兜底兜住最终一致。`account.event.queue` 本期只声明不消费，拓扑一次到位。

## User Stories

1. As a 用户, I want 记账时统计服务挂了也不影响我记这笔账, so that 记录动作永远秒成功
2. As a 用户, I want 统计页面在我记账后短暂延迟内自动反映最新数据, so that 我不需要理解"最终一致"也能信任数字
3. As a 后端开发者, I want 消息按 eventId 幂等去重, so that 网络重发不会让同一笔交易统计两次
4. As a 后端开发者, I want 处理失败的消息自动重试 3 次后进死信队列, so that 一条坏消息不会堵死整条管道
5. As a 后端开发者, I want 死信与兜底扫描有日志可查, so that 数据不一致可发现、可修
6. As a 后端开发者, I want 生产侧定时对账补发丢失事件, so that broker 不可用窗口期的变更不丢
7. As an 运维工程师, I want 队列/交换机/死信由代码声明式创建, so that 新环境起容器即得完整拓扑
8. As a 前端开发者, I want 统计接口契约不变, so that 异步化对我完全透明

## Implementation Decisions

- **拓扑（照 AGENTS.md，不新增概念）**：fanout exchange `account.event.exchange` → 队列 `account.event.queue`、`statistics.event.queue`；死信交换机/队列 `*.dlx`。全部经 Spring AMQP 声明式 Bean 创建，durable。
- **消息体规范**：必含 `eventId`(uuid) · `eventType` · `timestamp` · `data`；eventType ∈ `ACCOUNT_CREATED` | `ACCOUNT_UPDATED` | `ACCOUNT_DELETED` | `ITEM_ADDED` | `ITEM_DELETED`。公共消息结构放 common 模块，两侧共用。
- **胖事件**：`ITEM_ADDED` / `ITEM_DELETED` 的 `data` 携带 statistics 落 `data_point` 所需的全部字段（金额、分类、收支类型、日期、账户名），消费方**零回调** account-service；字段演进靠 `data` 内新增字段向后兼容，不删旧字段。
- **幂等落点**：仅在 `duck_saver_statistics` schema 建 `processed_event` 表（id/event_id 唯一键/consumed_at），实施第一步把 DDL 追加进 `docs/sql/04-statistics.sql` 并同步 dev-environment.md；消费规则为"查 eventId → 未处理则处理 + 同事务写入 processed_event"。第二个真实消费者出现前不推广成标准表。
- **队列归属**：`account.event.queue` 本期只声明绑定、不注册监听器（避免无人消费堆积告警）；留给未来通知/AI 场景。
- **生产侧**：account-service 业务事务提交后发布消息；发送失败本地重试，仍失败交由**定时兜底任务**扫描近期变更窗口对账补发（对账依据：业务表 updated_at vs 已知发布记录）。不上 outbox 表——当前规模下兜底任务足够，避免过度设计。
- **消费侧**：手动 ack；异常重试 3 次（配置间隔递增）后投死信；死信消费者仅记日志+告警标记，不做自动重放；另配定时兜底扫描死信与未处理缺口。
- **同步 Feign 写路径退场**：删除 account-service 中交易增删触发的 `StatisticsServiceClient` 同步调用及其 Fallback；Feign/OpenFeign 依赖若读路径也无使用者则一并移除。
- **配置注入**：连接信息经 Nacos；虚拟 host、重试参数可配置。

## Testing Decisions

- 只断言外部行为：broker 里的消息结构与消费后的库内终态；不断言监听器内部调用次数之类实现细节。
- Seam 1：根 POM reactor 全绿。
- Seam 2：Testcontainers `rabbitmq:3-management` + `mysql:8` 覆盖——正常消费落库、重复 eventId 幂等跳过、消费抛错重试后入死信、死信后主流程不受阻。
- Seam 3：单测覆盖消息组装（胖事件字段完整性）、eventType 枚举全覆盖；兜底任务的扫描判定逻辑用注入时钟/时间戳参数测。
- 冒烟套件扩展：记账 → 轮询统计接口直到数据点出现（awaitAtMost 沿用既有模式），断言"异步最终可见"。

## Out of Scope

- notification-service 接入消费（其延迟消息触发预算提醒/月末报告归 Spec C）
- 分布式事务/Seata（明确排除，MQ 最终一致性即其替代）
- 消息轨迹面板、broker 高可用集群
- OAuth2（003）、缓存/锁/可观测性（005）

## Further Notes

- 分支 `feature/phase2b2-mq-consistency`，PR 目标 develop；无契约变更（接口契约不动，仅架构行为变化）。
- EXECUTION-PLAN 对应任务：5-07、5-08、5-09、5-10。
- 实施顺序上本 spec 先于 003/005：纯内部改造，不被任何外部凭证或资质卡住。
- 完成判定：记账→统计链路全程异步且冒烟绿；杀掉 statistics 再记账、拉起后数据自动补齐（手工验收场景）；`docker compose down -v && up -d` 后拓扑自建可用。
