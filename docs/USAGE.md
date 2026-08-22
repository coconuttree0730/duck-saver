# Duck Saver 后端使用指南（阶段一）

> 适用范围：基础设施升级完成后的当前状态。业务接口仍是迁移自 PiggyMetrics 的旧契约，Duck Saver 业务从阶段二开始编写。
> 所有服务只绑定本机：URL 一律用 `localhost`；端口约定以 [AGENTS.md](../AGENTS.md)「网关路由」为准。

---

## 一、浏览器直接打开（Web 面板）

| 面板 | 网址 | 账号 | 密码 |
|------|------|------|------|
| **Nacos 控制台**（服务列表/配置） | http://localhost:8848/nacos | 无需登录 | 无需登录 |
| **Sentinel Dashboard**（限流规则） | http://localhost:8858 | `sentinel` | `sentinel` |
| **Grafana**（监控面板） | http://localhost:3001 | `admin` | `admin`（首次登录提示改密，可跳过） |
| **Prometheus**（指标原始数据） | http://localhost:9090 | 无需登录 | 无需登录 |
| **RabbitMQ 管理**（队列/交换机） | http://localhost:25672 | `guest` | `guest` |

Grafana 登录后左侧 → Dashboards → 「Duck Saver · 微服务概览」。

## 二、Swagger 接口文档（浏览器直接打开）

| 服务 | 网址 |
|------|------|
| 认证 auth-service | http://localhost:4000/uaa/swagger-ui.html |
| 账户 account-service | http://localhost:4000/accounts/swagger-ui.html |
| 统计 statistics-service | http://localhost:4000/statistics/swagger-ui.html |
| 通知 notification-service | http://localhost:4000/notifications/swagger-ui.html |
| AI ai-service | http://localhost:4000/ai/swagger-ui.html |

均免登录，可直接调试接口。

## 三、启动与停止

```bash
docker compose up -d          # 拉起全栈（14 个容器）
docker compose ps             # 查看状态
docker compose logs -f gateway  # 跟踪某个服务日志
docker compose down           # 停止并移除容器
docker compose down -v        # ⚠️ 额外清除 MongoDB 数据卷
```

就绪标志：Nacos 里六个服务全部在线；`curl localhost:4000/uaa/actuator/health` 返回 `{"status":"UP"}`。

## 四、业务接口：统一走网关 localhost:4000

路由前缀决定落到哪个服务：

```
localhost:4000/uaa/**            认证服务
localhost:4000/accounts/**       账户服务
localhost:4000/statistics/**     统计服务
localhost:4000/notifications/**  通知服务
localhost:4000/ai/**             AI 服务
```

### 登录态规则

除以下白名单外，**全部要求携带 satoken 请求头**，否则返回 `401 {"code":401,"message":"not logged in"}`：

- `POST localhost:4000/uaa/login`（登录）
- `POST localhost:4000/uaa/users`（开放注册）
- `/uaa/oauth2/**`
- 第二节的五个 Swagger 地址及其资源路径

### 三步上手（复制即用）

```bash
# 1. 注册（用户名≥3位、密码≥6位）
curl -X POST localhost:4000/uaa/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo123"}'

# 2. 登录，拿 token
curl -X POST localhost:4000/uaa/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo123"}'
# 响应：
# { "code":0, "message":"success",
#   "data":{ "tokenName":"satoken", "tokenValue":"xxxxxxxx-..." } }

# 3. 带 token 访问受保护接口（把 xxx 换成 data.tokenValue）
curl localhost:4000/accounts/current -H "satoken: xxx"
```

所有业务响应都是统一包装 `{code, message, data}`：`code=0` 成功；非 0 失败（400 参数错误 / 500 内部错误），错误信息在 `message`。

### 当前可用的旧契约接口

```
GET    localhost:4000/accounts/current                     当前账户
PUT    localhost:4000/accounts/current                     更新账户
GET    localhost:4000/accounts/{name}                      指定账户
GET    localhost:4000/statistics/current                   当前统计
GET    localhost:4000/notifications/recipients/current     通知设置
PUT    localhost:4000/notifications/recipients/current     更新通知设置
GET    localhost:4000/ai/ping                              AI 服务探活 → data.status="pong"
```

## 五、直连端口速查（排障用，勿对外暴露）

| 宿主地址 | 容器 | 说明 |
|---------|------|------|
| localhost:4000 | gateway :4000 | API 网关（唯一对外入口） |
| localhost:5000 | auth-service :5000 | context `/uaa` |
| localhost:6000 | account-service :6000 | context `/accounts` |
| localhost:7000 | statistics-service :7000 | context `/statistics` |
| localhost:8000 | notification-service :8000 | context `/notifications` |
| **localhost:19000** | ai-service :9000 | 宿主 9000 被占故映射到 19000，context `/ai` |
| localhost:8848 | nacos-server :8848 | 控制台 + API（9848 为 gRPC） |
| localhost:8858 | sentinel-dashboard | |
| localhost:9090 | prometheus | |
| localhost:3001 | grafana :3000 | |
| localhost:25672 | rabbitmq :15672 | 管理后台 |
| localhost:15673 | rabbitmq :5672 | AMQP |
| localhost:16379 | redis :6379 | 无密码 |
| localhost:27017 | mongodb | root 账号：`user` / 密码：`.env` 里 `MONGODB_PASSWORD`（默认 password） |
| localhost:80 | nginx | `/`→前端(未部署)，API 前缀→gateway |

健康检查与指标直连示例：

```
http://localhost:5000/uaa/actuator/health
http://localhost:6000/accounts/actuator/health
http://localhost:7000/statistics/actuator/prometheus
http://localhost:8000/notifications/actuator/prometheus
http://localhost:19000/ai/actuator/health
```

## 六、本地开发模式（IDE 跑服务）

只起基础设施容器，服务在 IDE 里跑：

```bash
docker compose -f docker-compose.dev.yml up -d
```

IDE 启动各服务时设置环境变量（否则默认连容器网络主机名会失败）：

```
NACOS_SERVER_ADDR=localhost:8848
MONGODB_HOST=localhost
REDIS_HOST=localhost
SENTINEL_DASHBOARD_ADDR=localhost:8858
```

改了代码重新打包并更新容器：

```bash
mvn -pl <module> package -DskipTests
docker compose up -d --build <service>
```

## 七、日志查看

```bash
docker compose logs -f gateway            # 跟踪单个服务实时日志
docker compose logs --since 10m account-service   # 最近 10 分钟
docker compose logs account-service | grep ERROR  # 只看报错
```

- 业务服务日志走 stdout（`docker compose logs` 即全部）；容器内持久化位置：Sentinel 客户端在 `/root/logs/csp/`
- 日志级别调整：改各服务 yml 的 `logging.level.*` 后 `docker compose up -d --build <service>`；或经 Nacos 配置中心热更（阶段二完善）
- 跨服务链路追踪（SkyWalking）与集中式日志（Loki/Grafana 统一面板）属**阶段二**，当前排查靠 compose logs + Grafana 指标面板定位时间窗

## 八、阶段二才部署的组件（现在没有，别找不到）

| 组件 | 规划端口 | 计划 |
|------|---------|------|
| SkyWalking UI / OAP | :8080 / :12800 | 阶段二 Week 7（零侵入 JVM Agent 接入六个服务） |
| Loki / Promtail | :3100 / :9080 | 阶段二 Week 7（Grafana 统一日志面板） |
| MySQL | :3306 | 阶段二 Week 4（数据层 MongoDB → MySQL + MyBatis Plus） |

## 九、测试

```bash
mvn clean verify                      # 全量单测 + 上下文测试（需 Docker，Testcontainers 起 Mongo）
mvn -pl smoke-tests verify -Dsmoke    # Gateway 黑盒冒烟（重建镜像并拉起全栈，测完自动清理）
```

## 十、常见问题

| 现象 | 处理 |
|------|------|
| 接口 401 但我以为放行了 | 对照第四节白名单；Swagger 免登录，其余一律要 satoken 头 |
| Grafana/Sentinel 密码不对 | 见第一节表格；Grafana 首次登录强制改密，改忘了可 `docker compose down -v` 重置 |
| 服务没注册进 Nacos | `docker compose logs <service>`；确认 Nacos 先于业务服务启动 |
| health 显示 DOWN | 多为 MongoDB 认证或 Redis 连不上；确认 `.env` 的 `MONGODB_PASSWORD` 与容器初始化一致 |
| 限流不生效 | 规则存于 Nacos（group `SENTINEL_GROUP`），先导入种子：见 [dev-environment.md](dev-environment.md)「Sentinel 限流规则导入」 |
| 端口冲突 | redis/rabbitmq/ai 已映射高位端口（第五节）；再冲突改 docker-compose.yml 左侧宿主端口即可 |
