---
name: kacean-boot-business
description: kacean-boot 业务域开发收敛指南。新增或修改 business/<域>（app/port/deploy）任何业务代码、创建新业务域/子域、实现 CRUD/用例/仓储/契约或审查业务代码时使用：先自动扫描基础架构与通用组件（common-web/common-app/common-infra/common-util/share/ArchUnit）的最新事实，按复用矩阵强制复用基类与契约，禁止越界调用与重复造轮子；扫描发现通用组件与各 skill 描述漂移时，以代码为准同步升级对应 skill。
---

# kacean-boot 业务域开发收敛

**目的**：所有业务域（`business/<域>`）的 agent 开发必须收敛到统一的基础架构与通用组件之上。代码是唯一事实来源，skill 描述可能滞后——每次业务开发**先扫描、再设计、后编码**，不凭记忆假设某个基类存在或不存在。

分工：命名细则见 `kacean-boot-naming`，注释见 `kacean-boot-javadoc`，app 用例编排见 `kacean-boot-app`，契约见 `kacean-boot-share`，构建验证见 `kacean-boot-build`，贫血/DDD 业务代码生成见 `kacean-boot-codegen`，skill 随代码演进的自动同步见 `kacean-boot-skill-sync`。本 Skill 只负责「扫描事实 → 收敛复用 → 红线检查」，不复制上述 Skill 的全文。

## 1. 开发前自动扫描（必做）

在仓库根目录执行。Windows / DSH 使用 PowerShell 5.1+（首行仅放宽当前进程的执行策略，不修改系统设置）：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& .agents/skills/kacean-boot-skill-sync/scripts/scan-context.ps1
```

Git Bash / Linux / macOS：

```bash
bash .agents/skills/kacean-boot-skill-sync/scripts/scan-context.sh
```

脚本输出契约与详细清单开关由 `kacean-boot-skill-sync` §1 单源维护；本节只规定业务开发前必须执行扫描，不复制输出清单。

扫描后还需按任务补读原始事实文件，不得只凭脚本摘要下结论：

| 主题 | 事实来源 |
| --- | --- |
| 持久化公共字段、DDL、DAO/仓储基类语义 | `common/common-infra/README.md` + 对应源码 |
| 统一响应体、MVC 基类边界 | `common/common-web/README.md` + 对应源码 |
| 依赖版本与模块聚合 | 根 `pom.xml`、各模块 `pom.xml` |
| 分层 / 子域隔离 / 契约规则 | `common/common-test/.../GlobalArchitectureTest.java` |
| 运行时配置（数据源、XML 扫描、填充器） | `runner/standalone-runner/src/main/resources/` + runner 源码 |
| 领域术语 | `docs/domain/glossary.md`（新业务概念同次改动补术语表） |

## 2. 复用矩阵（业务需求 → 必须复用的通用组件）

业务代码只能在下表「业务侧产出」列写自有代码，其余能力一律复用通用组件；扫描确认基类存在后才可继承，不存在时按第 4 节处理漂移，**不得自行新建平行基类替代**。

| 业务需求 | 必须复用（common-* / share） | 业务侧产出 |
| --- | --- | --- |
| 持久化对象 | `infra.entity.BaseEntity`（公共 8 列不重复声明） | `XxxEntity`，放 `port.infra.entity`，优先 codegen 生成 |
| MyBatis Mapper | `infra.mapper.BaseMapper` | `XxxMapper`，放 `port.infra.mapper` |
| DAO 通用读写 | `infra.dao.BaseDao` / `BaseDaoImpl`（`IRepository` 能力 + `ID`/`IDCollection` 访问） | `XxxDao` / `XxxDaoImpl`，特有查询用 `getBaseMapper()` + Wrapper 扩展 |
| 仓储接口 | `common.app.repo.BaseRepository<D>`（最小通用读写，查询方法语义见 `kacean-boot-naming`） | `XxxRepository`，放 `app.repo`，只加业务特有方法 |
| 仓储实现 | `infra.repo.BaseRepositoryImpl`（父类按泛型注入 DAO + 转换器） | `XxxRepositoryImpl` 子类，特有查询用 `dao()`/`converter()`，转换用 `fromDomain()`/`toDomain()`；保存返回由 Entity 重新转换、包含主键/版本号的聚合 |
| 领域 ↔ Entity 转换 | `infra.repo.BaseConvert<DOMAIN, ENTITY>`（MapStruct） | `XxxConvert` + `@Mapper(componentModel = "spring")`，放 `port.infra.convert` |
| 条件 / 分页查询 | app 侧 `Query`、`PageQuery`/`PageResult`/`Sort`；infra 侧 `WrapperBuilder` / `Operator` / `ColumnResolver`；普通 QP 字段自动 EQ | `XxxQueryParam`（普通类，继承 `Query`）；DAO 按需覆写 `declareProperties` / `declareWheres`，特殊条件用 `addWhere`，复杂条件用 `where(...)` + `markProcessed(...)` |
| 标识与批量 | `common.app.identity.ID` / `IDCollection`（二选一标识、去重、上限 1000） | 不自建标识值对象替代通用场景 |
| Controller / 响应 | `common-web` `BaseDTO`、`CurdController`/`DddController`、`CurdAssembler`/`DddAssembler`、`Result` | 贫血 CRUD 继承 CURD 基类，DDD 继承 DDD 子类；通用端点只在 `CurdController` 定义，业务类只扩展特有端点与转换 |
| 领域标记 / 可选用例基类 | `common.app.domain.BaseDomain`；`common.app.usecase.BaseCase<D extends BaseDomain>`（可选，签名见 `kacean-boot-build` §2） | 领域对象保持纯 Java；`XxxService` 与复杂用例不强制继承 `BaseCase` |
| 跨子域调用 | 对方 `<子域>-contract` 的 `XxxFacade` / `XxxView` / 管理契约 | 提供方在 `app.service.facade` 实现 `XxxFacadeImpl`；其他契约按 `kacean-boot-share` 接入 |
| 登录上下文与身份环境适配 | `logincontext-contract` 的 `LoginContextManager` / `LoginContextProvider`，语义见 `kacean-boot-share` | 消费方注入 `LoginContextManager`；Web Provider 放 `port.web.provider`，不新建平行登录上下文或 Provider SPI |
| 跨业务稳定共享类型 | `share/core` 既有类型 | 新共享类型先进 `share/core`，不在业务间互相 import |
| 工具方法 | `common-util` 聚合的 Guava / Apache Commons | 不为第三方工具再包一层无价值工具类 |

## 3. 收敛红线（违反即返工）

1. **不重复造轮子**：不自写通用 CRUD 基类、分页模型、统一响应体、标识类型、Entity ↔ 领域转换"新约定"；发现通用组件缺失能力时，先报告并由用户决定是否下沉到 common-*，不在业务域私设平行实现。
2. **持久化技术不越层**：MyBatis-Plus 类型（Wrapper、`IRepository`、Entity）不得进入 `app` 仓储接口与领域对象；不引入 `mybatis-plus-spring` 的 `IService/ServiceImpl`，不引入 JPA/Hibernate。
3. **依赖方向不越界**：`port → app`、`deploy → 业务模块`；app 不依赖 port，infra 不依赖 web；跨子域只依赖对方 contract，不依赖对方 app/port。
4. **调用链路按复杂度收敛**：简单 CRUD 允许 `web → infra` 直调（贫血阶段）；多步编排、跨仓储协作、独立事务/权限边界必须进入 `app` 的 `XxxService`，复杂用例再拆 `service/usecase`；`FacadeImpl` 始终经由 `XxxService`。
5. **批量与事务语义不夸大**：`BaseRepository.saveAll/removeByIds` 不默认保证原子性，需要整体提交/回滚时由调用方建立事务边界；`BaseDao` 批量保存与更新使用 MyBatis-Plus 批量能力，事务以实际方法注解和 Spring 代理边界为准，不因方法名推定整体原子性。`IDCollection` 批量标识上限 1000，空集合不访问数据库。
6. **公共字段不重复**：`id`/`external_no`/`deleted`/审计字段/`version` 由 `BaseEntity` 统一映射，业务 Entity 与 DDL 不得另起策略（如自增主键、String UUID）。
7. **不虚构事实**：写入代码或文档的每个类、方法、配置项必须能在源码中找到；「runner 已提供某处理器」「某基类已有某方法」等断言必须有代码证据。

## 4. 开发流程与漂移同步

1. 读 `docs/domain/glossary.md`，执行第 1 节扫描，确认目标子域现状（是否已有模块、Entity、仓储）。
2. 按复用矩阵列出本任务要继承/复用的组件，逐个在源码中确认签名（泛型参数、构造器、受保护方法）。
3. 需要按表生成代码时优先走 `kacean-boot-codegen`，明确选择 `anemic` 或 `ddd`；再按 `kacean-boot-app` / `kacean-boot-naming` / `kacean-boot-javadoc` 实现生成器不承载的特有业务产出。
4. 验证：`mvn -pl business/<域>/<域>-port -am test` 与 `mvn -pl business/deploy/<域>-deploy -am test`（ArchUnit 规则数与扫描结果一致，不为 0）；结构或依赖变化再按 `kacean-boot-build` 做标准验证。
5. **漂移同步**：扫描发现通用组件与某个 skill 描述不一致时，切换 `kacean-boot-skill-sync` 执行同步（以代码为准、最小化更新），并在总结中列出同步点；`common-infra` 等模块自带 README 优先作为字段级细节来源，skill 不复制易过期清单。
6. **代码疑似错误只报告**：扫描发现的是代码缺陷（如引用不存在的类、桩实现未完成）而非文档漂移时，默认只在总结中报告，不顺手修改通用组件代码；用户明确要求修复时再改。

## 5. 完成判定

- 复用矩阵中每一行都有明确去向：复用了哪个通用组件，或说明了为何不适用。
- 无第 3 节红线项；ArchUnit 与目标模块测试通过。
- 新业务概念已补 `docs/domain/glossary.md`；发生的 skill 漂移已同步并列出。
- 总结包含：扫描发现的组件变化、复用点清单、验证命令与结果。
