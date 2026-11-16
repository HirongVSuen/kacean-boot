---
name: power-system-naming
description: power-system 项目包、类、方法命名规范。新建/重命名包、类、接口、方法、常量、测试方法，或审查命名时使用：固定 <子域>.app 与 <子域>.port.<web|infra> 包结构、Facade/Repository/用例服务等类名后缀、findById/getRequired 等方法语义，以及中文测试方法名约定。
---

# power-system 包、类、方法命名规范

命名是架构约束的一部分：包结构与 `XxxFacade` 等命名会被 ArchUnit 强制（见 `common-test` 的 `GlobalArchitectureTest`），注释要求见 `power-system-javadoc`。

## 1. 通用原则

- 生产代码使用**有意义的英文**，中文术语与英文命名的对应关系以 `docs/domain/glossary.md` 为准；新增领域概念时同步术语表。
- 不用拼音、不用中英文混杂（如 `getPiaoZhong`），不使用无信息量的 `data`、`info`、`temp`、`doSomething`。
- 类名用大驼峰、方法与字段用小驼峰、常量与枚举值用 `UPPER_SNAKE_CASE`。
- 谨慎使用 `Manager`、`Helper`、`Util`：确有稳定工具语义才使用，业务逻辑放应用服务或领域对象。

## 2. 包命名

固定结构：`com.kacean.power.<子域>.app`（独立 app 模块）与 `com.kacean.power.<子域>.port.<web|infra>`（同属 port 模块），**包名一律全小写、用单数，子域名全局唯一**。

| 包段 | 规则 | 示例 |
| --- | --- | --- |
| `<子域>` | 子域术语的小写形式，顶层包即子域边界，app 与 port 同名；类名/术语才用大驼峰 | `tickettype`（对应 `TicketType`） |
| 层 | `app` 独立模块；`web` / `infra` 位于 `port` 之下，同属 port 模块 | `tickettype.app`、`tickettype.port.web` |
| 层内技术分包 | 固定小写集合，不随意发明 | 见下表 |
| 子域间契约 | `<子域>.contract`，独立为 `share/<子域>-contract` 模块 | `tickettype.contract` |
| 跨业务共享 | `com.kacean.power.core.share`（`share/core` 模块） | 见 `share/core/README.md` |

注意：Maven 业务模块仍按业务域聚合（`ticket-app/port`），包名不含业务域层级；因此**子域名必须全局唯一**，不同业务域不能重复使用同一子域名。

层内技术分包：

| 层 | 允许的技术分包 |
| --- | --- |
| `port.web` | `controller`、`dto`、`assembler`、`config` |
| `app` | `service`、`domain`、`repository` |
| `port.infra` | `persistence`、`client`、`config` |

`app/service` 内默认直接建 `XxxService` 作为用例入口；复杂用例类放 `usecase` 子包，`FacadeImpl` 契约实现放 `facade` 子包；**禁止将实现子包命名为 `contract`**（ArchUnit 按 `..contract..` 识别契约包，会误伤实现的依赖规则）。没有对应代码时不建空子包。应用层角色、调用方向与开发步骤见 `power-system-app`，契约模块见 `power-system-share`。

ArchUnit 已强制：层根包不得直接建类、不同子域实现包不能互相调用（对方 `contract` 契约包除外）、`XxxFacade` 只能在 `<子域>.contract`、`XxxFacadeImpl` 只能在 app 子域。port 内 **web 可直接调用 infra**（贫血阶段简单 CRUD 不强制经过 app），但 infra 不得依赖 web、app 不得依赖 port。命名时主动遵守，不要等测试报错。

## 3. 类命名

| 类型 | 规则 | 示例 |
| --- | --- | --- |
| MVC 入口 | `XxxController` | `TicketTypeController` |
| Web 入参 / 出参 DTO | `XxxRequest` / `XxxResponse`，只放 `<子域>.port.web.dto` | `DefineTicketTypeRequest` |
| DTO 与领域对象转换 | `XxxAssembler` | `TicketTypeAssembler` |
| 应用服务 | 默认 `XxxService`（`<子域>.app.service`），方法按用例动词命名；复杂用例再拆动词类放 `service/usecase`，不用 `Case` 后缀 | `TicketTypeService`；复杂用例 `DefineTicketType` |
| 聚合根 / 实体 | 领域名词，与术语表一致 | `TicketType` |
| 值对象 | 名词，优先 `record` 不可变 | `TicketState`、`TransitionTable` |
| 标识值对象 | `XxxId` | `TicketTypeId` |
| 仓储接口 | `XxxRepository`，放 `<子域>.app.repository` | `TicketTypeRepository` |
| 仓储实现 | 技术前缀 + 接口名，放 `port.infra` | `InMemoryTicketTypeRepository` |
| 子域间同步契约 | `XxxFacade` 接口（`<子域>.contract`）+ `XxxFacadeImpl` 实现（`<子域>.app.service.facade`） | `TicketTypeFacade` / `TicketTypeFacadeImpl` |
| 契约 DTO / 视图 | `XxxView`（跨子域只读视图），不放领域实体 | `TicketTypeView` |
| 领域事件 | `XxxEvent`，过去式业务事实 | `TicketTypeDisabledEvent` |
| 业务异常 | `XxxException`，表达失败原因 | `TicketTypeValidationException` |
| 配置类 | `XxxConfig` | `TicketWebConfig` |
| 测试类 | 被测类名 + `Test` | `DefineTicketTypeTest` |
| 跨业务引用 / 快照 | `XxxRef` / `XxxSnapshot`（share/core 既有风格） | `DeviceRef`、`EntitySnapshot` |

## 4. 方法命名

按返回值与副作用选择动词，保持同项目同语义同名：

| 语义 | 约定 | 示例 |
| --- | --- | --- |
| 用例命令 | 动词原形，与应用服务用例对应 | `define(...)`、`disable(...)` |
| 按标识查询，允许不存在 | `findById`，返回 `Optional<T>` | `Optional<TicketType> findById(TicketTypeId id)` |
| 按标识查询，必须存在 | `getRequired...`，不存在抛业务异常 | `TicketTypeView getRequired(String id)` |
| 集合查询 | `listXxx` / `findByXxx`，不用 `getXxxList` | `listTicketTypes()` |
| 存在性 / 布尔判断 | `existsById`、`isXxx`、`hasXxx`、`canXxx` | `canTransit(...)` |
| 仓储写入 | `save`（新增与更新统一入口）、`deleteById` | `TicketType save(TicketType t)` |
| 校验 | `validateXxx`，私有方法也保持语义 | `validateTransitions(...)` |
| 领域行为 | 业务动词短语，不用 `process/handle` 泛泛命名 | `transitTo(...)` |
| 静态工厂 / 转换 | `of`、`from`、`toXxx` | `TicketTypeId.of(...)` |

**测试方法**允许使用中文场景描述（项目既有风格），格式为「场景 + 期望」，可用下划线分隔分句：

```java
@Test
void 定义完整票种成功_保存后可按标识查询() { ... }

@Test
void 迁移引用未定义状态被拒绝并指出非法迁移() { ... }
```

生产方法不使用中文名；测试方法中文描述应与需求场景对应。

## 5. 字段、常量与枚举

- 字段小驼峰，布尔字段用 `enabled`、`published` 等形容词或过去分词，避免与访问器冲突的裸 `is`。
- 常量 `UPPER_SNAKE_CASE`；枚举类型名用大驼峰单数（`RoleType`），枚举值 `UPPER_SNAKE_CASE`。
- 标识、编码等字符串字段用语义名（`bizCode`、`internalCode`），不笼统叫 `code1/code2`。

## 6. 命名后自检

1. 包名是否全小写、子域 app 与 port 包同名，且未把类建在层根包。
2. `Facade` / `FacadeImpl` / `Repository` 等后缀是否符合上表，跨子域是否只引用对方 `<子域>.contract`（对应 `share/<子域>-contract` 依赖）。
3. 类名、方法名与 `docs/domain/glossary.md` 术语一致；新概念已补术语表。
4. 运行 `mvn -pl business/deploy/ticket-deploy -am test`，确认 ArchUnit 命名/位置相关规则通过。
