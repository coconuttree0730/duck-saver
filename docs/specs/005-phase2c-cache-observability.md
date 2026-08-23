# Spec C · 多级缓存 + 分布式锁 + 可观测性

> Issue 标题（发布时使用）：`feat: add multi-level caching, distributed lock and observability`
> 标签：`ready-for-agent`　分支建议：`feature/phase2c-cache-observability`
> 阶段二第 4/4 个 spec（收尾）；内部分两段交付：**C-1 缓存+锁**（业务代码）→ **C-2 可观测性**（纯 infra）

---

## Problem Statement

阶段二前三个 spec 完成后，业务链路已通但仍裸奔：账户读取每次穿透到 MySQL，交易高频读写无并发保护（乐观锁冲突靠用户手点重试），AI 分类/汇率这类"输入重复率高、变更频率低"的调用没有缓存位。同时系统没有链路追踪与日志聚合——出了慢调用无从定位，排障只能 SSH 翻容器 stdout。AGENTS.md 已定稿缓存分层、Redisson 锁与可观测方案，但一行未落地。

## Solution

**C-1**：统一走 Spring Cache `@Cacheable` 抽象实现 Caffeine（L1）+ Redis（L2）多级缓存，Redisson 提供 account 粒度分布式锁配合乐观锁重试。**C-2**：docker-compose 补齐 SkyWalking（链路）、Loki+Promtail（日志），与既有 Prometheus+Grafana（指标）汇成统一观测面。完成后：缓存命中率可量化、并发写有锁保护、一次请求的完整链路与日志在 Grafana/SkyWalking UI 内可查。

## User Stories

1. As a 用户, I want 账户详情页秒开, so that 高频查看不吃数据库
2. As a 用户, I want 两台设备同时改同一账户时系统自动重试而非报错, so that 并发冲突对我无感
3. As a 后端开发者, I want 缓存统一走 @Cacheable 注解, so that 缓存策略集中、业务代码零侵入
4. As a 后端开发者, I want 写操作先拿 account 粒度 Redisson 锁再走乐观锁重试（至多 3 次）, so that 热点账户写冲突可控
5. As a 后端开发者, I want AI 分类与汇率结果进 L1 缓存（TTL 24h/1h）, so that 阶段三接入 AI 时成本与延迟天然受控
6. As an 运维工程师, I want 在 SkyWalking UI 看到一次请求跨 gateway→account→statistics 的完整链路, so that 慢调用一眼定位
7. As an 运维工程师, I want 在 Grafana 一个面板里同时查指标和日志, so that 排障不换工具
8. As an 运维工程师, I want Promtail 自动采集五个服务+网关的容器日志进 Loki, so that 无需逐容器翻日志

## Implementation Decisions

- **C-1 缓存分层（照 AGENTS.md 定稿值）**：
  - L1 Caffeine（JVM）：AI 分类结果 TTL 24h · 汇率 TTL 1h——本 spec 先建好缓存位与序列化配置，真实数据源阶段三接入（可先以汇率占位接口验证）。
  - L2 Redis：账户信息 TTL 5min · 统计数据 TTL 1min。
  - 统一 Spring Cache 抽象，自定义多级 CacheManager（L1 未命中查 L2，L2 未命中查库回填）；写路径 @CacheEvict 保持 L1/L2 一致；Redis 序列化用 JSON（禁 JDK 序列化）。
  - 启用前提复核：仅对"输入重复率高且变更频率低"的读路径启用，账户/统计的写后读一致性由短 TTL + 主动失效保障。
- **C-1 分布式锁**：Redisson；锁粒度 account 级（`lock:account:{id}`），包裹"读-改-写"事务，锁内乐观锁冲突重试至多 3 次（沿用 Spec A 既有 @Version 机制），3 次耗尽返回 409；锁等待/持有时间可配置，watchdog 续期启用。
- **C-1 契约对齐**：409 语义已在契约错误码表中，无契约变更；若锁实现引入新错误场景（如获取锁超时），按 500 系统异常处理，不新增错误码。
- **C-2 SkyWalking**：docker-compose 新增 OAP+UI；各 JVM 服务以 `-javaagent` 零侵入接入，服务名按 `{service}-service` 命名，采样率开发环境 100%（生产可调，经 Nacos/compose 环境变量）；网关链路同样覆盖。
- **C-2 Loki**：docker-compose 新增 Loki+Promtail；Promtail 按容器标签采集全部后端服务日志；Grafana 预配 Loki 数据源与日志面板，与既有 Prometheus 面板同屏。
- **C-2 验收即交付**：可观测性无业务代码，验收标准为"面板能看到东西"——一条模拟慢调用在 SkyWalking 有完整 trace；Grafana 能按服务/时间过滤 Loki 日志；Prometheus 指标面板沿用既有配置补齐新服务。
- **实施顺序（spec 内部）**：先 C-1 后 C-2——C-2 收尾时正好能观测到包括缓存与锁在内的全部阶段二功能。

## Testing Decisions

- C-1 只断言外部行为：二次读取命中缓存（可用计数器/日志断言库访问次数下降）、写后读一致（TTL 内主动失效生效）、并发写场景最终一致且 409 仅在重试耗尽后出现。
- Seam 1：根 POM reactor 全绿。
- Seam 2：Testcontainers `redis:7` + `mysql:8`——缓存回填与失效、Redisson 锁互斥（两线程争锁）、锁内乐观锁重试路径。
- Seam 3：`-Dsmoke` 扩展：同一账户连续读两次断言响应一致且耗时特征符合缓存路径；并发脚本对同一账户并发写 N 次断言无丢失更新。
- C-2 以手工/脚本验收为主（trace 存在性、日志可查询），不做自动化断言——观测基础设施的"测试"就是看得见。

## Out of Scope

- AI 分类/汇率的真实数据源接入（阶段三，本 spec 只留缓存位）
- 延迟消息替代 notification-service 的 @Scheduled（AGENTS.md 目标态，但依赖阶段三通知业务场景，届时随通知功能落地；本期不动）
- Prometheus/Grafana 告警规则调优（现有基础配置够用）
- OAuth2（003）、MQ（004）

## Further Notes

- 分支 `feature/phase2c-cache-observability`，PR 目标 develop；无契约变更。
- EXECUTION-PLAN 对应任务：6-01～6-06、6-09（6-07/6-08 延迟消息部分移出，见 Out of Scope）；7-01～7-06、7-07（7-08/7-09 全量测试与压测归上线准备阶段）。
- 实施顺序：004 B2-MQ → 本 spec C-1 → 003 B1-OAuth2 → 本 spec C-2（C 跨两次进场是刻意的：C-2 放最后，让观测覆盖整个阶段二成果）。
- 完成判定：C-1 冒烟全绿 + 缓存/锁测试绿；C-2 三件套（SkyWalking trace / Loki 日志 / Prometheus 指标）在 Grafana 与 SkyWalking UI 可见；`docker compose down -v && up -d` 全栈可用。
