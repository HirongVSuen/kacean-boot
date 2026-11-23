# kacean-boot

`kacean-boot` 是一个基于 Java 21 的 Maven 多模块单体后端工程。项目通过 `runner`、`common`、`business`、`share` 四个顶层模块组织代码，业务模块按 `app / port` 组织：`app` 是独立应用模块，只使用 Spring DI 与通用应用层基类，不绑定持久化技术；`port` 同时承载 `web`（Spring MVC）与 `infra`（持久化和外部系统适配）两个包，并使用独立的 `deploy` 模块组合部署依赖。**DDD 前期贫血模式下 `web` 可直接调用 `infra`**，简单 CRUD 不绕道 `app`；业务复杂度上升后才由 `app` 的应用服务承载用例编排。

工程采用 **thin jar + 外置 runtime 依赖** 的分发方式，最终生成包含应用、依赖、配置、静态资源和启停脚本的 ZIP 分发包。

## 文档

- [领域术语表](docs/domain/glossary.md)：业务术语、代码命名与禁用叫法的唯一来源，涉及业务命名或领域建模前先阅读。
- [业务代码生成](.agents/skills/kacean-boot-codegen/SKILL.md)：开发期业务模块脚手架，以及按子域生成贫血或 DDD 模式的 Entity、Mapper、XML、DAO/CURD、Web 与领域/仓储代码。
- [持久化公共字段 DDL](common/common-infra/README.md)：`BaseEntity` 主键策略、公共列类型与 MySQL 示例表结构。
- [统一响应体](common/common-web/README.md)：`Result` 响应码、字段错误与序列化边界。

## 技术基线

| 项目 | 当前版本或约定 |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2，由 `standalone-runner` 提供 Boot MVC 自动配置与嵌入式容器，通过 BOM 管理版本 |
| 接口文档 | springdoc-openapi 3.2.0，由 `standalone-runner` 提供 Swagger UI 与 OpenAPI JSON |
| MyBatis-Plus | 3.5.18，通过 `mybatis-plus-bom` 统一管理版本；`standalone-runner` 引入 Boot 4 Starter 与 MySQL 运行时驱动，`common-codegen` 仅含开发期生成器与元数据驱动 |
| 持久化 | runner 已提供 MySQL 基础设施，业务持久化适配由对应 `port` 实现，不使用 JPA |
| 项目坐标 | `com.kacean.boot:kacean-boot:1.0-SNAPSHOT` |
| 构建工具 | Maven 多模块 |
| 分发打包 | `maven-assembly-plugin` 3.8.0 |
| 架构测试 | ArchUnit 1.3.0 + JUnit 5 |
| 工具库 | 根 POM 统一版本，`common-util` 聚合高频工具，低频工具按需引入 |
| 应用默认端口 | 8080 |

## 项目结构

```text
kacean-boot/
├── pom.xml                              根 POM：聚合顶层模块、管理统一版本与插件
├── runner/                              启动器聚合层，不包含业务代码
│   └── standalone-runner/               单体启动入口、运行资源与分发打包
├── common/                              公共层，不包含业务代码
│   ├── common-test/                     全局 ArchUnit 规则与共享测试配置（jar）
│   ├── common-web/                      最小 Spring MVC、通用 MVC 基类与统一请求/响应体（jar）
│   ├── common-app/                      Spring DI 与通用应用层基类（jar）
│   ├── common-infra/                    基础设施公共代码与依赖（jar）
│   ├── common-util/                     Guava 与 Apache Commons 工具集（jar）
│   └── common-codegen/                  开发期 MyBatis-Plus 代码生成器，不进入业务运行时（jar）
├── business/                            业务聚合层
│   ├── ticket/                          工单业务模块
│   │   ├── ticket-app/                  应用服务、领域模型（纯 Java）、仓储接口（独立模块）
│   │   └── ticket-port/                 web（Controller、DTO、assembler）+ infra（仓储实现、外部客户端）
│   ├── system/                          系统业务模块
│   │   ├── system-app/                  应用服务、领域模型（纯 Java）、仓储接口（独立模块）
│   │   └── system-port/                 web（Controller、DTO、assembler）+ infra（仓储实现、外部客户端）
│   └── deploy/                          部署聚合层
│       ├── ticket-deploy/               组合 ticket-app/port，供 runner 依赖
│       └── system-deploy/               组合 system-app/port，供 runner 依赖
└── share/                               共享与契约聚合层（纯 Java，按需显式依赖）
    ├── core/                            跨业务共享内核：稳定共享领域类型（原 core-share）
    ├── logincontext-contract/            logincontext 子域契约：登录上下文、管理契约与 Provider SPI
    └── tickettype-contract/             tickettype 子域对外契约（Facade/DTO/事件）
```

`common-web`、`common-app`、`common-infra`、`common-util` 均为普通 jar 依赖模块，不作为业务模块的 parent。`common-web` 依赖 `spring-webmvc`、`common-infra`（provided）、`common-app` 与 `swagger-annotations-jakarta`（仅注解包），承载通用 MVC 能力，现有通用 CRUD 基类 `CurdController` 及其 DDD 子类 `DddController`、MapStruct `CurdAssembler` / `DddAssembler`、统一响应体 `Result`；通用端点只在 `CurdController` 定义，DDD 子类增加领域泛型与转换入口；`common-app` 提供 Spring Context DI 与通用应用层类型，现有领域标记接口 `BaseDomain`、用例标记接口 `BaseCase`、通用仓储接口 `BaseRepository`，以及 `ID`、`IDCollection`、`Query`（业务 QP 基类，统一关键字与不可变排序列表）、`PageQuery`（组合业务查询参数、页码与页大小）、`PageResult`、`Sort` 等查询模型；`common-infra` 集中基础设施适配的公共代码与依赖，提供带 MyBatis-Plus 注解的持久化 `BaseEntity`、基于 `mybatis-plus-extension` 的 `IRepository` / `AbstractRepository` 扩展的 `BaseDao/BaseDaoImpl`（`@Autowired` 字段注入业务 Mapper，子类无需构造器，支持 `QueryWrapper` / `LambdaQueryWrapper`，不引入 `mybatis-plus-spring`）、MyBatis 基础 `mapper.BaseMapper`、`repo` 包的 MapStruct `BaseConvert` 转换契约（业务转换接口仍放 `port.infra.convert`）、`query.WrapperBuilder` / `Operator` / `ColumnResolver` 查询构建组件（`BaseDao.toWrapper` 依次执行 DAO 的 `declareProperties` / `declareWheres` 钩子与默认字段 EQ、关键字、排序处理，`list(query)` / `page(pageQuery)` 直接复用），以及委托基础 DAO 的 `BaseCurd` 与 `BaseRepositoryImpl`；`common-util` 聚合常用工具库。`common-test` 仅以 `test` scope 被部署模块依赖，不进入运行时 `lib/`。`common-codegen` 提供交互式/命令行代码生成，默认 `anemic`，可选 `ddd`；输出位于目标业务子域的 `port.web`、`port.infra`，DDD 另输出 `app.domain` / `app.repo`，不作为业务或启动模块依赖。

`standalone-runner` 当前未提供全局异常处理器；后续如需统一异常响应，应基于 `common-web` 的 `Result` 在 runner 侧实现，不下沉到 `common-web`。

业务 `app/port` 子模块的 parent 是对应业务聚合模块（当前为 `ticket`、`system`），再通过 Maven dependency 引入相应的 `common-*`。继承只负责项目构建配置，分层公共能力通过依赖传递。

`share` 是共享与契约聚合层：`share/core` 只放稳定、无单一业务归属的跨业务共享领域类型（原 `core-share`）；`share/<子域>-contract` 放对应子域的对外契约（`XxxFacade` 接口、DTO、领域事件与按需定义的 Provider SPI），由提供方子域在 app 层实现、消费方按需显式依赖。两者均为纯 Java jar，不依赖业务实现或框架，未被依赖时不会进入分发包。详细边界见 [`share/README.md`](share/README.md)。

领域模型为纯 Java 对象，不使用 JPA 等持久化框架注解。持久化适配由 `port` 或部署侧按需配置。ticket 子域（`tickettype`）当前已有票种类型贫血 CRUD 的 Web 与持久化代码，尚无 app 领域对象、仓储或应用服务实现；登录上下文契约独立为 `share/logincontext-contract`（`com.kacean.boot.logincontext.contract`，含 `LoginContext`、`LoginContextManager` 与 `LoginContextProvider`）；`LoginManager` 位于 standalone-runner，作为运行时编排组件创建同一上下文实例，编排 Spring Bean 或 Java SPI Provider 完成填充。loginaccount 子域承载登录账号贫血 CRUD，并提供该 SPI 的 `LoginAccountContextProvider` 实现：开发期通过请求头 `X-Login-Account` 模拟登录，查询启用账号填充上下文，尚未接入认证设施。

### 依赖方向

```text
standalone-runner
└── <域>-deploy（当前：ticket-deploy、system-deploy）
    ├── <域>-port ────────> <域>-app
    │   ├── common-web（最小 Spring MVC + 通用 MVC 基类 + 统一请求/响应体）
    │   └── common-infra
    └── <域>-app ─────────> common-app（Spring DI + 通用应用层基类）
```

必须遵守以下约束：

- 同业务依赖方向为 `port → app`；**port 模块内 `web` 可直接调用 `infra`**（贫血阶段简单 CRUD 不强制经过 app），`infra` 不得反向依赖 `web`，`app` 不依赖 `port`。业务模块可按需依赖 `share/core` 与所需的 `share/<子域>-contract`，`share` 下模块不反向依赖任何业务或公共技术模块。
- 业务代码按 `com.kacean.boot.<子域>.app` 与 `com.kacean.boot.<子域>.port.<web|infra>` 组织（如 `tickettype/app`、`tickettype/port/web`、`tickettype/port/infra`），子域名全局唯一且 app 与 port 包一致；层根包下不得直接建类，需再按 `controller/service/domain` 等技术分包细分；不同子域的实现包之间不能调用（同层、跨层均禁止）。该约束由 `common-test` 的 ArchUnit 规则强制。
- 子域间同步调用经由 `share/<子域>-contract` 模块的 `<子域>.contract` 契约包：契约以 `XxxFacade` 接口命名（DTO、领域事件同包），提供方在 `app` 层对应子域以 `XxxFacadeImpl` 实现，消费方只注入契约接口；契约包不得依赖任何 `app/port` 实现。`share/core` 只放跨业务共享领域类型，不承担子域契约。
- `standalone-runner` 只依赖部署子模块，不承载业务代码。
- `web` 只使用 Spring MVC；`common-web` 不依赖 Boot Starter、Tomcat 或 Jackson。Swagger 只引入 `swagger-annotations-jakarta` 注解包，文档运行时由 `standalone-runner` 的 `springdoc-openapi-starter-webmvc-ui` 提供，启动后访问 `/swagger-ui/index.html`（OpenAPI JSON 为 `/v3/api-docs`）。`app` 只使用 Spring DI 相关类型与通用应用层基类；具体 Web、数据访问和外部客户端实现不放入 `app`。
- `app` 不引入持久化框架与具体实现，持久化 Provider 与数据源（如有）由 `port` 或部署侧配置。
- 模块目录名必须与 `artifactId` 一致，业务子模块名称必须带业务前缀，例如 `ticket-port`。
- 业务 `app/port` 子模块继承对应业务聚合 POM；`app` 依赖 `common-app`，`port` 依赖 `common-web`、`common-infra` 与 `app`；部署子模块继承 `business/deploy`。
- 模块间依赖版本使用 `${project.version}`；Spring 等由 Spring Boot BOM 托管的依赖不显式声明版本，其余第三方依赖版本在根 POM `dependencyManagement` 统一管理。

### 代码归属

启动主类为 `com.kacean.boot.KaceanBootApplication`，默认扫描 `com.kacean.boot` 及其子包。业务代码建议按以下包结构组织：

| 代码类型 | 模块 | 建议包名 |
| --- | --- | --- |
| Spring MVC Controller、DTO、assembler、MVC 配置 | `<域>-port` | `com.kacean.boot.<子域>.port.web.{controller,dto,assembler,config}` |
| 应用服务、command/query、领域模型（纯 Java）、仓储接口 | `<域>-app` | `com.kacean.boot.<子域>.app.{service,domain,repository}` |
| 子域间调用契约（Facade 接口、DTO、领域事件） | `share/<子域>-contract` | `com.kacean.boot.<子域>.contract` |
| 跨业务稳定共享领域类型 | `share/core` | `com.kacean.boot.core.share` |
| 仓储实现、外部客户端、持久化适配 | `<域>-port` | `com.kacean.boot.<子域>.port.infra.{entity,mapper,dao,repo,convert,query,client,config}` |

其中 `<子域>` 为业务子域名，全小写且全局唯一（如 `tickettype`），同一子域在 `app` 与 `port` 中使用相同的分包名；Maven 业务模块仍按业务域聚合（`ticket-app/port`），包名不再包含业务域层级。跨子域调用示例：`ticketorder` 需要工单类型数据时，依赖 `share/tickettype-contract` 并注入 `tickettype.contract.TicketTypeFacade`，由 `tickettype/app` 的 `TicketTypeFacadeImpl` 提供实现，不直接 import `tickettype` 子域任何实现类。

### app 的 DI 与领域模型

`app` 直接使用 Spring DI。应用服务使用 `@Service` 或 `@Component`，依赖注入优先使用构造器；单构造器场景不需要额外添加 `@Autowired`。common 通用基类（`BaseDaoImpl`、`BaseCurd`、`BaseRepositoryImpl`、`CurdController`、`DddController`）统一在父类字段上使用 `@Autowired` 注入，由 Spring 按子类泛型解析，业务子类不再声明构造器。

```java
@Service
public class TicketApplicationService {
    private final TicketRepository ticketRepository;

    public TicketApplicationService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }
}
```

领域模型为纯 Java 对象，不使用 `@Entity` 等持久化框架注解。`app` 不处理 MVC，也不引入具体持久化实现。

### 工具库版本管理

根 POM 的 `dependencyManagement` 统一管理以下工具库版本，`common-util` 聚合其中的高频工具：

| 工具库 | 当前管理版本 |
| --- | --- |
| `com.google.guava:guava` | 33.8.0-jre |
| `org.apache.commons:commons-lang3` | 3.22.0 |
| `org.apache.commons:commons-collections4` | 4.6.0 |
| `commons-io:commons-io` | 2.22.0 |
| `commons-codec:commons-codec` | 1.23.0 |
| `org.apache.commons:commons-text` | 1.15.0 |
| `commons-beanutils:commons-beanutils` | 1.11.0，包含 CVE-2025-48734 修复 |

`common-util` 默认聚合 Guava、commons-lang3、commons-collections4、commons-io、commons-codec 和 commons-text。需要同时使用多类高频工具的模块显式依赖 `common-util`，不需要写版本号：

```xml
<dependency>
    <groupId>com.kacean.boot</groupId>
    <artifactId>common-util</artifactId>
    <version>${project.version}</version>
</dependency>
```

只使用单个工具时可以直接声明对应工具库；`commons-beanutils` 等低频工具不进入 `common-util`，确有需要时由使用方单独声明，版本仍由根 POM 管理。`common-util` 不被其他公共模块强制依赖，因此未使用工具集的模块不会携带这些工具库。

## 环境准备

- 安装并配置 JDK 21，确保 `java -version` 指向 21。
- 安装 Maven，确保命令行可以直接执行 `mvn`。
- 使用一键验证脚本时，还需要 Bash、`unzip` 和 `curl`。

## 构建

在项目根目录执行：

```bash
# 全量构建并运行测试，同时生成分发包
mvn clean package

# 跳过测试构建
mvn clean package -DskipTests

# 只构建启动模块及其依赖
mvn clean package -pl runner/standalone-runner -am
```

主要构建产物：

```text
runner/standalone-runner/target/standalone-runner-1.0-SNAPSHOT.jar
runner/standalone-runner/target/standalone-runner-1.0-SNAPSHOT.zip
runner/standalone-runner/target/standalone-runner-1.0-SNAPSHOT/
```

其中 jar 是 thin jar，不包含 runtime 依赖；实际部署请使用 ZIP 分发包，不要把它当作可独立运行的 fat jar。

## 本地运行

可以直接在 IDE 中运行：

```text
runner/standalone-runner/src/main/java/com/kacean/boot/KaceanBootApplication.java
```

启动后访问：

- 首页：<http://localhost:8080/>
- 静态页面：<http://localhost:8080/index.html>
- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

本地 IDE 启动默认加载 jar 内的 `application.yml` 与 `classpath:conf/logback-spring.xml`。

## 分发包运行

解压 `standalone-runner-1.0-SNAPSHOT.zip`，进入解压后的 `standalone-runner-1.0-SNAPSHOT/` 目录。

### Linux、macOS 或 Git Bash

```bash
sh bin/startup.sh       # 启动
sh bin/stop.sh          # 停止
sh bin/restart.sh       # 重启
sh bin/archive-logs.sh  # 手动归档历史日志
```

### Windows

```bat
bin\startup.bat
bin\stop.bat
bin\restart.bat
bin\archive-logs.bat
```

启动脚本会检查应用 jar、避免重复启动、创建运行目录，并在启动前自动执行日志归档。PID 文件位于：

```text
run/standalone-runner.pid
```

## 分发包结构

```text
standalone-runner-1.0-SNAPSHOT/
├── bin/                         startup/stop/restart/archive-logs 的 sh 与 bat 脚本
├── conf/
│   ├── application.yml          打包时从主 application.yml 复制
│   └── logback-spring.xml       外置日志配置
├── lib/
│   ├── standalone-runner.jar    应用 thin jar
│   └── *.jar                    全部 runtime 依赖
├── web/                         外置静态资源，可直接修改
├── .logs/                       运行期自动创建，不随包分发
└── run/                         运行期自动创建，保存 PID 文件
```

启动脚本通过 `java -cp "lib/*" com.kacean.boot.KaceanBootApplication` 加载应用及依赖，并通过 `--spring.config.additional-location` 和 `--logging.config` 加载外置配置。

## 配置与静态资源

### 修改源码配置

源码期只修改：

```text
runner/standalone-runner/src/main/resources/application.yml
runner/standalone-runner/src/main/resources/conf/logback-spring.xml
```

不要单独维护另一份源码 `conf/application.yml`；assembly 会在打包时自动复制主配置，保证 jar 内与分发包外置配置同源。

### 修改运行配置

解压分发包后，可直接修改：

```text
conf/application.yml
conf/logback-spring.xml
```

外置配置由启动脚本显式加载，优先于 jar 内配置。修改端口可调整 `server.port`，重启后生效。

### 持久化

`standalone-runner` 已引入 MyBatis-Plus Boot 4 Starter 和 MySQL 运行时驱动，数据源默认连接 `localhost:3306/kacean_boot`，可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量覆盖；真实密码不得写入配置仓库。`application.yml` 同时配置 Mapper XML 扫描、下划线转驼峰、`ASSIGN_ID` 主键策略和数值逻辑删除值；当前 runner 未配置 `MetaObjectHandler` 公共字段填充处理器，也不存在 `OperatorContext`，`common-infra` `BaseEntity` 中声明 `FieldFill` 的外部编号与审计字段需后续补充填充处理器才会自动赋值。业务持久化应在对应 `port` 新增 Entity、Mapper 与仓储适配，不改变 `app` 层仓储契约，也不使用 JPA。

### 静态资源

静态资源加载顺序为：

```yaml
spring:
  web:
    resources:
      static-locations: file:./web/,classpath:/web/
```

分发包外置 `web/` 优先，可直接修改而无需重新打包；目录不存在时由 jar 内 `web/` 兜底。

## 日志

- 应用同时输出到控制台和 `.logs/kacean-boot.log`。
- 文件按天滚动，单文件达到 50 MB 后切分。
- Logback 不自动删除历史日志。
- `archive-logs` 脚本将 30 天前的历史日志压缩到 `.logs/archive/`：sh 使用 gzip，bat 使用 ZIP。
- 启动前会自动归档一次，也可通过 cron 或 Windows 计划任务定期执行归档脚本。

## 架构测试

`common-test` 的 `GlobalArchitectureTest` 统一维护全局 ArchUnit 规则。`ticket-deploy` 与 `system-deploy` 分别通过 `ArchitectureTest` 继承并执行这些规则。

运行架构测试：

```bash
mvn test -pl business/deploy/ticket-deploy,business/deploy/system-deploy -am
```

当前全局规则共 **7 条**（2 条分层依赖 + 2 条子域分包 + 3 条子域契约）：

1. `app` 不依赖 `port`（web/infra）。
2. `infra` 不依赖 `web`；**不禁止 `web` 直调 `infra`**（贫血阶段允许，是否经过 `app` 由业务复杂度决定）。
3. 子域层根包必须按技术分包（`controller/service/domain` 等）。
4. 不同子域的实现包之间不能调用。
5. 契约包（`<子域>.contract`、`core.share`）不依赖业务分层实现。
6. `XxxFacade` 接口只能定义在 `<子域>.contract`。
7. `XxxFacadeImpl` 只能位于 app 层对应子域。

`app` 的公共技术依赖集中在 `common-app`，不再增加额外注解位置或框架规则。

新增部署模块的架构测试时，子类必须显式声明 `@AnalyzeClasses`，因为该注解不支持继承；父类中的 `@ArchTest` 字段会自动执行。检查测试报告时，应确认部署模块的 `Tests run` 为实际规则数，而不是 `0`。

共享配置 `archunit.properties` 设置了 `archRule.failOnEmptyShould=false`，因此骨架阶段空包不会导致规则失败。

## 一键构建与冒烟验证

在项目根目录执行：

```bash
sh .agents/skills/kacean-boot-build/scripts/verify.sh
```

脚本会自动完成：

1. 执行 `mvn clean package`。
2. 解压 ZIP 并启动应用。
3. 断言启动日志包含 `Started KaceanBootApplication`。
4. 断言 `/`、`/index.html`、Swagger UI 与 OpenAPI JSON 均返回 HTTP 200。
5. 检查 `.logs/kacean-boot.log`，随后停止应用并清理 `target/dist-verify`。

默认使用 8080 端口。无论验证成功或失败，脚本都会清理本次启动的应用和临时目录。

## 新增业务模块

以新增 `<域>` 为例：

1. 在 `business/<域>/` 下创建 `<域>-app`、`<域>-port` 两个子模块。
2. 两个子模块的 parent 均为 `<域>` 业务聚合 POM，并正确配置 `relativePath`。
3. 添加公共依赖：`<域>-app → common-app`，`<域>-port → common-web、common-infra`；再添加业务依赖：`<域>-port → <域>-app`。
4. 在 `business/<域>/pom.xml` 聚合两个子模块，并在 `business/pom.xml` 注册新业务模块。
5. 在 `business/deploy/` 下创建 `<域>-deploy`，组合该业务的 app/port，并注册到 `business/deploy/pom.xml`。
6. 在 `standalone-runner/pom.xml` 中添加对 `<域>-deploy` 的依赖。
7. 如需执行全局架构规则，让 `<域>-deploy` 以 `test` scope 依赖 `common-test`，并添加显式声明 `@AnalyzeClasses` 的测试子类。
8. `common-web` 只保留 `spring-webmvc`、`common-app` 与 `swagger-annotations-jakarta`，`common-app` 只保留 Spring DI 与通用应用层基类，基础设施公共依赖放入 `common-infra`；需要多类高频工具时依赖 `common-util`，单个或低频工具由使用方直接声明。
9. 只有稳定且确实跨业务共享的领域类型才放入 `share/core`；子域对外契约放 `share/<子域>-contract`，均由使用方显式依赖；不要将单一业务实现或普通工具类移入其中。

## 打包与脚本约定

- 禁止使用 `spring-boot-maven-plugin` 的 `repackage` 生成 fat jar。
- 禁止使用 assembly 的 `jar-with-dependencies` 作为应用分发方式。
- assembly 描述符位于 `runner/standalone-runner/assembly.xml`。
- 新增运行脚本或资源后，应放入 `src/main/resources/` 对应目录，并在 `assembly.xml` 中登记。
- Windows bat 脚本必须保持纯 ASCII，避免 cmd 编码解析问题。
- sh 启动脚本保留 Git Bash/Cygwin 路径转换，并保持应用工作目录为分发包根目录，确保外置 `web/`、`.logs/` 的相对路径有效。
