# common-web 统一响应体

`Result<T>` 是不可变 `record`，核心字段固定为 `code`、`message`、`data`，不携带时间戳、请求路径等额外字段。

## 响应码规则

| 场景 | `code` | `message` | `data` |
| --- | --- | --- | --- |
| 成功且有数据 | `0` | `success` | 业务数据，可为 `null` |
| 成功且无数据 | `0` | `success` | `null` |
| 业务失败 | 业务模块定义的非空白错误码，不得使用 `0` | 非空白错误说明 | 可选错误详情 |
| 参数校验失败 | `VALIDATION_ERROR` | `参数校验失败` | 可选错误详情 |

成功码和参数校验失败码分别由 `Result.SUCCESS_CODE`、`Result.VALIDATION_ERROR_CODE` 统一定义。业务模块通过 `Result.failure(code, message, data)` 传入业务错误码，不另设通用失败码覆盖具体错误原因。

## 参数校验错误详情

参数校验失败时，`data` 可携带具体错误详情。`common-web` 当前不约定字段级错误结构，由实际全局异常处理器与调用方契约确定。

`common-web` 不引入 Jackson 序列化运行时，仅提供 `jackson-annotations` 注解包，供业务 DTO 使用 `@JsonFormat` 等注解。`Result` 为纯 Java `record`，不添加序列化框架注解，由 `standalone-runner` 提供的 Jackson 在 Web 响应输出时完成序列化。

## CRUD 与 DDD MVC 基类

- `CurdAssembler<DTO, ENTITY>` 定义 DTO ↔ Entity 的单值、集合与 `PageResult` 双向转换，适用于贫血模式。
- `DddAssembler<DTO, DOMAIN, ENTITY> extends CurdAssembler<DTO, ENTITY>`，增加 DTO ↔ DOMAIN 转换；具体业务接口使用 `@Mapper(componentModel = "spring")`。
- `CurdController<DTO, ENTITY, CURD>` 通过 `@Autowired` 字段注入 `CurdAssembler` 与 `BaseCurd`（按子类泛型解析，子类无需构造器），统一承载通用 CRUD 端点。
- `DddController<DTO, DOMAIN, ENTITY, CURD> extends CurdController<DTO, ENTITY, CURD>`，通过 `@Autowired` 字段注入 `DddAssembler`，业务 CURD 由父类字段注入，通过协变 `assembler()` 暴露领域转换能力。它只增加 `DOMAIN` 泛型和转换入口，不复制通用端点。

贫血控制器直接继承 `CurdController`，DDD 控制器直接继承 `DddController`。业务子类只声明特有端点，不重复实现父类已有 CRUD 映射，也不另建 `BaseController` / `SimpleController` 等平行继承层。DDD 父类继承的通用端点仍走 `BaseCurd` 与 Entity；需要领域用例时，由业务控制器新增端点并调用 app 服务。

业务 port 模块通过 `common-web` 传递获得 Spring MVC 编译 API，无需重复声明 `spring-webmvc`；嵌入式容器、Jackson 与 MVC 自动配置由 `standalone-runner` 提供。
