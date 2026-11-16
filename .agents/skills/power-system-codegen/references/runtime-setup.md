# MyBatis-Plus 运行时接入（按需）

代码生成只负责编译期代码。`standalone-runner` 已统一接入 MyBatis-Plus Boot 4 Starter 与 MySQL 数据源；业务从内存仓储切换到数据库时，还需在目标子域实现持久化适配，不要因为生成了代码就假设表或仓储已经可用。

## 1. 依赖边界

目标 `<business>-port` 使用：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-core</artifactId>
</dependency>
```

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
@MapperScan("com.kacean.power.tickettype.port.infra.persistence.mapper")
```

不要把所有子域 Mapper 无条件扫描进每个业务部署单元。

## 3. 公共字段填充

`standalone-runner` 的 `com.kacean.power.config.MyBatisPlusMetaObjectHandler` 已提供公共字段填充：

- `externalNo` 插入时使用带连字符的标准 UUID（36 字符），已有值不覆盖。
- 插入时初始化 `deleted = 0`、`version = 0` 以及创建、修改审计字段。
- 创建人、修改人取自 `OperatorContext`，未设置时使用 `system`。
- 更新时只刷新 `modifyAt` 和 `modifyBy`。

处理器作为 Spring 组件由 MyBatis-Plus 调用；Starter 与数据源由 runner 统一配置，但只有实际执行对应 insert/update SQL 时才会触发填充。

## 4. 逻辑删除与乐观锁

生成器只在指定列存在时添加 `@TableLogic` / `@Version`。逻辑删除值必须与数据库列类型一致，例如数值列可配置：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-value: 1
      logic-not-delete-value: 0
```

如果 `deleted` 是 `BOOLEAN`，不要盲目使用上述数值配置，应按数据库方言和字段映射验证。乐观锁需要 MyBatis-Plus 拦截器配置才能完整生效；生成 `@Version` 本身不等于自动配置了全部运行时插件。

## 5. 领域与持久化转换

PO 是基础设施对象，不是 `app.domain` 领域对象。需要 Repository 适配时：

1. 在对应 `app` 定义领域对象与仓储接口。
2. 在同子域 `port.infra.persistence` 实现适配器。
3. 显式完成 PO ↔ 领域对象转换，不把 PO 泄漏到 Web 或其他子域。
4. 按 `power-system-app` 和 `power-system-naming` 完成命名与依赖检查。
5. 在对应 `<business>-deploy` 运行架构测试与集成测试。
