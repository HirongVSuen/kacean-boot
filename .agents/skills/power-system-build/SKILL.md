---
name: power-system-build
description: power-system 项目构建、模块与依赖调整、thin jar 分发、启停日志和标准验证指南。适用于构建打包、新增业务模块、调整 common-web/common-app/common-infra/common-util/share（core 与各子域 contract）、修改运行资源或排查构建启动问题。
---

# power-system 构建与基础架构

执行前先核对实际目录和 POM；用户已调整结构时以实际项目为准。根 `README.md` 保存完整架构与使用说明，根 `pom.xml` 是依赖版本的唯一事实来源，本文件不复制易过期的版本清单。`share` 的共享内核与子域契约边界见 `share/README.md` 与 `share/core/README.md`，本 Skill 不复制字段、枚举或构造器清单。

## 1. 不可破坏的基线

- Java 21、Maven 多模块、UTF-8。Spring Boot、Jakarta、Guava 和 Apache Commons 的版本统一由根 POM `dependencyManagement` 管理；被 BOM 托管的依赖不写版本，项目内模块依赖使用 `${project.version}`。
- 顶层仅 `runner`、`common`、`business`、`share`。目录名与 artifactId 一致；`<modules>` 只负责聚合，`<dependencies>` 决定构建顺序和运行时内容。
- 使用 **thin jar + assembly 外置依赖**。禁止 Spring Boot `repackage` 和 assembly `jar-with-dependencies`。
- 日志目录固定为 `.logs/`；bat 文件必须纯 ASCII；sh 启动脚本保留 Cygwin 路径转换，并以 `APP_HOME` 为工作目录。

## 2. 当前模块与依赖边界

```text
power-system
├── runner/standalone-runner          启动、运行资源、assembly 分发
├── common
│   ├── common-test                   ArchUnit 基类与测试配置，仅 test scope
│   ├── common-web                    最小 Spring MVC
│   ├── common-app                    Spring DI + Jakarta Persistence API
│   ├── common-infra                  基础设施公共代码与依赖
│   └── common-util                   高频 Guava / Apache Commons 工具集
├── business
│   ├── ticket/ticket-web             parent=ticket；依赖 common-web、ticket-app
│   ├── ticket/ticket-app             parent=ticket；依赖 common-app
│   ├── ticket/ticket-infra           parent=ticket；依赖 common-infra、ticket-app
│   └── deploy/ticket-deploy          parent=deploy；组合 ticket-web/app/infra
└── share                             纯 Java 共享与契约聚合，按需显式依赖
    ├── core                          跨业务共享内核（原 core-share）
    └── tickettype-contract           tickettype 子域对外契约（Facade/DTO/事件）
```

- `common-web/common-app/common-infra/common-util` 均为普通 jar 依赖模块，不再作为业务 parent。
- `common-web` 只直接依赖 `spring-webmvc`，不得加入 Boot Starter、Tomcat 或 Jackson。`standalone-runner` 通过 `spring-boot-starter-web` 提供嵌入式容器、Jackson 和 Boot MVC 自动配置。
- `common-app` 只提供 `spring-context` 和 `jakarta.persistence-api`。具体持久化实现放在 infra/deploy，不放入 app。
- 依赖方向仅允许 `web → app`、`infra → app` 和 `deploy → 业务模块`；app 不依赖 web/infra，web 与 infra 不互相依赖。
- `share/core` 只放稳定、无单一业务归属的跨业务共享领域类型，不依赖其他项目模块或框架。
- `share/<子域>-contract` 放该子域对外契约（`XxxFacade`、DTO、领域事件），纯 Java，可按需依赖 `share/core`，不得依赖业务实现模块；提供方 app 模块依赖它并以 `XxxFacadeImpl` 实现，消费方只依赖 contract 模块。
- `share` 下模块只有被显式依赖才进入分发包，不自动依赖到所有业务，也不用于堆放普通工具类。
- `standalone-runner` 只依赖各业务 deploy 模块，不写业务代码。跨业务调用不得依赖其他业务的 web 模块；稳定共享类型放入 share/core。
- 新业务三子模块继承各自业务聚合 POM；deploy 子模块继承 `business/deploy`。不要把业务 `<modules>` 移入 deploy。

## 3. 代码归属

模块归属如下。**包名、类名、方法命名细则以 `power-system-naming` 为单一来源**，本节只保留模块 / POM 视图，不复制包名表，避免双份维护漂移。

| 代码 | 模块 |
| --- | --- |
| Controller、DTO、assembler、MVC 配置 | `<域>-web` |
| 应用服务、command/query、领域模型、领域服务、仓储接口 | `<域>-app` |
| 子域间调用契约（Facade 接口、DTO、领域事件） | `share/<子域>-contract` |
| 跨业务稳定共享领域类型 | `share/core` |
| 仓储实现、持久化适配、外部客户端 | `<域>-infra` |
| 启动主类 | `standalone-runner`（`com.kacean.power.PowerSystemApplication`） |

app 直接使用 `@Service`/`@Component` 等 Spring DI 注解，优先构造器注入；领域模型可使用 `@Entity`、`@Id`、`@Table` 等 JPA 注解。空包用 `.gitkeep` 占位。

## 4. 添加依赖

- `common-util` 默认聚合 Guava、commons-lang3、commons-collections4、commons-io、commons-codec、commons-text；只有需要多类高频工具时才依赖它。
- 只使用一个工具时直接声明该工具；commons-beanutils 等低频工具不放入 `common-util`，由实际使用方按需声明。版本仍由根 POM 管理。
- 不为第三方工具库再包装一层无实际价值的项目工具类，不让其他 common 模块强制依赖 `common-util`。
- 双 BOM 未管理的外部依赖，应先在根 POM `dependencyManagement` 统一版本，再由使用模块声明。
- 修改依赖后检查直接依赖与传递依赖，不能只看 POM：

```bash
# Reactor 内模块尚未 install 时，保留 compile，确保依赖可解析
mvn -pl business/ticket/ticket-web -am compile dependency:tree
mvn -pl common/common-util dependency:tree
```

## 5. 构建与架构测试

```bash
mvn clean package                                   # 全量构建与分发包
mvn clean package -pl runner/standalone-runner -am # 启动模块及依赖
mvn clean package -DskipTests                      # 明确跳过测试时才使用
mvn test -pl business/deploy/ticket-deploy -am     # 架构测试
```

分发产物：`runner/standalone-runner/target/standalone-runner-1.0-SNAPSHOT.zip`；模块自身 jar 均为 thin jar。

ArchUnit 守护 Java 代码依赖方向、子域分包隔离与子域契约约束，**不验证 Maven parent、POM 白名单或模块聚合关系**，这些需结合 POM 审查和 `dependency:tree` 检查。

全局规则位于 `common-test` 的 main 源码，配置为其 main resources 下的 `archunit.properties`（`archRule.failOnEmptyShould=false`）。部署模块以 test scope 依赖 common-test；子类必须显式声明 `@AnalyzeClasses`，父类 `@ArchTest` 字段会自动执行。架构测试后确认报告为当前 **8 条规则**（3 条分层依赖 + 2 条子域分包 + 3 条子域契约），不能是 0 条。新增全局规则后同步更新此处数量与根 README。

代码注释（中文类 Javadoc、`@author kacean-agent`、record `@param`、契约方法注释）遵循 `.agents/skills/power-system-javadoc/SKILL.md`；包、类、方法命名（子域分包、`XxxFacade`/`XxxRepository`、用例动词服务、中文测试方法名）遵循 `.agents/skills/power-system-naming/SKILL.md`。

## 6. 标准验证

构建、依赖、结构、打包、脚本或日志改动后执行：

```bash
sh .agents/skills/power-system-build/scripts/verify.sh        # 默认 8080，可传端口参数
```

脚本自动完成全量构建、解压、启动、断言和清理，失败也会清理。断言包括：构建成功、出现 `Started PowerSystemApplication`、`/` 与 `/index.html` 返回 200、生成 `.logs/power-system.log`，最后停止应用并删除 `target/dist-verify`。

## 7. 运行资源与分发约定

- assembly 描述符为 `runner/standalone-runner/assembly.xml`；新增脚本或资源必须放入 `src/main/resources/` 并在描述符中登记。
- 主配置只维护 `src/main/resources/application.yml`，assembly 自动复制为分发包 `conf/application.yml`，不要另建独立源文件。
- 日志配置为 `src/main/resources/conf/logback-spring.xml`；静态资源为 `src/main/resources/web/`，分发包外置 `web/` 优先于 jar 内资源。
- 分发包包含 `bin/`、`conf/`、`web/`、`lib/`；`.logs/` 与 `run/` 在运行时创建。业务日志为 `.logs/power-system.log`，启动控制台日志为 `.logs/standalone-runner.log`，归档到 `.logs/archive/`。
- 启动入口为 `bin/startup.sh` 或 `bin/startup.bat`；停止使用对应 stop 脚本。不得移除脚本中的 `cd "$APP_HOME"` 或 Windows 工作目录设置，否则外置 web 和相对日志路径会失效。

## 8. 常见任务检查清单

### 新增业务模块

1. 在 `business/<域>/` 建立 web/app/infra，artifactId 带业务前缀，parent 均为该业务聚合 POM，`relativePath=../pom.xml`。
2. 分别依赖 `common-web/common-app/common-infra`，再添加 `web → app`、`infra → app`。
3. 在 `business/pom.xml` 注册业务，在 `business/deploy/` 新建 `<域>-deploy` 并注册，组合三个业务模块。
4. 若子域需对外提供契约：在 `share/` 下新建 `<子域>-contract`（纯 Java jar，parent=share）并注册到 `share/pom.xml`；提供方 app 模块依赖它实现 `XxxFacadeImpl`，消费方只依赖该 contract 模块。
5. 在 `standalone-runner` 添加该 deploy 依赖，为 deploy 添加 ArchUnit 子类和 common-test 测试依赖。
6. 执行标准验证，并同步根 README 与本 Skill 中发生变化的约束。

### 排查构建或启动

- `Child module ... does not exist`：先核对 `<modules>` 与实际目录，不凭记忆重建已移除模块。
- 缺少 `ServletWebServerFactory`：检查 runner 的 `spring-boot-starter-web`，不要把 Starter 加回 common-web。
- Controller 可直接使用 Spring MVC 注解；若直接使用 Servlet API、Jackson 注解等可选能力，由实际代码所在模块显式补充对应 API。
- 静态资源或日志路径异常：检查启动工作目录、assembly 输出路径和 `spring.web.resources.static-locations`。
