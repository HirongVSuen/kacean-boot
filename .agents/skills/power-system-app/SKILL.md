---
name: power-system-app
description: power-system 应用层（app）开发指南。新增或修改应用服务、复杂用例类、Facade 契约实现、仓储接口、领域对象时使用：XxxService 默认承载用例、复杂用例再放 service/usecase、FacadeImpl 放 service/facade，贫血阶段 web 可直调 infra、app 按需介入，以及与 share 契约的联动。
---

# power-system 应用层开发指南

适用于 `<子域>.app` 业务模块（独立 module；web 与 infra 同属 `<域>-port` 模块）。命名细节见 `power-system-naming`，注释见 `power-system-javadoc`，契约模块见 `power-system-share`，构建验证见 `power-system-build`。

**定位**：DDD 前期贫血模式下，简单 CRUD 由 `port.web` 直接调用 `port.infra` 即可，不强制经过 app；app 承载领域模型与仓储接口，**业务复杂度上升（多步编排、跨仓储协作、独立事务/权限边界）时才在 app 增加应用服务**，避免为简单场景提前铺设用例层。

## 1. 包结构与角色

```text
com.kacean.power.<子域>.app
├── domain/                  # 实体、值对象、领域服务（纯 Java）
├── repo/                    # 仓储接口，由 port 模块 infra 包实现
└── service/
    ├── TicketTypeService.java   # 默认：子域应用服务，承载常规用例方法
    ├── usecase/             # 可选：复杂用例独立类（DefineTicketType），仅复杂时创建
    └── facade/              # 契约实现：TicketTypeFacadeImpl，仅在有 contract 时创建
```

- **需要应用服务时默认建 `XxxService`**：子域应用服务是复杂用例入口，常规用例直接作为其方法（`define` / `revise` / `findById`）；简单 CRUD 不提前铺设（web 可直调 infra）。
- **只有复杂用例才增加 `usecase`**：当某用例编排明显复杂（多步流程、多仓储/外部协作、独立事务或权限边界）时，在 `service/usecase` 新建动词类，由 `XxxService` 委托；简单用例不提前拆分。
- **禁止将实现子包命名为 `contract`**：`GlobalArchitectureTest` 按 `..contract..` 识别契约包并禁止其依赖 app 实现，`service/contract` 会误伤 `FacadeImpl`；契约实现一律放 `service/facade`。
- `usecase`、`facade` 是 service 内固定的角色子包，不发明其他子包；没有对应代码时不建空包。

## 2. 角色职责与装配

| 角色 | 位置 | 注解 | 职责边界 |
| --- | --- | --- | --- |
| 应用服务 `TicketTypeService` | `service` | `@Service` | 默认用例入口，编排「校验 → 领域对象 → 仓储」；复杂用例委托 `usecase` |
| 复杂用例 `DefineTicketType` | `service/usecase` | `@Service` | 仅复杂用例：独立编排与校验，由应用服务调用 |
| 契约实现 `TicketTypeFacadeImpl` | `service/facade` | `@Service` | `XxxView` ↔ 领域对象转换并委托应用服务，不承载业务规则 |
| 仓储接口 `TicketTypeRepository` | `repo` | 无 | 只定义持久化抽象，可继承 common-app `BaseRepository<D>` 复用最小读写；实现放 `port.infra` |
| 领域对象 | `domain` | 无 | 纯 Java，不依赖 Spring、Web、持久化 |

`BaseCase<D extends BaseDomain>` 是构造器接收 `BaseRepository<D>`、通过受保护的 `getRepository()` 暴露仓储的**可选用例基类**，不是标记接口；`XxxService` 与复杂用例均不强制继承。

## 3. 调用方向

```text
web controller ──（简单 CRUD，贫血阶段）──→ port.infra 仓储实现 ──→ app 的 repository 接口 / domain

web controller ─┐（复杂用例，经过 app）
                ├─→ XxxService ──→（复杂用例）usecase ──→ domain / repository
facade 实现 ────┘        └──────────（常规用例直接编排）──→ domain / repository
```

- **web 可直调 infra**：简单查询/保存场景，Controller 可直接注入 `port.infra` 的仓储实现（或 app 定义的仓储接口），不强制经过 `XxxService`。
- **复杂业务才经过 app**：出现多步编排、跨仓储协作、独立事务或权限边界时，由 `XxxService` 承载用例，Controller 改为调用应用服务；`FacadeImpl` 始终经由 `XxxService`，不直接调用 `usecase`。
- `usecase` 之间不互相调用；多个用例共享的规则下沉 domain 服务或独立校验组件。
- app 不依赖 port；跨子域只依赖对方 `<子域>.contract`（见 `power-system-share`）。

## 4. 开发步骤

1. 阅读 `docs/domain/glossary.md`，新领域概念同步术语表。
2. 默认在 `service` 的 `XxxService` 中增加用例方法（业务复杂度足够时），方法名与用例对应（`define` / `revise`）；按标识查询用 `findById` 返回 `Optional`，必须存在用 `getRequired...`。简单 CRUD 可不建应用服务，由 web 直调 infra。
3. 用例复杂时再抽 `service/usecase` 动词类，`XxxService` 保持编排入口；不要按行数机械拆分。
4. 需要保存时在 `repo` 定义接口，由 `port.infra` 提供实现；app 不出现具体持久化技术。通用读写可继承 common-app 的 `BaseRepository<D>`（`findById`/`findByIds`/`save`/`saveAll`/`removeById`/`removeByIds`），其中 `findById` 按基类签名返回 `D`，未命中为 `null`，不要当作 `Optional<D>` 使用；标识使用 `ID`（`id` 主键与 `code` 外部编号二选一）与 `IDCollection`（去重后最多 1000 个标识），批量写不默认保证原子性。infra 侧继承 `BaseRepositoryImpl`，由父类按泛型注入 `BaseDao` 与 `BaseConvert`，保存后返回由持久化 Entity 重新转换、包含主键与版本号的聚合；基类清单见 `power-system-build` 与 `common/common-infra/README.md`。分页查询不进通用接口，由业务仓储按自身条件声明，复用 `PageQuery`/`Query`/`PageResult`/`Sort` 等 common-app 查询模型；业务 QP 以普通类继承 `Query` 并声明过滤字段，DAO 按需覆写 `declareProperties` / `declareWheres`：前者声明 SELECT、关键字和固定排序，后者通过 `builder.query(XxxQueryParam.class)` 获取具体 QP，并用 `addWhere(field, Operator, value)` 声明特殊条件；`BaseDao.toWrapper` 再依次执行 `processDefaultFields()`（普通字段默认 EQ）、`processKeyword()`、`processOrder()`，业务即可复用 `list(query)` / `page(pageQuery)`。字段统一写 Entity Java 属性，由 `ColumnResolver` 映射数据库列；别名或复杂 `where(...)` 条件用 `markProcessed(...)` 避免重复处理。
5. 出现真实跨子域调用时，按 `power-system-share` 定义或消费契约；`XxxFacade` 由提供方在 `service/facade` 实现 `XxxFacadeImpl`，Provider SPI 等适配型契约按实际实现环境放置。
6. 测试使用纯 JUnit + 测试内 fake 仓储（app 测试不依赖 port）；测试方法中文描述与需求场景对应。
7. 验证：`mvn -pl business/deploy/<域>-deploy -am test`，再按需执行全量 `mvn test`。

## 5. 不做

- 不建 `TicketService` 这类业务域上帝类（服务按子域命名，如 `TicketTypeService`）。
- 简单用例不提前抽 `usecase`，不使用 `Case` 后缀（`DefineTicketTypeCase` 冗余）。
- 不提前创建契约或空子包；只有真实需求出现时再建。
- 不用 `@Service` / `@Component` 差异替代架构约束；边界由包名、类名与 ArchUnit 保证。
