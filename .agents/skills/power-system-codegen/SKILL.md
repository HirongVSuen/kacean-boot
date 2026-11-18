---
name: power-system-codegen
description: power-system 的 MyBatis-Plus 开发期代码生成与业务模块脚手架。用户要求连接数据库按表生成 PO/Entity/Mapper/Mapper XML、逆向生成持久化代码、把代码落到指定业务子域、创建 business app/port/deploy 模块，或使用 common-codegen 交互式/命令行生成器时使用。负责术语与分包检查、覆盖保护、生成后验证及运行时接入边界；不用于与代码生成无关的普通 CRUD 开发。
---

# MyBatis-Plus 代码生成

使用 `common/common-codegen` 从数据库元数据生成项目适配代码。生成器只在开发期使用；生成结果始终进入目标业务子域，不集中写入 `common`，也不进入运行时依赖。

## 强制约束

1. 先读 `docs/domain/glossary.md`，再确定业务域、子域和表对应的业务概念。新增概念时，同次改动更新术语表。
2. 模块与包遵循 `power-system-build`、`power-system-naming`：
   - 业务域：`business/<business>/`，目录名允许小写字母、数字和连字符。
   - 子域：全局唯一的全小写 Java 包名，如 `tickettype`。
   - 输出根包：`com.kacean.power.<subdomain>.port.infra.persistence`。
3. 默认只生成 `PO / Mapper / Mapper XML`，禁用 MyBatis-Plus 的 Service 和 Controller 模板。
4. PO 带持久化注解，只能进入 `port.infra.persistence.po`，并固定继承 `common-infra` 的持久化 `BaseEntity`。公共列不重复生成，示例建表规范见 `common/common-infra/README.md`。不得污染 `app.domain`；需要领域对象或仓储适配时，再按 `power-system-app` 单独实现纯 Java 对象与转换。
5. 默认保留已有同名文件。只有用户明确同意覆盖后才传 `--overwrite`；执行前先检查未提交修改。
6. 数据库密码只从环境变量读取，默认变量为 `CODEGEN_DB_PASSWORD`。不得把密码写入命令参数、Skill、日志或仓库文件。
7. 不自动修改数据库，不执行 DDL/DML，不自动生成测试数据。

## 入口

在仓库根目录执行。

### 交互式（首选）

Bash / Git Bash：

```bash
bash .agents/skills/power-system-codegen/scripts/generate.sh
```

PowerShell：

```powershell
& .agents/skills/power-system-codegen/scripts/generate.ps1
```

菜单：

- `1`：创建 `<business>-app`、`<business>-port`、`<business>-deploy`，注册 Maven 聚合与 `standalone-runner` 依赖。
- `2`：读取数据库表，生成目标子域的 PO、Mapper 和 XML。

脚本先编译 `common-codegen`、解析其开发期 classpath，再直接启动 Java；参数会原样传递，包含空格的 `--project-root` 不会拆错。

### 非交互式

已有业务模块，只生成持久化代码：

```bash
export CODEGEN_DB_PASSWORD='实际密码'
bash .agents/skills/power-system-codegen/scripts/generate.sh generate \
  --business ticket \
  --subdomain tickettype \
  --jdbc-url 'jdbc:mysql://localhost:3306/power_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai' \
  --username root \
  --tables ticket_type
```

PowerShell 使用 `$env:CODEGEN_DB_PASSWORD='实际密码'`，其余参数直接传给 `generate.ps1`。

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
  --tables asset_device
```

`module` 是可选步骤：目标 `<business>-port` 已存在时禁止重复创建，直接执行 `generate`。已有目录或部署目录存在时，脚手架会拒绝覆盖。

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
| `--overwrite` | 否 | 覆盖 PO/Mapper/XML 同名文件；默认不覆盖 |
| `--project-root` | 否 | 项目根目录，默认当前目录 |

## 输出位置

以 `ticket / tickettype` 为例：

```text
business/ticket/ticket-port/
├── pom.xml                         # 缺少时自动补充 common-infra 与 mybatis-plus-core
└── src/main/
    ├── java/com/kacean/power/tickettype/port/infra/persistence/
    │   ├── po/TicketTypePO.java
    │   └── mapper/TicketTypeMapper.java
    └── resources/mapper/tickettype/TicketTypeMapper.xml
```

- `common-infra` 提供 PO 固定继承的 `infra.po.BaseEntity` 与 Mapper 固定继承的 `infra.mapper.BaseMapper`，`mybatis-plus-core` 提供 PO 注解与 MyBatis 编译 API；生成器不会擅自引入数据源或启动数据库。
- 主键固定为 `Long + ASSIGN_ID`，`id/external_no/deleted/create_at/create_by/modify_at/modify_by/version` 由 `BaseEntity` 统一映射，不在子类重复生成；目标表必须预先包含这些公共列。
- 表注释、字段注释进入中文 Javadoc，类作者固定为 `kacean-agent`。
- XML 使用独立资源目录，不把 XML 混进 Java 包。

## 执行后检查

1. 查看 `git diff`，确认只修改目标业务模块及脚手架必要的聚合文件。
2. 逐表核对：类名、字段类型、可空性、主键策略、时间类型、逻辑删除值、乐观锁列、XML namespace/resultMap。
3. 检查 `port.infra` 未依赖 `port.web`，生成代码未进入其他子域。
4. 编译并测试目标业务部署单元：

```bash
mvn -pl business/<business>/<business>-port -am test
mvn -pl business/deploy/<business>-deploy -am test
```

5. 如果还要连接真实数据库运行，按 [references/runtime-setup.md](references/runtime-setup.md) 单独配置 Starter、驱动、数据源和 Mapper XML 扫描。代码生成成功不等于运行时已接入数据库。
6. 如果要补 DAO、仓储适配、应用服务、Facade 或 Controller，切换对应 Skill：DAO/仓储优先继承 common-infra 的 `BaseDao`/`BaseDaoImpl`/`BaseRepositoryImpl` 并配合 `StructMapper`（见 [references/runtime-setup.md](references/runtime-setup.md)），不要重新打开 MyBatis-Plus 的 Service/Controller 模板。

## 排错

- **目标 port 不存在**：先确认业务域目录；确需新模块时执行 `module`。
- **数据库连接失败**：核对 JDBC URL、用户、网络和环境变量；不要用 `--password` 规避环境变量约束。
- **未生成表/字段**：核对表名大小写、schema、数据库元数据权限和 `--tables`。
- **类名不符合领域术语**：停止批量覆盖，先更新术语表，再调整表前缀或手工重命名并同步 XML namespace。
- **已有文件未更新**：这是默认保护；先 diff 并保留手工修改，确需覆盖时才加 `--overwrite`。
- **H2 验证**：可用 `jdbc:h2:mem:<库名>;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false`；进程结束后内存库消失，不用于持久运行。
