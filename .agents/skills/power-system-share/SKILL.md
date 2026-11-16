---
name: power-system-share
description: power-system 共享与契约层（share）开发指南。新增或修改 share/core 共享类型、<子域>-contract 的 Facade/View/领域事件，或进行跨子域调用时使用；与 power-system-app 联动完成提供方实现与消费方接入。
---

# power-system 共享与契约层开发指南

适用于 `share/core` 与 `share/<子域>-contract`。模块事实与依赖约束以 `share/README.md` 为准；命名见 `power-system-naming`，注释见 `power-system-javadoc`，模块创建见 `power-system-build`，实现侧见 `power-system-app`。

## 1. 两类模块的边界

| 模块 | 内容 | 依赖约束 | 变更要求 |
| --- | --- | --- | --- |
| `core` | 稳定、跨业务、无单一业务归属的领域类型 | 只用 JDK | 最高谨慎，评估全部使用方 |
| `<子域>-contract` | 提供方子域所有的 `XxxFacade`、`XxxView`、领域事件 | 只用 JDK，可按需依赖 `core`；不得依赖任何 app/port 实现模块 | 随提供方演化，变更需评估消费方兼容性 |

- 契约归提供方，不把高频演化的业务契约放进 `core`。
- 只有真实跨子域调用需求出现时才创建契约；子域内部类型不进 share。

## 2. 提供方开发步骤

1. 没有契约模块时，按 `power-system-build` 在 `share/` 新建 `<子域>-contract` 并注册到 `share/pom.xml`。
2. 在 `com.kacean.power.<子域>.contract` 定义：
   - `XxxFacade`：跨子域同步调用接口，方法按语义命名（如 `getRequired`），中文 Javadoc 说明业务语义、返回值与异常；
   - `XxxView`：跨子域只读视图，不放领域实体；
   - `XxxEvent`：过去式业务事实，按需定义。
3. 提供方 app 模块添加自身 contract 依赖，在 `<子域>.app.service.facade` 实现 `XxxFacadeImpl`（`@Service`），只做视图转换并委托 `XxxService`。
4. 新术语同步 `docs/domain/glossary.md`。

## 3. 消费方接入步骤

1. 只添加对方 `<子域>-contract` 的 Maven 依赖，构造器注入 `XxxFacade`。
2. 不得依赖对方 `ticket-app`、`ticket-port` 等实现模块，不得 import 对方 app/port 实现包。
3. 不绕过契约直接访问对方领域对象或仓储。

## 4. 联动要点

- 契约接口只能位于 `<子域>.contract`；实现只能位于提供方 app 子域的 `service/facade`。
- **实现子包禁止叫 `contract`**：ArchUnit 按 `..contract..` 识别契约包并禁止其依赖 app 实现，误用会导致架构测试失败。
- `share` 模块只有被显式依赖才进入分发包，不要加入 `common-*` 让所有业务自动获得。

## 5. 验证

- 聚焦：`mvn -pl business/deploy/ticket-deploy -am test`，确认子域隔离、契约独立性、Facade 位置等规则通过。
- 全量：`mvn test`；契约变更时同时构建消费方模块，确认编译与行为兼容。
