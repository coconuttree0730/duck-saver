# API 契约（前后端唯一权威定义）

> **权威源说明**：本文件由项目根目录分发至前后端两个仓库，**以 duck-saver-backend/API-CONTRACT.md 为唯一权威版本**；接口变更先改后端副本再同步本文件。

> 后端实现与本文件保持一致；前端的请求路径、字段、枚举以本文件为准。
> 修改任何接口：**先改本文件，再改代码，两侧同步**。
> 下述路径均为网关后的路径；服务端口与路由见 `duck-saver-backend/AGENTS.md`「网关路由」。

## 全局响应包装

所有接口的真实响应统一为 `{code, message, data, timestamp}` 包装（`timestamp` 为毫秒时间戳）；
本文档各节描述的业务结构均位于 `data` 字段内。错误码表：

| code | 含义 |
|------|------|
| 200  | 操作成功 |
| 400  | 参数错误 |
| 401  | 未登录/凭证失效 |
| 403  | 无权限 |
| 404  | 资源不存在 |
| 409  | 冲突（如乐观锁重试耗尽） |
| 500  | 系统异常 |

业务细分码按模块预留：1xxx 账户 / 2xxx 统计 / 3xxx 通知 / 4xxx 认证。

认证细分码：

| code | 含义 |
|------|------|
| 4001 | OAuth provider 暂未开放（如微信待资质，disabled） |
| 4002 | 第三方授权凭证无效或已过期（code 换 token 失败） |
| 4003 | 该第三方身份已绑定其他账号 |

---

## account-service（:6000）

```
GET    /accounts/current          获取当前账户信息
POST   /accounts                  创建新账户
PUT    /accounts/{name}           更新账户
DELETE /accounts/{name}           删除账户
POST   /accounts/{name}/items     添加收支记录（领域术语 Transaction；路径 /items 为兼容保留）
DELETE /accounts/{name}/items/{id} 删除收支记录
GET    /accounts/demo             获取 demo 账户（预填充数据）
```

创建账户请求：

```json
{ "name": "日常开销", "currency": "CNY" }
```

账户响应：

```json
{
  "name": "日常开销",
  "currency": "CNY",
  "lastUpdate": "2026-08-21T10:00:00Z",
  "countries": ["CN"],
  "items": [
    {
      "id": "uuid",
      "title": "午餐",
      "amount": 48.00,
      "currency": "CNY",
      "category": "餐饮",
      "type": "EXPENSE",
      "date": "2026-08-21"
    }
  ],
  "saving": {
    "amount": 10000.00,
    "interest": 0.0150,
    "deposit": 5000.00,
    "currency": "CNY"
  }
}
```

添加收支记录请求：

```json
{
  "title": "午餐",
  "amount": 48.00,
  "currency": "CNY",
  "category": "餐饮",
  "type": "EXPENSE",
  "date": "2026-08-21"
}
```

枚举：

- **category**：餐饮、交通、购物、娱乐、居住、通讯、医疗、教育、其他
- **type**：`INCOME`、`EXPENSE`

---

## statistics-service（:7000）

```
GET  /statistics/current          获取当前账户统计
GET  /statistics/{account}        获取指定账户统计
PUT  /statistics/{account}        创建/更新数据点
GET  /statistics/demo             获取 demo 统计
```

统计响应：

```json
{
  "account": "日常开销",
  "expense": { "currentValue": 5000.00, "previousValue": 4500.00, "percentChange": 11.1 },
  "income":  { "currentValue": 15000.00, "previousValue": 14000.00, "percentChange": 7.1 },
  "saving":  { "currentValue": 10000.00, "previousValue": 9500.00, "percentChange": 5.3 },
  "cashflow": [
    { "date": "2026-08-01", "income": 500.00, "expense": 300.00, "saving": 200.00 }
  ]
}
```

---

## notification-service（:8000）

```
GET    /notifications/settings/current    获取通知设置
PUT    /notifications/settings/current    更新通知设置
POST   /notifications/recipient           添加通知接收人
DELETE /notifications/recipient/{name}    删除通知接收人
```

通知设置响应：

```json
{
  "recipient": {
    "name": "demo",
    "email": "demo@example.com",
    "frequency": "WEEKLY",
    "enabled": true
  },
  "notificationConfig": [
    { "type": "BACKUP", "cronExpression": "0 0 9 ? * MON" },
    { "type": "BILL_REMINDER", "cronExpression": "0 0 10 1 * *" }
  ]
}
```

---

## auth-service（:5000）

```
POST /uaa/login                    密码登录
POST /uaa/token/refresh            刷新令牌（refresh_token 轮换，旧值作废）
POST /uaa/logout                   登出（当前 token 立即失效）
POST /uaa/oauth2/github            GitHub OAuth2 登录
POST /uaa/oauth2/wechat            微信 OAuth2 登录（当前未开放，返回 4001）
POST /uaa/oauth2/{provider}/bind   绑定第三方身份到当前登录用户
DELETE /uaa/oauth2/{provider}/bind 解除第三方绑定
POST /uaa/account/client           创建客户端凭证
GET  /uaa/account/{username}       获取用户信息
```

密码登录请求：

```json
{ "grant_type": "password", "username": "demo", "password": "demo" }
```

密码登录请求：

```json
{ "username": "demo", "password": "demo" }
```

登录响应（data 内）：

```json
{
  "tokenName": "satoken",
  "tokenValue": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "3f7d8f9a-....",
  "expires_in": 604800
}
```

- tokenValue 为 JWT（sa-token-jwt Mixin 模式），后续请求以 `satoken: <tokenValue>` 头携带
- refresh_token 服务端存储、一次性轮换；配合 `POST /uaa/token/refresh` 换取新令牌对
- 刷新请求体：`{ "refresh_token": "..." }`

---

## ai-service（:9000）

```
POST /ai/classify                   支出智能分类
POST /ai/query                      自然语言查询
GET  /ai/insight/{account}          获取支出洞察
```

分类请求：

```json
{ "description": "美团外卖 48元", "categories": ["餐饮", "交通", "购物", "娱乐"] }
```

分类响应：

```json
{
  "category": "餐饮",
  "confidence": 0.95,
  "alternatives": [{ "category": "购物", "confidence": 0.03 }]
}
```

自然语言查询请求：

```json
{ "question": "我上个月交通花了多少钱", "account": "demo" }
```

查询响应：

```json
{
  "answer": "您上个月交通支出共计 328.50 元，占总支出的 12.3%。",
  "data": { "total": 328.50, "percentage": 0.123, "period": "2026-07-01 ~ 2026-07-31" }
}
```

质量目标：分类准确率 ≥ 85%（响应 < 2s）；意图识别准确率 ≥ 80%；分类结果缓存 TTL 24h；月末生成支出洞察报告，预测超支提前 7 天经 notification-service 发邮件预警。
