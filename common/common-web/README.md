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

`common-web` 不引入 Jackson，也不添加序列化框架注解。`Result` 为纯 Java `record`，由 `standalone-runner` 提供的 Jackson 在 Web 响应输出时完成序列化。
