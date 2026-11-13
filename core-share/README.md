# core-share

`core-share` 是 power-system 的**跨业务共享内核**，用于沉淀稳定、无单一业务归属的领域类型与契约。

它是根目录下的普通 Java jar 模块，不是公共工具包，也不承担跨业务服务调用。当前提供设备引用、人员引用与场站信息，只有被其他模块显式依赖后才会进入应用分发包。

## 模块边界

### 可以放入

- 多个业务共同使用、含义稳定且定义一致的领域值对象。
- 跨业务共享的领域标识、枚举和稳定契约。
- 不含框架注解的纯 Java 领域事件或数据传输类型。

### 不应放入

- 仅属于 ticket 或其他单一业务的模型、服务和规则。
- Controller、Spring MVC DTO、assembler 或 Web 配置。
- Spring DI、JPA、Hibernate、Spring Data 等框架注解与实现。
- 仓储实现、数据库访问、外部客户端和基础设施配置。
- 通用字符串、集合、IO 等工具类；高频工具由 `common-util` 管理，低频工具按需直接引入。
- 为复用而提前抽象、但尚无二个以上真实使用方的类型。

## 依赖约束

`core-share` 的主代码只使用 JDK，不依赖其他项目模块，也不依赖 Spring、Jakarta 或第三方框架。不得通过增加依赖让它反向依赖 `common-*`、`ticket-*` 或其他业务模块。

业务模块确有使用需求时显式添加：

```xml
<dependency>
    <groupId>com.kacean.power</groupId>
    <artifactId>core-share</artifactId>
    <version>${project.version}</version>
</dependency>
```

不要为了让所有业务自动获得该模块，把它加入 `common-app`、`common-web` 或 `common-infra`。

## 包结构

根包固定为：

```text
com.kacean.power.core.share
```

类型较少时保持简单结构；出现多个清晰的共享主题后，再按领域主题划分子包，不预先建立复杂的 `model/service/util` 技术分层。

## 当前共享契约

### 通用冗余快照

```java
EntitySnapshot(String id, String internalCode, String name)
```

用于在引用方冗余保存名称、内部系统编码和 ID。快照整体可为 `null`；一旦提供，三个字段均须非空且非空白。快照是辅助信息，不要求与主标识相同，不参与设备、人员或场站的身份判断。

### 设备引用

```java
DeviceRef(IdentifierScheme scheme, String value, EntitySnapshot snapshot)
```

设备引用身份仅由 `scheme + value` 决定；保留双参数构造器，此时 `snapshot` 为 `null`。`IdentifierScheme` 支持：

- `KKS`
- `GB50549`
- `DOUBLE_NAME`
- `INTERNAL_ID`

### 人员引用

```java
PersonRef(String personId, RoleType roleType, EntitySnapshot snapshot)
```

人员引用身份仅由 `personId + roleType` 决定；同一人员承担不同角色时属于不同引用。保留双参数构造器，此时 `snapshot` 为 `null`。`RoleType` 支持：

- `ISSUER`：签发人
- `PERMITTER`：许可人
- `WORK_LEADER`：工作负责人
- `SUPERVISOR`：监督人
- `OPERATOR`：操作人

设备与人员的主标识均须非空且非空白，标识值按原样保存，不自动 trim、转大写或进行其他规范化。

### 场站信息

```java
Site(SiteType siteType, String name, String internalCode)
```

`Site` 是不包含独立身份标识和层级关系的场站/设施位置信息，按 `siteType + name + internalCode` 判断相等。`SiteType` 当前支持：

- `PLANT`：电厂
- `SUBSTATION`：变电站
- `SWITCHING_STATION`：开关站
- `CONVERTER_STATION`：换流站

`siteType` 和 `name` 必填，名称不允许为空白；`internalCode` 可为 `null`，非 `null` 时不允许为空白。名称与编码均按原样保存。

## 设计与变更原则

1. **先证明共享，再提取**：至少存在多个真实业务使用场景，且各方对概念和语义达成一致。
2. **保持稳定**：优先新增兼容能力，谨慎修改已有类型的字段、构造器、方法签名和语义。
3. **避免业务泄漏**：不要为了某个业务的特殊需求给共享类型增加专用字段或行为。
4. **保持纯 Java**：共享类型不应依赖 Web、IoC、持久化或序列化框架才能使用。
5. **不做服务代理**：跨业务流程调用仍由业务应用服务负责，`core-share` 只承载稳定共享类型与契约。

新增或修改共享类型前，应确认所有依赖方，并进行兼容性检查：

```bash
# 构建 core-share 自身
mvn test -pl core-share

# 示例：检查 ticket-app 对 core-share 的实际依赖
mvn -pl business/ticket/ticket-app -am compile dependency:tree -Dincludes=com.kacean.power:core-share

# 依赖或结构变更后的完整验证；8080 被占用时可指定其他端口
sh .agents/skills/power-system-build/scripts/verify.sh 18080
```

## 放置前检查

一个类型只有同时满足以下条件才适合进入本模块：

- 有二个以上真实业务使用方，而不是预期未来可能复用。
- 各业务对它的语义和生命周期理解一致。
- 不属于任何单一业务的内部实现。
- 不需要引入框架或其他项目模块。
- 变更方愿意评估并验证所有现有依赖方。
