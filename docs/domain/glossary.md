# 领域术语表

本文件是 power-system 领域术语的**唯一来源**，人类文档与 AI 生成内容（代码命名、注释、需求与设计文档、提交信息）均以本表为准。

## 维护规则

1. 新增或调整领域概念时，在同一次改动中同步更新本表，并标注对应代码位置。
2. 代码中的类名、枚举值、字段名、包名应与"代码命名"列一致；文档与注释中的中文应与"术语"列一致。
3. 不使用"禁用叫法"列中的名称；历史代码如存在旧叫法，重命名后更新本表。
4. 条目按业务域分组；跨业务共享的概念放入"核心共享"，单一业务概念放入对应业务域分组。
5. 术语与代码不一致时，以代码实际语义为依据修正本表，或提出重命名改动，不允许长期分叉。

## 条目格式

| 列 | 说明 |
| --- | --- |
| 术语 | 标准中文名称 |
| 代码命名 | 类名 / 枚举值 / 字段名等英文命名 |
| 类型 | DDD 分类：聚合根、聚合内实体、值对象、共享值对象、聚合内值对象、枚举、标识、引用、支撑实体等 |
| 定义 | 业务含义、边界与相等性等关键语义 |
| 同义词 / 禁用叫法 | 允许的同义词；`禁用：` 前缀表示不得再使用的叫法 |
| 代码位置 | 对应源码文件或模块路径，无代码落点时留空 |

## 核心共享（share/core）

| 术语 | 代码命名 | 类型 | 定义 | 同义词 / 禁用叫法 | 代码位置 |
| --- | --- | --- | --- | --- | --- |
| （待补充） | | | | | |

## 两票（ticket）

| 术语 | 代码命名 | 类型 | 定义 | 同义词 / 禁用叫法 | 代码位置 |
| --- | --- | --- | --- | --- | --- |
| 票种 | `TicketType` | 实体（贫血） | 两票配置主体，仅持标识、名称与状态机模板等数据，业务规则由应用服务承担 | | `business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/TicketType.java` |
| 票种标识 | `TicketTypeId` | 标识 | 票种唯一标识值对象 | | `business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/TicketTypeId.java` |
| 状态机模板 | `TransitionTable` | 值对象 | 不可变，持有票证全部合法状态与允许迁移；合法性由应用服务在保存前校验 | | `business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/TransitionTable.java` |
| 状态 | `TicketState` | 值对象 | 票证状态，由状态机模板定义，不使用硬编码枚举 | | `business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/TicketState.java` |
| 状态迁移 | `Transition` | 值对象 | 从起点状态到终点状态的一次允许迁移 | | `business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/Transition.java` |
| 票种应用服务 | `TicketTypeService` | 应用服务 | 票种用例入口：定义票种（`define`，含完整性与模板合法性校验）与按标识查询（`findById`）；复杂用例再拆 `service/usecase` 独立类 | | `business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/service/TicketTypeService.java` |
| 票种仓储 | `TicketTypeRepository` | 仓储接口 | 票种保存与按标识查询；内存实现 `InMemoryTicketTypeRepository` 仅用于开发测试 | | `business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/repository/TicketTypeRepository.java` |

## 新增条目模板

复制以下行到对应分组，并删除本说明：

```text
| 术语 | 代码命名 | 类型 | 定义（业务含义、边界、关键语义） | 同义词；禁用：旧叫法 | 代码路径 |
```
