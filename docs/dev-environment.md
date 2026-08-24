# 本地开发环境

> 搭建或排查本地环境时读本文件。业务服务端口以 [../AGENTS.md](../AGENTS.md)「网关路由」为唯一来源，本文件只列基础设施。

## 启动顺序

1. 基础设施：`docker compose up -d`（MySQL、Redis、RabbitMQ、Nacos、可观测组件）
2. 确认 Nacos 控制台可达后，启动各业务服务与 gateway
3. 就绪标志：各服务 `/swagger-ui.html` 可访问，gateway `/uaa/**` 路由通

## 基础设施容器

| 服务 | 端口 | 说明 |
|------|------|------|
| mysql | 3306 | 1 实例 4 Schema；建库脚本在 `docs/sql/`（statistics 库含事件幂等表 `processed_event`） |
| redis | 6379 | L2 缓存 |
| rabbitmq | 5672（AMQP）/ 15672（管理后台） | 账户事件拓扑由服务启动时声明式创建（exchange `account.event.exchange` + 两队列 + 死信） |
| nacos-server | 8848 | 注册/配置中心控制台 |
| nginx | 80 / 443 | 反向代理：`/` → frontend:3000，API 路径 → gateway:4000 |
| sentinel-dashboard | 8858 | 限流控制台 |
| skywalking-oap / skywalking-ui | 12800 / 8080 | 链路追踪 |
| prometheus | 9090 | 指标采集 |
| grafana | 3000 | 指标+日志统一面板 |
| loki / promtail | 3100 / 9080 | 日志存储/采集 |

## Sentinel 限流规则导入（Nacos 持久化）

服务启动时从 Nacos 拉取限流规则：group `SENTINEL_GROUP`，dataId `{service}-flow-rules`（QPS）与 `{service}-system-rules`（全局线程 500）。种子 JSON 在 `docs/nacos-seed/`，Nacos 就绪后导入：

```bash
for f in docs/nacos-seed/*.json; do
  name=$(basename "$f" .json)
  curl -s -X POST 'http://localhost:8848/nacos/v1/cs/configs' \
    -d "dataId=$name&group=SENTINEL_GROUP&type=json" \
    --data-urlencode "content@$f"
done
```

规则可在 Sentinel Dashboard（:8858，默认 sentinel/sentinel）查看调整；改 Nacos 配置即热更新。

## 安全基线

- 所有容器端口只绑 `127.0.0.1`，不对局域网/公网暴露
- 下游服务信任网关透传的 `X-User-Name` 头——该机制仅在「业务服务端口不可被外部直连」的前提下安全

## MySQL 初始化

`docker compose up -d mysql` 时，容器会自动执行 `docs/sql/` 下的脚本完成首建（仅首次、数据卷为空时）：

- `01-create-databases.sql`：创建四个 Schema 与四个服务专用账号（各账号仅有自己库的权限）
- `02-auth.sql` ~ `05-notification.sql`：各库表结构（统一含 id/version/deleted/created_at/updated_at）
- `06-seed-demo.sql`：demo 种子数据（账户 demo/CNY、示例交易记录、储蓄概况、通知配置；幂等可重放）

> `dev_password` 仅限本地开发；生产凭证走 Nacos 配置注入。
> 手动重置：`docker compose down -v` 清掉 mysql-data 卷后重启即重新初始化。

## SkyWalking Agent（可观测性链路追踪）

docker-compose 以只读卷方式将 `./skywalking/skywalking-agent` 挂载进各 JVM 服务（`-javaagent` 零侵入接入）。该目录不入版本库，首次拉取代码后需手动下载：

```bash
cd skywalking
curl -L -C - -o sw.tgz https://archive.apache.org/dist/skywalking/java-agent/9.4.0/apache-skywalking-java-agent-9.4.0.tgz
tar xzf sw.tgz   # 解压出 skywalking-agent/
cd .. && docker compose up -d
```

- OAP UI：http://127.0.0.1:8082 ；OAP gRPC 11800 仅容器网络内部使用
- Loki：Promtail 按容器名采集全部后端容器日志 → http://127.0.0.1:3100 ；Grafana 预配 "Duck Saver - Logs (Loki)" 面板
