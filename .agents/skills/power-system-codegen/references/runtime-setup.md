# MyBatis-Plus 运行时接入（按需）

代码生成只负责开发期产物。`standalone-runner` 已统一接入 MyBatis-Plus Boot 4 Starter 与 MySQL 数据源；即使已生成 Entity、DAO、CURD 或 DDD 仓储适配，也仍需检查目标模块依赖、运行配置与真实数据库，不要因为生成了代码就假设表或仓储已经可用。

## 1. 依赖边界

目标 `<business>-port` 使用：

```xml
<dependency>
    <groupId>com.kacean.power</groupId>
    <artifactId>common-infra</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.kacean.power</groupId>
    <artifactId>common-web</artifactId>
    <version>${project.version}</version>
</dependency>
```

`common-infra` 提供 Entity 固定继承的 `BaseEntity`，并传递 MyBatis-Plus 编译 API；公共字段及其 DDL 规范见 `common/common-infra/README.md`。`common-web` 传递 Spring MVC 编译 API，因此 port 无需重复声明 `mybatis-plus-core` 或 `spring-webmvc`。DDD 模式额外确保 app 依赖 `common-app`、port 依赖 `<business>-app`。这些编译依赖不会把 MVC 容器或数据源下沉到业务模块。

`standalone-runner` 已添加 Spring Boot 4 对应的 Starter 和 MySQL 驱动：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

不要把 Starter、JDBC 驱动或生成器加入 `common-app`，也不要让 `<business>-app` 依赖 `<business>-port`。`common-codegen` 自身包含 MySQL/PostgreSQL/H2 驱动仅供开发期读取元数据，不会传递给业务模块。

## 2. 数据源与 XML

`application.yml` 提供本地开发默认值，运行环境通过变量覆盖，不把真实密码提交到仓库：

```bash
export DB_URL='jdbc:mysql://localhost:3306/power_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='实际账号'
export DB_PASSWORD='实际密码'
```

数据源默认使用 `com.mysql.cj.jdbc.Driver`。XML 扫描固定为 `classpath*:mapper/**/*.xml`，并开启下划线转驼峰；全局主键策略为 `ASSIGN_ID`，逻辑删除值为 `1`、未删除值为 `0`。

生成的 Mapper 带 `@Mapper`，通常无需再添加 `@MapperScan`。若项目改为显式扫描，只扫描对应子域：

```java
@MapperScan("com.kacean.power.tickettype.port.infra.mapper")
```

不要把所有子域 Mapper 无条件扫描进每个业务部署单元。

## 3. 公共字段填充

`BaseEntity` 的外部编号与审计字段声明了 `FieldFill`，由运行模块提供的 `MetaObjectHandler` 自动赋值；`standalone-runner` 已提供 `com.kacean.power.config.MyBatisPlusMetaObjectHandler`，规则如下：

- `externalNo` 插入时使用带连字符的标准 UUID（36 字符），已有值不覆盖。
- 插入时初始化 `deleted = 0`、`version = 0` 以及创建、修改审计字段，调用方已设置的值不被覆盖。
- 创建人、修改人通过 `LoginContextManager.getRequired()` 取当前登录用户名；无登录上下文（定时任务、启动初始化等）时回退为 `system`，不阻断持久化。
- 更新时只刷新 `modifyAt` 和 `modifyBy`，且强制覆盖实体上的旧值（MyBatis-Plus 3.5.x 的 `strictUpdateFill` 也是「为 null 才填充」，先查询再更新场景需用 `setFieldValByName` 强制刷新）。

处理器作为 Spring 组件由 MyBatis-Plus 调用，只有实际执行对应 insert/update SQL 时才会触发填充。生成或手写业务代码时不得重复声明这些公共字段，也不要在业务代码中手工设置审计字段；其他运行模块若独立部署，需复用该处理器或提供同等实现。

## 4. 逻辑删除与乐观锁

`@TableId`、`@TableLogic` 与 `@Version` 统一声明在 `BaseEntity`，生成器不会在业务 Entity 中重复生成。逻辑删除值必须与公共 DDL 的数值列类型一致：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-value: 1
      logic-not-delete-value: 0
```

`deleted` 固定使用 `0 / 1`。乐观锁需要 MyBatis-Plus 拦截器配置才能完整生效；`BaseEntity` 声明 `@Version` 本身不等于自动配置了全部运行时插件。

## 5. 领域与持久化转换

Entity 是基础设施对象，不是 `app.domain` 领域对象。需要 Repository 适配时优先复用 common-infra 基类：

1. 在对应 `app.domain` 定义领域对象、`app.repo` 定义仓储接口；通用读写可继承 common-app `BaseRepository<D>`，标识使用 `ID`/`IDCollection`（`id` 主键与 `code` 外部编号二选一，批量标识去重后最多 1000 个）。
2. 业务 DAO 接口继承 `infra.dao.BaseDao`，实现继承 `BaseDaoImpl`，生成的 `XxxMapper` 由父类 `@Autowired` 字段注入（按子类泛型解析，无需构造器，其父接口为 common-infra `BaseMapper`）；特有查询通过 `getBaseMapper()` 使用 `QueryWrapper`/`LambdaQueryWrapper`。
3. 在 `port.infra.convert` 定义 MapStruct 转换接口，继承 `infra.repo.BaseConvert<领域, Entity>` 并加 `@Mapper(componentModel = "spring")`，使用 `fromDomain`/`toDomain` 显式完成 Entity ↔ 领域对象转换，不把 Entity 泄漏到 Web 或其他子域。
4. 仓储实现继承 `infra.repo.BaseRepositoryImpl`，DAO 与转换器由父类 `@Autowired` 字段注入，无需构造器；保存后返回由持久化 Entity 重新转换、包含主键与版本号的聚合。特有查询用受保护的 `dao()`/`converter()` 扩展，Entity ↔ 领域对象转换统一调用 `fromDomain()`/`toDomain()`。业务 QP 继承 `Query`，DAO 按需覆写 `declareProperties` / `declareWheres`，通过 `builder.query(XxxQueryParam.class)` 获取具体 QP，并用 `addWhere(field, Operator, value)` 声明 LIKE、IN、BETWEEN 等特殊条件；`BaseDao.toWrapper` 默认执行 `processDefaultFields()`（普通字段 EQ）、`processKeyword()`、`processOrder()`，业务复用 `list(query)` / `page(pageQuery)`。字段统一使用 Entity Java 属性，由 `ColumnResolver` 映射数据库列；别名或复杂 `where(...)` 条件需 `markProcessed(...)`。
5. `BaseRepository.saveAll/removeByIds` 不默认保证原子性，需要整体提交或回滚时由调用方建立事务边界；`BaseDao` 自带的批量保存与更新使用 MyBatis-Plus 批量能力，事务以实际方法注解和 Spring 代理边界为准，不因方法名推定整体原子性。
6. 按 `power-system-app` 和 `power-system-naming` 完成命名与依赖检查，在对应 `<business>-deploy` 运行架构测试与集成测试。
