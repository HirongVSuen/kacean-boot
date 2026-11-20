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

`BaseCase<D extends BaseDomain>` 是**可选用例基类**，`XxxService` 与复杂用例均不强制继承；构造器与仓储访问签名见 `power-system-build` §2。

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
2. 默认在 `service` 的 `XxxService` 中增加用例方法（业务复杂度足够时），用例命令、查询与必须存在语义的方法命名和返回类型遵循 `power-system-naming`。简单 CRUD 可不建应用服务，由 web 直调 infra。
3. 用例复杂时再抽 `service/usecase` 动词类，`XxxService` 保持编排入口；不要按行数机械拆分。
4. 需要保存时在 `repo` 定义接口，由 `port.infra` 提供实现；app 不出现具体持久化技术。通用读写可继承 common-app 的 `BaseRepository<D>`，分页查询不进通用接口，由业务仓储按自身条件声明。基类签名、标识模型、仓储适配、查询扩展与调用顺序以 `power-system-build` §2 为 Skill 层单一说明，字段与 DDL 等细节按需阅读 `common/common-infra/README.md` 和对应源码；方法命名与返回语义见 `power-system-naming`。
5. 出现真实跨子域调用时，按 `power-system-share` 定义或消费契约；`XxxFacade` 由提供方在 `service/facade` 实现 `XxxFacadeImpl`，Provider SPI 等适配型契约按实际实现环境放置。
6. 测试使用纯 JUnit + 测试内 fake 仓储（app 测试不依赖 port）；测试方法中文描述与需求场景对应。
7. 验证：`mvn -pl business/deploy/<域>-deploy -am test`，再按需执行全量 `mvn test`。

## 5. 不做

- 不建 `TicketService` 这类业务域上帝类（服务按子域命名，如 `TicketTypeService`）。
- 简单用例不提前抽 `usecase`，不使用 `Case` 后缀（`DefineTicketTypeCase` 冗余）。
- 不提前创建契约或空子包；只有真实需求出现时再建。
- 不用 `@Service` / `@Component` 差异替代架构约束；边界由包名、类名与 ArchUnit 保证。
