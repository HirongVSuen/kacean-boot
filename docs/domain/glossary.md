# 领域术语表

本文件是 kacean-boot 领域术语的唯一来源，文档、代码命名与 AI 生成内容均以本表为准。

## 维护规则

1. 新增或调整领域概念时，在同一次改动中同步更新本表，并标注对应代码位置。
2. 代码中的类名、枚举值、字段名、包名应与“代码命名”列一致；文档与注释中的中文应与“术语”列一致。
3. 条目按业务域分组；跨业务共享的概念放入“核心共享”，单一业务概念放入对应业务域分组。

## 两票（ticket）

| 术语 | 代码命名 | 类型 | 定义 | 同义词 / 禁用叫法 | 代码位置 |
| --- | --- | --- | --- | --- | --- |
| 票种类型 | `TicketType` | 实体（贫血） | 两票中的票种分类定义，当前包含票种类型名称与类目编号 | 票种 | `business/ticket/ticket-port/src/main/java/com/kacean/boot/tickettype/port/infra/entity/TicketTypeEntity.java` |

## 系统（system）

| 术语 | 代码命名 | 类型 | 定义 | 同义词 / 禁用叫法 | 代码位置 |
| --- | --- | --- | --- | --- | --- |
| 系统 | `system` | 业务域 | 系统相关业务边界，当前包含 system、loginaccount 子域及 app、port 与 deploy 模块 | — | `business/system/pom.xml` |
| 登录上下文 | `LoginContext` | 契约响应对象 | 当前登录用户的统一可变响应对象，包含用户标识与用户名，由 Provider 逐次填充 | — | `share/logincontext-contract/src/main/java/com/kacean/boot/logincontext/contract/LoginContext.java` |
| 登录上下文管理契约 | `LoginContextManager` | 对外契约 | 创建统一登录上下文并由所有 Provider 填充，填充不完整时抛出 `LoginContextUnavailableException` | — | `share/logincontext-contract/src/main/java/com/kacean/boot/logincontext/contract/LoginContextManager.java` |
| 登录上下文提供者 | `LoginContextProvider` | SPI | 将具体认证或执行环境中的身份信息填充到统一 `LoginContext` | — | `share/logincontext-contract/src/main/java/com/kacean/boot/logincontext/contract/LoginContextProvider.java` |
| 登录管理器 | `LoginManager` | 运行时组件 | 创建同一 `LoginContext`，按顺序调用所有 Provider 填充后返回；位于 standalone-runner，作为运行时编排组件而非业务应用服务 | — | `runner/standalone-runner/src/main/java/com/kacean/boot/runner/logincontext/LoginManager.java` |
| 登录上下文不可用异常 | `LoginContextUnavailableException` | 业务异常 | Provider 完成填充后仍缺少当前登录用户的必需身份信息时抛出 | — | `share/logincontext-contract/src/main/java/com/kacean/boot/logincontext/contract/LoginContextUnavailableException.java` |
| 登录账号 | `LoginAccount` | 实体（贫血） | 系统登录账号，包含登录账号、登录密码哈希与账号状态（0-禁用，1-启用） | 登录账户 | `business/system/system-port/src/main/java/com/kacean/boot/loginaccount/port/infra/entity/LoginAccountEntity.java` |
| 登录账号上下文提供者 | `LoginAccountContextProvider` | SPI 实现 | loginaccount 子域的登录上下文提供者：开发期从请求头 `X-Login-Account` 读取登录账号并查询启用账号，填充用户标识与登录名；未接入认证设施，未命中时不填充 | — | `business/system/system-port/src/main/java/com/kacean/boot/loginaccount/port/web/provider/LoginAccountContextProvider.java` |
