---
name: power-system-skill-sync
description: power-system 业务开发相关 skill 的自更新指南。基础架构设计（根 POM、模块结构、ArchUnit 规则、runner 运行资源）或通用组件（common-web/common-app/common-infra/common-util/common-codegen、share/core 与各子域 contract）发生变化后，以及用户要求「扫描并升级 skill」「同步 skill」「检查 skill 是否过期」时使用：自动扫描代码事实、按影响映射定位受影响的业务开发 skill、最小化更新并校验，保证 agent 业务开发始终收敛在最新架构与通用组件之上。
---

# power-system 业务开发 skill 自更新

**目的**：业务域开发（`power-system-business`）依赖的架构约束与通用组件描述，必须随代码演进自动同步，防止各业务域 agent 基于过期基类、包名或规则开发，造成实现漂移。

**核心原则**：代码是唯一事实来源，skill 是被同步的镜像。本 Skill 只修改 `.agents/skills/**`；发现代码缺陷（引用不存在的类、未完成的桩实现）只报告，不修改通用组件或业务代码；根 `README.md`、`AGENTS.md`、术语表的必要变更在总结中提出建议，由用户确认后处理。

## 1. 触发与扫描

触发时机（满足其一即执行）：

- `common/**`、`share/**`、根 `pom.xml`、`common-test` 架构规则、`runner/**` 运行资源出现提交或未提交变更。
- 新增/重命名/删除公共基类、契约类型、技术分包、Maven 模块。
- 用户明确要求同步或校验 skill。

在仓库根目录执行扫描：

```bash
bash .agents/skills/power-system-skill-sync/scripts/scan-context.sh
```

脚本输出：未提交变更、通用组件公共类清单、ArchUnit 规则数、业务域代码分布、各 skill 引用类存在性校验（防虚构）。再补充收集变化范围：

```bash
git status --short
git diff HEAD --stat -- common share pom.xml runner
git log --oneline -5        # 已提交的重构同样纳入核对
```

## 2. 影响映射（事实来源 → 必须核对的 skill）

按变化文件定位下表行，逐行核对对应 skill 的具体章节；**只更新发生漂移的部分，不做全文重写**。

| 变化的事实来源 | 受影响的 skill 与核对点 |
| --- | --- |
| `common/common-infra`（BaseEntity/BaseMapper/BaseDao(Impl)/BaseRepositoryImpl/StructMapper/QueryConditionBuilder/QueryParamStruct/BaseCurd 及 README） | `power-system-build` §2 模块边界；`power-system-app` §4 仓储与查询链路；`power-system-naming` §2 infra 分包、§3 类命名；`power-system-codegen` 输出说明与 `references/runtime-setup.md` §5；`power-system-business` §2 复用矩阵、§3 红线 |
| `common/common-app`（BaseDomain/BaseCase/BaseRepository、ID/IDCollection、Query 与分页模型） | `power-system-build` §2；`power-system-app` §2/§4；`power-system-naming` §3；`power-system-business` §2 |
| `common/common-web`（BaseController/BaseAssembler/Result 及 README） | `power-system-build` §2；`power-system-naming` §3；`power-system-business` §2 |
| `common/common-codegen`（生成器、模板、脚本参数） | `power-system-codegen` 全文（参数表、输出位置、POM 依赖补充）；`references/runtime-setup.md`；`power-system-business` §4 开发流程 |
| `common/common-test`（GlobalArchitectureTest 规则与包约束） | `power-system-build` §5 规则数量；`power-system-naming` §2 分包约束；`power-system-app` §1 包结构；`power-system-share` 契约约束；`power-system-business` §3 红线 |
| 根 `pom.xml`、模块聚合、各模块依赖 | `power-system-build` §1/§2/§4；`power-system-codegen` `references/runtime-setup.md` §1 依赖边界 |
| `runner/**`（Starter、数据源、填充器、assembly、脚本、日志） | `power-system-build` §2/§6/§7；`power-system-codegen` `references/runtime-setup.md` §2/§3/§4 |
| `share/core`、`share/<子域>-contract` | `power-system-share` 全文；`power-system-build` §2/§3；`power-system-business` §2 跨子域与共享类型行 |
| `docs/domain/glossary.md` | `power-system-naming`、`power-system-app`、`power-system-share`、`power-system-business` 中的术语与示例类名 |
| 新增业务开发规范（新角色、新分包、新约定） | 先归入单一来源 skill，其余 skill 只引用不复制；必要时更新 `power-system-business` 复用矩阵 |

未列出的变化（如纯业务实现细节）不触发 skill 更新。

## 3. 自更新流程

1. **确定变化集**：由扫描结果与 diff 列出变化的类、包、依赖、规则、配置项；逐项打开源码/README 核对真实签名与语义，不凭 diff 摘要臆测。
2. **定位受影响章节**：按第 2 节映射找到目标 skill 与段落；无对应内容时才新增小节，优先挂在既有结构下。
3. **最小化更新**：
   - 类名、包名、方法签名、泛型参数、构造器注入方式与源码逐字一致；已删除/重命名的旧引用全量清除。
   - 同步 frontmatter `description`，保证触发词覆盖新能力（路由描述不超过 1024 字符）。
   - 保持单一来源：字段清单、DDL、版本号等易过期细节留在模块 README 或根 POM，skill 只写约束与指针；同一约束在多个 skill 出现时，一处为全文、其余为引用。
   - `power-system-business` 的复用矩阵与红线必须覆盖新增通用组件，确保业务开发能复用到新能力。
4. **校验**（全部通过才算完成）：
   - 重跑 `scan-context.sh`，第 5 节引用类存在性校验全部存在。
   - 旧引用清零：`grep -rn "旧包名/旧类名" .agents/skills`。
   - 文档内相对链接（references/scripts/模块 README）真实存在。
   - ArchUnit 规则数等计数与代码一致；写入 skill 的命令真实可执行（脚本路径、模块路径、参数名与生成器 `Options` 一致）。
   - 仅文档改动不必触发全量构建；若同时改了代码，按 `power-system-build` 执行对应模块测试。
5. **输出同步报告**：变化的代码事实、更新的 skill 文件与要点、发现的代码问题（仅报告）、建议同步的根 README/术语表事项。

## 4. 防退化规则

- **不虚构**：每个写入 skill 的类、方法、配置、命令必须有代码证据；禁止「已提供」「已支持」等无证据断言，未实现的能力写明「待新增」。
- **不扩散**：一次只同步本次变化影响的 skill；不顺手重排无关段落、不改写无关示例。
- **不回滚用户意图**：skill 中用户手工新增且与代码不冲突的约定保留；冲突时在报告中说明取舍。
- **可重复执行**：同一变化集重复执行本流程应产生相同结果，已同步内容不重复追加。
