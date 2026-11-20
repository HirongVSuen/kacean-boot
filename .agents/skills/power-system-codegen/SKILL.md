---
name: power-system-codegen
description: power-system 的 MyBatis-Plus 开发期代码生成与业务模块脚手架。用户要求连接数据库按表生成 Entity/Mapper/Mapper XML、生成贫血或 DDD 分层业务代码、把代码落到指定业务子域、创建 business app/port/deploy 模块，或使用 common-codegen 交互式/命令行生成器时使用。负责术语与分包检查、覆盖保护、生成后验证及运行时接入边界；不用于与代码生成无关的普通 CRUD 开发。
---

# 业务代码生成

使用 `common/common-codegen` 从数据库元数据生成项目适配代码。生成器只在开发期使用；生成结果始终进入目标业务子域，不集中写入 `common`，也不进入运行时依赖。

## 强制约束

1. 先读 `docs/domain/glossary.md`，再确定业务域、子域和表对应的业务概念。新增概念时，同次改动更新术语表。
2. 模块与包遵循 `power-system-build`、`power-system-naming`：
   - 业务域：`business/<business>/`，目录名允许小写字母、数字和连字符。
   - 子域：全局唯一的全小写 Java 包名，如 `tickettype`。
   - 输出根包：`com.kacean.power.<subdomain>`；Web、infra 和 app 分别进入既有 port/app 模块。
3. 提供 `anemic` 与 `ddd` 两种模式，默认 `anemic`。两种模式都使用项目自定义 Entity/Mapper/XML 与业务模板，禁用 MyBatis-Plus 原生 Service/Controller 模板。
4. Entity 带持久化注解，只能进入 `port.infra.entity`，并固定继承 `common-infra` 的持久化 `BaseEntity`。公共列不重复生成，示例建表规范见 `common/common-infra/README.md`。领域对象只放 `app.domain`，不得与 Entity 混用。
5. 默认保留所有已有同名文件。只有用户明确同意覆盖后才传 `--overwrite`；执行前先检查未提交修改。`--overwrite` 会覆盖本次涉及的全部同名生成文件，不是逐文件覆盖。
6. 非交互式生成的数据库密码只从环境变量读取，默认变量为 `CODEGEN_DB_PASSWORD`；交互式生成优先读取该变量，未设置时按终端提示输入（终端支持时使用隐藏输入）。不得把密码写入命令参数、Skill、日志或仓库文件。
7. 不自动修改数据库，不执行 DDL/DML，不自动生成测试数据。

## 两种模式

### 贫血模式（`anemic`，默认）

按表生成完整的数据库驱动 CRUD 链路：

- infra：`Entity`、`Mapper`、XML、`Dao`、`DaoImpl`、`Curd`。
- web：`DTO`、`Assembler`、`Controller`。
- `XxxAssembler extends CurdAssembler<DTO, ENTITY>`，只负责 DTO ↔ Entity 转换。
- `XxxController extends CurdController<DTO, ENTITY, CURD>`，通用 CRUD 端点由父类提供，业务控制器不重复声明。
- 允许 `port.web → port.infra` 直调，不为了简单 CRUD 强行增加 app 用例层。

### DDD 模式（`ddd`）

先生成贫血模式的全部产物，再追加：

- app：`app.domain.Xxx`、`app.repo.XxxRepository`。
- infra：`port.infra.convert.XxxConvert`、`port.infra.repo.XxxRepositoryImpl`。
- Web 侧改为 `DddAssembler<DTO, DOMAIN, ENTITY>` 与 `DddController<DTO, DOMAIN, ENTITY, CURD>`。

继承关系固定为：

```text
CurdAssembler<DTO, ENTITY>
└── DddAssembler<DTO, DOMAIN, ENTITY>       增加 DTO ↔ DOMAIN 转换

CurdController<DTO, ENTITY, CURD>           通用 CRUD 端点只有这一份
└── DddController<DTO, DOMAIN, ENTITY, CURD> 增加 DOMAIN 泛型与 DDD Assembler 入口
```

不要创建 `BaseController`、`SimpleController`、`BaseAssembler`、`SimpleAssembler` 等平行层。DDD 继承的通用 CRUD 端点仍通过 `BaseCurd` 操作 Entity；领域转换、仓储和应用服务供业务扩展使用，生成器不会假装把通用端点自动改成领域用例，也不会生成 `XxxService`、复杂用例或 Facade。

### 从贫血升级到 DDD

先检查并保存业务侧手工修改，再执行 `generate --mode ddd`。默认不覆盖已有文件，因此会追加 domain/repo/convert，但已有 `XxxAssembler`、`XxxController` 仍保持原父类；如需切换为 DDD 父类，应手工调整这两个文件，或在确认可以丢弃同名生成文件的修改后显式使用 `--overwrite`。升级后必须检查 `port → app` 依赖与生成源码编译结果。

## 入口

在仓库根目录执行。

### 交互式（首选）

Windows / DSH 使用 PowerShell 5.1+；首行仅放宽当前进程的执行策略，不修改系统设置，每个新的 DSH PowerShell 进程都需要执行：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& .agents/skills/power-system-codegen/scripts/generate.ps1
```

Bash / Git Bash：

```bash
bash .agents/skills/power-system-codegen/scripts/generate.sh
```

菜单：

- `1`：创建 `<business>-app`、`<business>-port`、`<business>-deploy`，注册 Maven 聚合与 `standalone-runner` 依赖。
- `2`：读取数据库表，生成贫血模式代码。
- `3`：读取数据库表，生成 DDD 模式代码。

脚本先编译 `common-codegen`、解析其运行所需 classpath（排除测试依赖），再直接启动 Java；参数原样传递，包含空格的 `--project-root` 不会拆错。

### 非交互式

已有业务模块，生成默认贫血代码：

```bash
export CODEGEN_DB_PASSWORD='实际密码'
bash .agents/skills/power-system-codegen/scripts/generate.sh generate \
  --business ticket \
  --subdomain tickettype \
  --jdbc-url 'jdbc:mysql://localhost:3306/power_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai' \
  --username root \
  --tables ticket_type \
  --mode anemic
```

生成 DDD 代码只需将最后一项改为 `--mode ddd`。Windows / DSH 在当前 PowerShell 进程设置执行策略后，使用 `$env:CODEGEN_DB_PASSWORD='实际密码'`，将 `bash .../generate.sh` 替换为 `& .../generate.ps1`，其余参数保持不变。

创建新业务模块后再生成：

```bash
bash .agents/skills/power-system-codegen/scripts/generate.sh module \
  --business asset \
  --subdomain assetdevice

bash .agents/skills/power-system-codegen/scripts/generate.sh generate \
  --business asset \
  --subdomain assetdevice \
  --jdbc-url 'jdbc:mysql://localhost:3306/power_system' \
  --username root \
  --tables asset_device \
  --mode ddd
```

`module` 是可选步骤：目标 `<business>-port` 已存在时禁止重复创建，直接执行 `generate`。已有业务目录或部署目录存在时，脚手架会拒绝覆盖。

查看完整参数：

```bash
bash .agents/skills/power-system-codegen/scripts/generate.sh help
```

## generate 参数

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `--business` | 是 | 业务域目录及 Maven 模块前缀，如 `ticket` |
| `--subdomain` | 是 | 目标 Java 子域包，如 `tickettype`；不要机械地等于业务域名 |
| `--jdbc-url` | 是 | JDBC URL；内置 MySQL、PostgreSQL、H2 驱动 |
| `--tables` | 是 | 表名，英文逗号分隔；只允许字母、数字、下划线 |
| `--username` | 否 | 数据库用户，默认空 |
| `--password-env` | 否 | 密码环境变量名，默认 `CODEGEN_DB_PASSWORD` |
| `--schema` | 否 | 数据库 schema；PostgreSQL 等需要时指定 |
| `--table-prefix` | 否 | 类名中移除的表前缀，英文逗号分隔 |
| `--mode` | 否 | `anemic` 或 `ddd`，默认 `anemic` |
| `--overwrite` | 否 | 覆盖本次模式的同名生成文件；默认不覆盖 |
| `--project-root` | 否 | 项目根目录，默认当前目录 |

## 输出位置

以 `ticket / tickettype` 为例，两种模式共同生成：

```text
business/ticket/ticket-port/
├── pom.xml
└── src/main/
    ├── java/com/kacean/power/tickettype/port/
    │   ├── infra/
    │   │   ├── entity/TicketTypeEntity.java
    │   │   ├── mapper/TicketTypeMapper.java
    │   │   ├── dao/TicketTypeDao.java
    │   │   ├── dao/TicketTypeDaoImpl.java
    │   │   └── curd/TicketTypeCurd.java
    │   └── web/
    │       ├── dto/TicketTypeDTO.java
    │       ├── assembler/TicketTypeAssembler.java
    │       └── controller/TicketTypeController.java
    └── resources/mapper/tickettype/TicketTypeMapper.xml
```

DDD 模式另外生成：

```text
business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/
├── domain/TicketType.java
└── repo/TicketTypeRepository.java

business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/
├── convert/TicketTypeConvert.java
└── repo/TicketTypeRepositoryImpl.java
```

依赖与映射约定：

- port 缺少依赖时幂等补充 `common-infra` 与 `common-web`；MyBatis-Plus 和 Spring MVC 编译 API 分别由二者传递提供，不再重复声明 `mybatis-plus-core`、`spring-webmvc`。DDD 还确保 app 依赖 `common-app`、port 依赖 `<business>-app`。已有依赖不重复插入，运行容器仍由 runner 提供。
- DAO 实现使用 `@Repository`，CURD 使用 `@Component`，Controller 使用 `@RestController`，依赖由 common 基类 `@Autowired` 字段注入，生成的子类不再声明构造器；业务 Assembler/Convert 使用 `@Mapper(componentModel = "spring")`。
- 主键固定为 `Long + ASSIGN_ID`，`id/external_no/deleted/create_at/create_by/modify_at/modify_by/version` 由 `BaseEntity` 统一映射，不在 Entity 子类重复声明；目标表必须预先包含这些公共列。
- DTO 与 DDD 领域对象显式保留除 `deleted` 外的公共字段及业务字段，供查询、更新和 MapStruct 映射使用；逻辑删除由持久层管理，不对外暴露。
- DTO 字段统一生成 `@Schema(description = 字段注释)` 文档注解，`LocalDateTime` 字段额外生成 `example = "2026-01-01 12:00:00"` 与 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`，请求、响应与 Swagger 示例时间统一使用该格式；领域对象不引入 Jackson/Swagger 注解。相关注解由 common-web 的 `jackson-annotations`、`swagger-annotations-jakarta` 依赖提供，业务 port 模块经 common-web 传递获得。
- DTO、Entity 与 DDD 领域对象统一使用 Lombok `@Getter`/`@Setter`（与 `BaseEntity` 一致），不再生成手写访问器；Lombok 依赖由根 POM 全局继承，业务模块无需重复声明。
- 表注释、字段注释进入中文 Javadoc，类作者固定为 `kacean-agent`；XML 使用独立资源目录。

## 执行后检查

1. 查看 `git diff`，确认只修改目标业务模块及脚手架必要的聚合文件。
2. 逐表核对类名、字段类型、可空性、主键策略、时间类型、逻辑删除值、乐观锁列、XML namespace/resultMap，以及公共字段没有重复进入 Entity。
3. 贫血模式检查 `CurdAssembler` / `CurdController`；DDD 模式检查 `DddAssembler` / `DddController`、`app.domain/repo` 与 `infra.convert/repo`，且通用端点没有在子类重复生成。
4. 检查 `app` 未依赖 `port`、`port.infra` 未依赖 `port.web`，生成代码未进入其他子域。
5. 编译并测试目标业务部署单元：

```bash
mvn -pl business/<business>/<business>-port -am test
mvn -pl business/deploy/<business>-deploy -am test
```

6. 如果要连接真实数据库运行，按 [references/runtime-setup.md](references/runtime-setup.md) 单独配置 Starter、驱动、数据源和 Mapper XML 扫描。代码生成成功不等于运行时已接入数据库。
7. 多步编排、独立事务、跨仓储协作等业务用例按 `power-system-app` 另行实现；不要重新打开 MyBatis-Plus 的 Service/Controller 模板。

## 排错

- **目标 port 不存在**：先确认业务域目录；确需新模块时执行 `module`。
- **数据库连接失败**：核对 JDBC URL、用户、网络和环境变量；不要用 `--password` 规避环境变量约束。
- **未生成表/字段**：核对表名大小写、schema、数据库元数据权限和 `--tables`。
- **类名不符合领域术语**：停止批量覆盖，先更新术语表，再调整表前缀或手工重命名并同步 XML namespace。
- **已有文件未更新**：这是默认保护；先 diff 并保留手工修改，确需覆盖时才加 `--overwrite`。
- **贫血升级后仍是 CURD 父类**：默认保护保留了既有 Assembler/Controller；按“从贫血升级到 DDD”手工切换父类或审慎覆盖。
- **H2 验证**：可用 `jdbc:h2:mem:<库名>;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false`；进程结束后内存库消失，不用于持久运行。
