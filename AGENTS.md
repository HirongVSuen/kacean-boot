# power-system 项目指引

## 加载与事实边界

- 每次任务选择**一个主 Skill + 按需协作 Skill**，不预读全部 Skill、README 或 references。
- `.agents/skills/<name>/SKILL.md` 由 DSH 发现并按任务加载；模块 README、references 与脚本只在主 Skill 或当前任务明确需要时读取。
- 代码、根 `pom.xml` 与 `docs/domain/glossary.md` 是事实来源；文档与代码冲突时以代码为准，并通过 `power-system-skill-sync` 处理文档漂移。
- 未读取并核实源码前，不假设某个类、方法、配置或模块存在。

## 任务路由

| 任务 | 主 Skill | 按需协作 Skill |
| --- | --- | --- |
| 业务代码、CRUD、仓储、业务审查 | `power-system-business` | app、share、naming、javadoc |
| 应用服务、复杂用例、领域对象 | `power-system-app` | business、share |
| 跨子域契约与共享类型 | `power-system-share` | app、build |
| 数据库表代码生成、业务模块脚手架 | `power-system-codegen` | business、naming |
| 构建、依赖、分发、启动与运行资源 | `power-system-build` | skill-sync |
| 包、类、方法命名与命名审查 | `power-system-naming` | business、javadoc |
| Java 注释与 Javadoc | `power-system-javadoc` | naming |
| Skill 过期、公共架构变化与规范同步 | `power-system-skill-sync` | 受影响的 Skill |

## 默认工作闭环

1. 识别任务类型与目标模块，检查未提交修改，保护用户正在进行的工作。
2. 加载主 Skill；业务开发先执行其规定的 `scan-context`，其他任务读取对应事实来源。
3. 只补读与当前任务相关的 README、references 和源码，说明要复用的组件与约束。
4. 在任务范围内实现，优先复用既有组件，不顺手修复无关问题或扩大架构改动。
5. 按主 Skill 执行聚焦验证；失败时收集证据并修复当前改动导致的问题，避免无新信息的重复尝试。
6. 审查最终 diff，确认用户修改未被覆盖，再同步本次涉及的术语、README 与 Skill。

## 自主边界

### 可直接执行

- 搜索、读取、扫描、格式化以及运行与任务相关的测试。
- 修改任务相关代码、测试和文档，修复当前改动造成的失败。
- 按既定规则更新术语表、受影响 Skill 和根路由表，并报告同步点。

### 需要确认

- 业务语义存在多种解释，或需要修改公开契约、基础架构和依赖基线。
- 执行数据库 DDL/DML、覆盖用户修改或已有生成文件、破坏性 Git 操作。
- 使用真实凭证、生产数据或会产生外部副作用的服务。

### 停止并报告

- 任务目标无法唯一确定，或关键事实缺失且无法从仓库取得。
- 继续操作可能破坏用户工作，或验证反复失败且没有新的诊断信息。
- 发现非本次范围的代码缺陷时记录证据，不擅自扩大修复范围。

## 完成标准

- 代码与文档符合主 Skill 约束，未引入越界依赖、平行实现或无关改动。
- 已执行与改动级别匹配的验证：纯文档做引用、链接与 diff 检查；业务代码做目标模块与架构测试；结构、依赖或脚本变化做标准验证。
- 新领域概念已同步术语表，公共架构变化已同步受影响 Skill 和路由。
- 总结列出实际修改、复用点、验证命令与结果、未解决问题；不将未执行的验证写成已通过。

## 领域术语

- 领域术语的唯一来源是 `docs/domain/glossary.md`，文档与 AI 生成内容均以该文件为准。
- 涉及业务命名、领域建模、实体 / DTO / 枚举命名，或编写需求与设计文档前，先阅读 `docs/domain/glossary.md`。
- 新增或调整领域概念时，在同一次改动中同步更新术语表，并标注对应代码位置。
- 代码、注释、文档中的中文术语与英文命名必须与术语表一致，不使用术语表标注的禁用叫法。
