# share

`share` 是 power-system 的**共享与契约聚合层**，只包含纯 Java jar 模块，统一解决两类跨边界复用：

| 子模块 | 职责 | 包名 | 稳定性 |
| --- | --- | --- | --- |
| `core` | 跨业务共享内核：稳定、无单一业务归属的领域类型（原 `core-share`） | `com.kacean.power.core.share` | 高，谨慎变更 |
| `<子域>-contract` | 该子域对外契约：`XxxFacade` 接口、DTO（`XxxView`）、领域事件 | `com.kacean.power.<子域>.contract` | 归提供方子域所有，随子域演化 |

## 核心原则：契约归提供方，core 保持纯净

- **子域契约不进入 `core`**：`core` 只放真正跨业务共享的稳定类型；每个子域的对外契约独立成 `<子域>-contract` 模块，避免高频演化的业务契约污染最稳定的共享内核。
- **契约与实现分离**：契约模块只有接口/DTO/事件；实现 `XxxFacadeImpl` 位于提供方业务模块的 `<子域>.app` 包，由 Spring 注入。
- **消费方只依赖契约**：需要调用其他子域时，只添加对方 `<子域>-contract` 的 Maven 依赖并注入 `XxxFacade`，不依赖对方的 `app/port` 实现模块。

## 依赖约束

- `core` 只使用 JDK，不依赖任何项目模块或框架。
- `<子域>-contract` 只使用 JDK，可按需依赖 `core`；**不得依赖任何业务实现模块**（`ticket-app` 等），否则契约会把实现传递拖进消费方。
- 提供方 app 模块依赖自己的 contract 模块以实现 `XxxFacadeImpl`；消费方模块依赖对方 contract 模块。
- `share` 下模块只有被显式依赖才进入分发包，不要加入 `common-*` 让所有业务自动获得。

## ArchUnit 对应约束

`common-test` 的 `GlobalArchitectureTest` 强制：

- 不同子域的 `app/port` 实现包之间禁止依赖（切片隔离）；`<子域>.contract` 不参与切片，是跨子域唯一合法入口。
- `..contract..` 与 `..core.share..` 不得依赖任何 `app/port` 实现。
- `XxxFacade` 接口只能位于 `<子域>.contract`；`XxxFacadeImpl` 只能位于 app 子域。

## 新增子域契约模块

1. 在 `share/` 下新建 `<子域>-contract/`（artifactId 与目录同名，parent 为 `share`），并注册到 `share/pom.xml` 的 `<modules>`。
2. 在 `com.kacean.power.<子域>.contract` 包中定义 `XxxFacade`、`XxxView` 与事件，全部为中文 Javadoc 的纯 Java 类型。
3. 提供方 app 模块添加该 contract 依赖，在 `<子域>.app` 下实现 `XxxFacadeImpl`（`@Service`，构造器注入所需应用服务/仓储）。
4. 消费方添加对方 contract 依赖并注入接口；执行 `mvn -pl business/deploy/ticket-deploy -am test` 验证架构规则。

> 只有出现真实的跨子域调用需求时才创建契约与模块，不为"将来可能复用"提前抽象。
