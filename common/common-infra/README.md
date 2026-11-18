# common-infra 持久化公共字段规范

`com.kacean.power.common.infra.po.BaseEntity` 是 MyBatis-Plus 持久化对象的公共基类。业务 PO 继承该基类后，不得重复声明下列公共字段。

## 通用持久化与仓储基类

- `query.QueryConditionBuilder<E>` 是查询条件构建器扩展基类，每个实现按需覆写 `addProperty`（SELECT 返回字段）、`addWhere`（WHERE 过滤条件）、`addOrder`（ORDER BY 排序），通过 `wrapper()` 使用 QueryWrapper 全部能力；`toWrapper()` 只构建一次，重复调用不重复追加条件。
- `struct.QueryParamStruct<QP, E>` 把 common-app 的 `Query` 实现（业务查询参数）转换为 `QueryConditionBuilder`；app 的 `Query` 统一关键字与排序访问，业务过滤字段由实现类型声明，业务实现推荐 record。
- `BaseDao<E>` 基于 MyBatis-Plus `IRepository<E>`（mybatis-plus-extension，不引入 `mybatis-plus-spring` 的 `IService/ServiceImpl`）扩展：通用 CRUD 与 Wrapper 条件查询由 IRepository 提供，可传入 `QueryWrapper`、`LambdaQueryWrapper` 或 `QueryConditionBuilder`（`list` / `getOneOpt` / `count`）；同时增加 `ID` / `IDCollection` 访问方法，单标识由 `ID` 保证 `id` 与 `code` 二选一，分别匹配主键与 `external_no`，批量主键与外部编号条件使用 OR 组合。批量方法逐条执行、不保证原子性，事务边界由调用方控制。`IDCollection` 将 `null` 列表归一为空列表并保存不可变快照，各列表内部去重，空元素与空白编号拒绝，去重后的标识总数最多 1000 个；空集合不访问数据库。
- `mapper.BaseMapper<E>` 是 MyBatis 基础 Mapper，继承原生 `com.baomidou.mybatisplus.core.mapper.BaseMapper`，业务持久化 Mapper 统一继承它；`mapper` 包只承载 MyBatis 持久化 Mapper。
- `struct.StructMapper<DOMAIN, DO>` 是 MapStruct 双向转换契约，独立于 MyBatis Mapper 单独放在 `struct` 包；业务转换接口通过 `@Mapper` 生成实现。
- `BaseDaoImpl<M, E>` 继承 MyBatis-Plus `AbstractRepository<M, E>`，构造器注入业务 Mapper（`M extends mapper.BaseMapper<E>`）并实现 `getBaseMapper()`；`saveBatch` / `saveOrUpdateBatch` / `updateBatchById` 逐条实现，不依赖 Spring SqlSession 批处理；`saveOrUpdate` 按 BaseEntity 的 Long id 判定插入或更新，不依赖 TableInfo 缓存；`removeByIds` / `listByIds` 空集合短路。子类可通过 `getBaseMapper()` 使用 `QueryWrapper` / `LambdaQueryWrapper` 扩展特有查询。
- `BaseRepositoryImpl<D, E>` 实现 `common-app` 的 `BaseRepository<D>`，构造器注入 `BaseDao<E>` 与 `struct.StructMapper<D, E>`。查询和删除委托 DAO，保存使用 `saveOrUpdate` / `saveOrUpdateBatch`，并将持久化后写回的主键、版本号重新转换为聚合。`saveAll` 最多接收 1000 个聚合；`saveAll` 与 `deleteByIds` 不默认保证原子性，部分失败时可能保留已成功的写入，事务边界由调用方控制。

业务仓储只需继承 `BaseRepositoryImpl` 并注入自己的 DAO 与转换器；特有查询可通过受保护的 `baseDao()`、`structMapper()` 扩展。MyBatis-Plus 类型不得因此进入 `app` 层仓储接口。

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
