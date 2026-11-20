---
name: power-system-build
description: power-system 项目构建、模块与依赖调整、thin jar 分发、启停日志和标准验证指南。适用于构建打包、新增业务模块、调整 common-web/common-app/common-infra/common-util/common-codegen/share（core 与各子域 contract）、修改运行资源或排查构建启动问题。
---

# power-system 构建与基础架构

执行前先核对实际目录和 POM；用户已调整结构时以实际项目为准。根 `README.md` 保存完整架构与使用说明，根 `pom.xml` 是依赖版本的唯一事实来源，本文件不复制易过期的版本清单。`share` 的共享内核与子域契约边界见 `share/README.md` 与 `share/core/README.md`，本 Skill 不复制字段、枚举或构造器清单。

## 1. 不可破坏的基线

- Java 21、Maven 多模块、UTF-8。Spring Boot、Guava 和 Apache Commons 的版本统一由根 POM `dependencyManagement` 管理；被 BOM 托管的依赖不写版本，项目内模块依赖使用 `${project.version}`。
- 顶层仅 `runner`、`common`、`business`、`share`。目录名与 artifactId 一致；`<modules>` 只负责聚合，`<dependencies>` 决定构建顺序和运行时内容。
- 使用 **thin jar + assembly 外置依赖**。禁止 Spring Boot `repackage` 和 assembly `jar-with-dependencies`。
- 日志目录固定为 `.logs/`；bat 文件必须纯 ASCII；sh 启动脚本保留 Cygwin 路径转换，并以 `APP_HOME` 为工作目录。
- Windows 直接执行的 `.ps1` 必须兼容 Windows PowerShell 5.1；含中文时保存为 UTF-8 BOM，避免 5.1 按 ANSI 误读。

## 2. 当前模块与依赖边界

```text
power-system
├── runner/standalone-runner          启动、运行资源、assembly 分发
├── common
│   ├── common-test                   ArchUnit 基类与测试配置，仅 test scope
│   ├── common-web                    最小 Spring MVC + 通用 MVC 基类 + 统一请求/响应体
│   ├── common-app                    Spring DI + 通用应用层基类
│   ├── common-infra                  基础设施公共代码与依赖
│   ├── common-util                   高频 Guava / Apache Commons 工具集
│   └── common-codegen                开发期业务模块脚手架与 MyBatis-Plus 分层代码生成器，不进入业务运行时
├── business
│   ├── ticket/ticket-app             parent=ticket；依赖 common-app，独立应用模块
│   ├── ticket/ticket-port            parent=ticket；web 与 infra 同模块，依赖 common-web、common-infra、ticket-app
│   ├── system/system-app             parent=system；依赖 common-app 与 logincontext-contract，独立应用模块
│   ├── system/system-port            parent=system；web 与 infra 同模块，依赖 common-web、common-infra、system-app 与 provided 的 jakarta.servlet-api
│   └── deploy
│       ├── ticket-deploy             parent=deploy；组合 ticket-app/port
│       └── system-deploy             parent=deploy；组合 system-app/port
└── share                             纯 Java 共享与契约聚合，按需显式依赖
    ├── core                          跨业务共享内核
    ├── logincontext-contract         logincontext 子域契约：登录上下文、管理契约与 Provider SPI
    └── tickettype-contract           tickettype 子域对外契约（Facade/DTO/事件）
```

- `common-web/common-app/common-infra/common-util/common-codegen` 均为普通 jar 模块，不作为业务 parent；其中 `common-codegen` 只由开发期脚本直接启动，不被业务或 runner 依赖。
- `common-web` 直接依赖 `spring-webmvc`、`common-infra`、`common-app`、`jackson-annotations` 与 `swagger-annotations-jakarta`（后两者仅注解包），提供 `BaseDTO`、`CurdController<DTO, ENTITY, CURD>` 通用 CRUD 基类及其子类 `DddController<DTO, DOMAIN, ENTITY, CURD>`、`CurdAssembler<DTO, ENTITY>` 及其子类 `DddAssembler<DTO, DOMAIN, ENTITY>`、统一响应体 `Result`（`common.web.response`）。通用端点只在 `CurdController` 声明，DDD 子类只增加领域泛型与转换入口，不得加入 Boot Starter、Tomcat 或 Jackson 序列化运行时；`Result` 的响应码与序列化边界见 `common/common-web/README.md`。`standalone-runner` 通过 `spring-boot-starter-webmvc` 提供嵌入式容器、Jackson 和 Boot MVC 自动配置，并通过 `springdoc-openapi-starter-webmvc-ui` 提供 Swagger 文档（`/swagger-ui/index.html`、`/v3/api-docs`）；当前 runner 未提供全局异常处理器，后续新增时应基于 `Result` 放在 runner 侧，不下沉到 common-web。
- `common-app` 只提供 `spring-context` 与通用应用层类型：领域标记接口 `BaseDomain`（`common.app.domain`）、可选用例基类 `BaseCase<D extends BaseDomain>`（`common.app.usecase`，构造器接收 `BaseRepository<D>` 并提供受保护的 `getRepository()`，不是标记接口）、通用仓储接口 `BaseRepository`（`common.app.repo`，仅定义最小读写能力，查询方法语义见 `power-system-naming`），以及标识模型 `ID`/`IDCollection`（`common.app.identity`）、业务查询参数基类 `Query` 与 `PageQuery`/`PageResult`/`Sort` 等查询模型（`common.app.query`）；业务 QP 继承 `Query`，统一关键字与不可变排序列表，`PageQuery` 组合业务查询参数、页码与页大小。领域模型保持纯 Java，不定义持久化实体基类或应用服务基类；审计字段由 infra 的持久化 `BaseEntity` 声明，填充机制由运行侧按需配置。具体持久化实现放在 port/deploy/runner，不放入 app。
- `common-infra` 依赖 common-app、`mybatis-plus-extension`（传递 core，提供 `IRepository`/`AbstractRepository`）与 `spring-tx`，不引入 `mybatis-plus-spring`、Starter、数据源或 JDBC 驱动。`com.kacean.power.common.infra.entity.BaseEntity` 是带 MyBatis-Plus 注解的持久化对象基类，与 app 的纯 Java 领域模型（`BaseDomain`）不同，不得放入或用于 `app.domain`；公共字段映射与 DDL 规范见 `common/common-infra/README.md`。公共基类按包划分：
  - `infra.mapper.BaseMapper`：业务 MyBatis Mapper 统一父接口；`mapper` 包只承载 MyBatis Mapper。
  - `infra.dao.BaseDao` / `BaseDaoImpl`：基于 `IRepository` / `AbstractRepository`（不引入 `IService/ServiceImpl`），`@Autowired` 字段注入业务 Mapper（按子类泛型解析，子类无需构造器）；通用 CRUD 与 `QueryWrapper`/`LambdaQueryWrapper` 条件查询由 `IRepository` 提供，另增 `ID`/`IDCollection` 访问方法（`id` 主键与 `code` 外部编号二选一，批量条件 OR 组合、空集合短路）；批量保存与更新使用 MyBatis-Plus 批量能力；事务以实际方法注解和 Spring 代理边界为准，不因方法名推定整体原子性。`saveOrUpdate` 按 BaseEntity 的 Long id 判定插入或更新；`BaseRepository.saveAll/removeByIds` 不默认保证原子性，需要整体提交或回滚时由调用方建立事务边界。
  - `infra.query.WrapperBuilder` + `Operator` + `ColumnResolver`：携带当前 QP 与 Entity 类型；DAO 的 `declareProperties` 声明 SELECT、关键字和固定排序，`declareWheres` 通过 `addWhere` 声明特殊条件；`BaseDao.toWrapper` 按序调用两个钩子及 `processDefaultFields()`（普通字段默认 EQ）、`processKeyword()`、`processOrder()`，`list(Query)` / `page(PageQuery)` 直接复用。字段和排序统一使用 Entity Java 属性，由 ColumnResolver 安全映射数据库列。
  - `infra.repo.BaseConvert<DOMAIN, ENTITY>`：MapStruct 双向转换契约，使用 `fromDomain`/`toDomain`，独立于 MyBatis Mapper；业务转换接口仍放 `port.infra.convert`。
  - `infra.curd.BaseCurd`：`@Autowired` 字段注入 `BaseDao` 的通用 CRUD 委托基类（子类无需构造器）；部分批量写方法声明 `@Transactional(rollbackFor = Exception.class)`，并非所有同名批量方法都有事务注解，具体以实际方法为准。
  - `infra.repo.BaseRepositoryImpl`：实现 common-app 的 `BaseRepository`，`@Autowired` 字段注入 `BaseDao` 与 `BaseConvert`（子类无需构造器），保存后返回由持久化 Entity 重新转换、包含主键与版本号的聚合；特有查询通过受保护的 `dao()`/`converter()` 扩展。
  具体业务 DAO、转换器与仓储仍在各自 port 模块扩展，MyBatis-Plus 类型不得进入 app 层仓储接口。持久化 Provider 由实际运行模块配置。
- 不引入 JPA/Hibernate；ticket（tickettype）子域当前已有 `TicketTypeEntity`、`TicketTypeMapper` 与 XML 持久化代码，尚无 app 领域对象、仓储或应用服务实现。登录上下文契约、Provider 放置、运行时语义与开发期模拟边界见 `power-system-share` §4。`standalone-runner` 已统一引入 MyBatis-Plus Boot 4 Starter 和 MySQL 运行时驱动，`application.yml` 通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 配置数据源，真实密码不得提交。`common-codegen` 的元数据驱动仅供开发期使用；Starter、JDBC 驱动和数据源配置只放 runner，不得下沉到 app。
- maven-compiler-plugin 版本由根 POM pluginManagement 统一管理，不得回退到 Maven 默认绑定的旧版本。
- 依赖方向：`port → app` 与 `deploy → 业务模块`；app 不依赖 port。**DDD 前期贫血模式下，port 内 web 可直接调用 infra**（简单 CRUD 不绕道 app），infra 不得反向依赖 web；业务复杂度上升后才由 app 承载用例编排，此时 web 改为调用 app 应用服务。
- `share/core` 只放稳定、无单一业务归属的跨业务共享领域类型，不依赖其他项目模块或框架。
- `share/<子域>-contract` 放该子域对外契约（`XxxFacade`、DTO、领域事件，以及按需定义的管理契约、上下文对象、Provider SPI 与契约异常），纯 Java，可按需依赖 `share/core`，不得依赖业务实现模块；`XxxFacade` 由提供方 app 以 `XxxFacadeImpl` 实现，Provider 与管理契约实现按适配或运行职责放置，消费方只依赖 contract 模块。
- `share` 下模块只有被显式依赖才进入分发包，不自动依赖到所有业务，也不用于堆放普通工具类。
- `standalone-runner` 的业务实现只通过各业务 deploy 模块引入，不写业务代码；Web、文档和数据库等运行时技术依赖由 runner 自行声明。跨业务调用不得依赖其他业务的 port 模块；稳定共享类型放入 share/core。
- 新业务两子模块继承各自业务聚合 POM；deploy 子模块继承 `business/deploy`。不要把业务 `<modules>` 移入 deploy。

## 3. 代码归属

模块归属如下。**包名、类名、方法命名细则以 `power-system-naming` 为单一来源**，本节只保留模块 / POM 视图，不复制包名表，避免双份维护漂移。

| 代码 | 模块 |
| --- | --- |
| Controller、DTO、assembler、Web Provider 适配、MVC 配置 | `<域>-port`（`port.web` 包） |
| 应用服务、command/query、领域模型、领域服务、仓储接口 | `<域>-app` |
| 子域间调用契约（Facade、DTO、事件、管理契约、上下文、Provider SPI、契约异常） | `share/<子域>-contract` |
| 跨业务稳定共享领域类型 | `share/core` |
| 仓储实现、持久化适配、外部客户端 | `<域>-port`（`port.infra` 包） |
| 启动主类 | `standalone-runner`（`com.kacean.power.PowerSystemApplication`） |

app 直接使用 `@Service`/`@Component` 等 Spring DI 注解，优先构造器注入；领域模型为纯 Java 对象，不使用 JPA 等持久化框架注解。空包用 `.gitkeep` 占位。

## 4. 添加依赖

- `common-util` 默认聚合 Guava、commons-lang3、commons-collections4、commons-io、commons-codec、commons-text；只有需要多类高频工具时才依赖它。
- 只使用一个工具时直接声明该工具；commons-beanutils 等低频工具不放入 `common-util`，由实际使用方按需声明。版本仍由根 POM 管理。
- 不为第三方工具库再包装一层无实际价值的项目工具类，不让其他 common 模块强制依赖 `common-util`。
- 双 BOM 未管理的外部依赖，应先在根 POM `dependencyManagement` 统一版本，再由使用模块声明。
- 修改依赖后检查直接依赖与传递依赖，不能只看 POM：

```bash
# Reactor 内模块尚未 install 时，保留 compile，确保依赖可解析
mvn -pl business/ticket/ticket-port -am compile dependency:tree
mvn -pl common/common-util dependency:tree
```

## 5. 构建与架构测试

```bash
mvn clean package                                   # 全量构建与分发包
mvn clean package -pl runner/standalone-runner -am # 启动模块及依赖
mvn clean package -DskipTests                      # 明确跳过测试时才使用
mvn test -pl business/deploy/ticket-deploy,business/deploy/system-deploy -am # 当前全部部署模块的架构测试
```

分发产物：`runner/standalone-runner/target/standalone-runner-1.0-SNAPSHOT.zip`；模块自身 jar 均为 thin jar。

ArchUnit 守护 Java 代码依赖方向、子域分包隔离与子域契约约束，**不验证 Maven parent、POM 白名单或模块聚合关系**，这些需结合 POM 审查和 `dependency:tree` 检查。

全局规则位于 `common-test` 的 main 源码，配置为其 main resources 下的 `archunit.properties`（`archRule.failOnEmptyShould=false`）。部署模块以 test scope 依赖 common-test；子类必须显式声明 `@AnalyzeClasses`，父类 `@ArchTest` 字段会自动执行。架构测试后确认报告为当前 **7 条规则**（2 条分层依赖 + 2 条子域分包 + 3 条子域契约），不能是 0 条。分层只强制「app 不依赖 port」「infra 不依赖 web」，**不禁止 web 直调 infra**（贫血阶段允许）。新增全局规则后同步更新此处数量与根 README。

代码注释（中文类 Javadoc、`@author kacean-agent`、record `@param`、契约方法注释）遵循 `.agents/skills/power-system-javadoc/SKILL.md`；包、类、方法命名（子域分包、`XxxFacade`/`XxxRepository`、用例动词服务、中文测试方法名）遵循 `.agents/skills/power-system-naming/SKILL.md`。

## 6. 标准验证

构建、依赖、结构、打包、脚本或日志改动后执行。Windows / DSH 使用 PowerShell 5.1+（首行仅放宽当前进程的执行策略，不修改系统设置）：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& .agents/skills/power-system-build/scripts/verify.ps1 -Port 8080
```

Git Bash / Linux / macOS：

```bash
sh .agents/skills/power-system-build/scripts/verify.sh        # 默认 8080，可传端口参数
```

两套脚本均自动完成全量构建、解压、启动、断言和清理，失败也会清理。断言包括：构建成功、出现 `Started PowerSystemApplication`、`/`、`/index.html`、Swagger UI 与 OpenAPI JSON 返回 200、生成 `.logs/power-system.log`，最后停止应用并删除 `target/dist-verify`。

## 7. 运行资源与分发约定

- assembly 描述符为 `runner/standalone-runner/assembly.xml`；新增脚本或资源必须放入 `src/main/resources/` 并在描述符中登记。
- 主配置只维护 `src/main/resources/application.yml`，assembly 自动复制为分发包 `conf/application.yml`，不要另建独立源文件。
- 日志配置为 `src/main/resources/conf/logback-spring.xml`；静态资源为 `src/main/resources/web/`，分发包外置 `web/` 优先于 jar 内资源。
- 分发包包含 `bin/`、`conf/`、`web/`、`lib/`；`.logs/` 与 `run/` 在运行时创建。业务日志为 `.logs/power-system.log`，启动控制台日志为 `.logs/standalone-runner.log`，归档到 `.logs/archive/`。
- 启动入口为 `bin/startup.sh` 或 `bin/startup.bat`；停止使用对应 stop 脚本。不得移除脚本中的 `cd "$APP_HOME"` 或 Windows 工作目录设置，否则外置 web 和相对日志路径会失效。

## 8. 常见任务检查清单

### 新增业务模块

可优先使用 `power-system-codegen` 的 `module` 命令创建 app/port/deploy 脚手架；无论自动生成还是手工创建，都按以下清单检查。

1. 在 `business/<域>/` 建立 app/port 两个子模块，artifactId 带业务前缀，parent 均为该业务聚合 POM，`relativePath=../pom.xml`。
2. `<域>-app` 依赖 `common-app`；`<域>-port` 依赖 `common-web`、`common-infra`，再添加 `port → app`。MyBatis-Plus 与 Spring MVC 编译 API 由 common 模块传递提供，无需重复声明；直接使用 Servlet API 等可选编译 API 时仍由实际使用模块显式声明，MVC 容器与运行依赖由 runner 提供。
3. 在 `business/pom.xml` 注册业务，在 `business/deploy/` 新建 `<域>-deploy` 并注册，组合 app/port 两个业务模块。
4. 若子域需对外提供契约：在 `share/` 下新建 `<子域>-contract`（纯 Java jar，parent=share）并注册到 `share/pom.xml`；Facade、Provider SPI 与管理契约的实现位置按 `power-system-share` 执行，消费方只依赖该 contract 模块。
5. 在 `standalone-runner` 添加该 deploy 依赖，为 deploy 添加 ArchUnit 子类和 common-test 测试依赖。
6. 执行标准验证，并同步根 README 与本 Skill 中发生变化的约束。

### 排查构建或启动

- `Child module ... does not exist`：先核对 `<modules>` 与实际目录，不凭记忆重建已移除模块。
- 缺少 `ServletWebServerFactory`：检查 runner 的 `spring-boot-starter-webmvc`，不要把 Starter 加回 common-web。
- Controller 可直接使用 Spring MVC 注解；若直接使用 Servlet API、Jackson 注解等可选能力，由实际代码所在模块显式补充对应 API。
- 静态资源或日志路径异常：检查启动工作目录、assembly 输出路径和 `spring.web.resources.static-locations`。
