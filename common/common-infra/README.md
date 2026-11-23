# common-infra 持久化公共字段规范

`com.kacean.boot.common.infra.entity.BaseEntity` 是 MyBatis-Plus 持久化对象的公共基类。业务 Entity 继承该基类后，不得重复声明下列公共字段。

## 通用持久化与仓储基类

- `query.WrapperBuilder<E>` 携带当前 QP 与 Entity 类型，配合 `Operator` 声明比较、模糊匹配、IN、BETWEEN 和自定义条件，并承载 SELECT、公共关键字、固定排序等属性；通过 `query(XxxQueryParam.class)` 获取类型安全的具体 QP。
- `WrapperBuilder.processDefaultFields()` 扫描未声明的 QP 字段，跳过公共 `Query` 字段、static 字段和已处理字段，其余非空值默认使用 EQ；空值、空白字符串和空集合条件忽略。`processKeyword()` 用括号包围多列 OR，`processOrder()` 处理动态排序与固定排序。
- `ColumnResolver` 将 Entity 的 Java 属性解析为数据库列，优先读取 `@TableId` / `@TableField`，否则按驼峰转下划线；未知字段和 `@TableField(exist = false)` 字段会被拒绝，动态排序不能直接拼接客户端列名。
- app 的 `Query` 是业务 QP 基类，统一关键字与不可变排序列表；业务 QP 继承它声明过滤字段，不再实现接口或使用 record 继承。
- `BaseDao<E>` 基于 MyBatis-Plus `IRepository<E>`（mybatis-plus-extension，不引入 `mybatis-plus-spring` 的 `IService/ServiceImpl`）扩展：通用 CRUD 与 Wrapper 条件查询由 IRepository 提供；`list(Query)` / `page(PageQuery)` 由 `BaseDao.toWrapper` 创建 Builder，依次执行 `declareProperties`、`declareWheres`、`processDefaultFields`、`processKeyword`、`processOrder` 和 `build`；同时增加 `ID` / `IDCollection` 访问方法，单标识由 `ID` 保证 `id` 与 `code` 二选一，分别匹配主键与 `external_no`，批量主键与外部编号条件使用 OR 组合。批量方法逐条执行、不保证原子性，事务边界由调用方控制。`IDCollection` 将 `null` 列表归一为空列表并保存不可变快照，各列表内部去重，空元素与空白编号拒绝，去重后的标识总数最多 1000 个；空集合不访问数据库。
- `mapper.BaseMapper<E>` 是 MyBatis 基础 Mapper，继承原生 `com.baomidou.mybatisplus.core.mapper.BaseMapper`，业务持久化 Mapper 统一继承它；`mapper` 包只承载 MyBatis 持久化 Mapper。
- `repo.BaseConvert<DOMAIN, ENTITY>` 是 MapStruct 双向转换契约，统一使用 `fromDomain` / `toDomain`，独立于 MyBatis Mapper，与仓储基类共同放在 `repo` 包；业务转换接口仍放 `port.infra.convert`，通过 `@Mapper(componentModel = "spring")` 生成实现。
- `BaseDaoImpl<M, E>` 继承 MyBatis-Plus `AbstractRepository<M, E>`，通过 `@Autowired` 字段注入业务 Mapper（`M extends mapper.BaseMapper<E>`，按子类泛型解析，子类无需构造器）并实现 `getBaseMapper()`；`saveBatch` / `saveOrUpdateBatch` / `updateBatchById` 逐条实现，不依赖 Spring SqlSession 批处理；`saveOrUpdate` 按 BaseEntity 的 Long id 判定插入或更新，不依赖 TableInfo 缓存；`removeByIds` / `listByIds` 空集合短路。子类可通过 `getBaseMapper()` 使用 `QueryWrapper` / `LambdaQueryWrapper` 扩展特有查询。
- `BaseRepositoryImpl<DOMAIN, E, DAO>` 实现 `common-app` 的 `BaseRepository<DOMAIN>`，通过 `@Autowired` 字段注入业务 DAO 与 `repo.BaseConvert<DOMAIN, E>`（按子类泛型解析，子类无需构造器）。查询和删除委托 DAO，保存使用 `saveOrUpdate` / `saveOrUpdateBatch`，并将持久化后写回的主键、版本号重新转换为聚合。`saveAll` 最多接收 1000 个聚合；`saveAll` 与 `removeByIds` 不默认保证原子性，部分失败时可能保留已成功的写入，事务边界由调用方控制。

业务仓储只需继承 `BaseRepositoryImpl` 并注入自己的 DAO 与转换器；特有查询可通过受保护的 `dao()`、`converter()` 扩展。MyBatis-Plus 类型不得因此进入 `app` 层仓储接口。

## DAO 查询构建

业务 QP 放在 app 或 web，继承 `Query` 并声明业务过滤字段。业务 DAO 按需覆写两个钩子：`declareProperties` 声明 SELECT、关键字和固定排序，`declareWheres` 只声明 LIKE、IN、BETWEEN 等特殊条件；普通同名字段无需声明，自动按 EQ 处理：

```java
@Override
public void declareProperties(Query query, WrapperBuilder<TicketEntity> builder) {
    builder.keywordLike("name", "externalNo")
            .addOrder("createAt", SortDirection.DESC);
}

@Override
public void declareWheres(Query query, WrapperBuilder<TicketEntity> builder) {
    TicketQueryParam typedQuery = builder.query(TicketQueryParam.class);
    builder.addWhere("name", Operator.LIKE, typedQuery.getName());
    // status 等同名普通字段由 processDefaultFields 自动按 EQ 处理
}
```

`BaseDao.toWrapper(query)` 固定执行两个钩子和三个默认处理方法，因此业务 DAO 可直接复用 `list(query)` 与 `page(pageQuery)`。构建 API 使用 Entity 的 Java 属性名，由 `ColumnResolver` 映射数据库列；特殊条件若使用 QP 别名映射其他 Entity 字段，或 `where(...)` 自定义条件消费了 QP 字段，必须调用 `markProcessed(...)` 排除默认处理。

## Java 映射约定

- `id` 固定为 `Long`，使用 `@TableId(value = "id", type = IdType.ASSIGN_ID)`，由 MyBatis-Plus 分配主键，不使用数据库自增或 `String UUID`。
- `externalNo` 映射 `external_no`，长度不超过 40 个字符，插入时填充。
- `deleted` 映射 `deleted`，`0` 表示未删除，`1` 表示已删除，插入时填充。
- `createAt`、`createBy` 分别映射 `create_at`、`create_by`，插入时填充。
- `modifyAt`、`modifyBy` 分别映射 `modify_at`、`modify_by`，插入和更新时填充。
- `version` 映射 `version`，从 `0` 开始，插入时填充并用于乐观锁。

审计字段与外部编号由实际运行模块配置的 `MetaObjectHandler` 填充；逻辑删除和乐观锁还需要相应的 MyBatis-Plus 运行时配置。

## 公共表字段 DDL

所有使用 `BaseEntity` 的 MySQL 业务表必须包含以下八个公共列，列名、类型和空值约束保持一致：

| 列名 | MySQL 类型 | 空值 / 默认值 | Java 类型 | 用途 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT` | `NOT NULL` | `Long` | 主键，`ASSIGN_ID` |
| `external_no` | `VARCHAR(40)` | `NOT NULL` | `String` | 跨系统外部编号 |
| `deleted` | `TINYINT` | `NOT NULL DEFAULT 0` | `Integer` | 逻辑删除标记 |
| `create_at` | `DATETIME(3)` | `NOT NULL` | `LocalDateTime` | 创建时间，毫秒精度 |
| `create_by` | `VARCHAR(64)` | `NOT NULL` | `String` | 创建人 |
| `modify_at` | `DATETIME(3)` | `NOT NULL` | `LocalDateTime` | 修改时间，毫秒精度 |
| `modify_by` | `VARCHAR(64)` | `NOT NULL` | `String` | 修改人 |
| `version` | `BIGINT` | `NOT NULL DEFAULT 0` | `Long` | 乐观锁版本号 |

MySQL 8 示例：

```sql
CREATE TABLE example_table (
    id BIGINT NOT NULL COMMENT '主键，由 MyBatis-Plus ASSIGN_ID 分配',
    external_no VARCHAR(40) NOT NULL COMMENT '跨系统外部编号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    create_at DATETIME(3) NOT NULL COMMENT '创建时间',
    create_by VARCHAR(64) NOT NULL COMMENT '创建人',
    modify_at DATETIME(3) NOT NULL COMMENT '修改时间',
    modify_by VARCHAR(64) NOT NULL COMMENT '修改人',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    example_name VARCHAR(64) NOT NULL COMMENT '业务字段示例',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

`id` 不得声明为 `AUTO_INCREMENT`。`modify_at` 不使用数据库 `ON UPDATE`，避免绕过统一的审计填充规则。代码生成器只读取已有表结构，不创建或修改数据库表。
