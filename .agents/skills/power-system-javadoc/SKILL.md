---
name: power-system-javadoc
description: power-system 项目 Java 注释与 Javadoc 规范。生成或修改类、接口、枚举、record、测试类，以及编写/补齐/审查注释时使用：强制中文类注释与 @author kacean-agent，统一标签顺序、record @param、契约接口方法注释与领域术语。
---

# power-system Java 注释规范

适用于本仓库全部 Java 源码（含测试类）。**AI 生成代码的作者统一为 `@author kacean-agent`**。仓库既有代码可能保留 `@author hirong` 等历史署名（如 `share/core`、`runner` 主类），不要为了统一而批量改写已有 author。

## 1. 必须遵守

1. 每个新建顶层类型（类 / 接口 / 枚举 / record / 注解，**含测试类**）都必须有类级 Javadoc：一段中文业务描述 + `@author kacean-agent`，不能只写描述漏掉 author。
2. 标签顺序固定：中文描述 → 空行 ` *` → `@author kacean-agent` → `@param` / `@return` / `@throws` 等其他标签。
3. record 必须为每个组件写 `@param`；公开应用服务、`<域>.contract` 契约接口的方法写中文 Javadoc，说明业务语义、校验规则、返回值与异常，而不是复述方法名。
4. 中文术语以 `docs/domain/glossary.md` 为唯一来源；类名、字段名、API、日志文本等保持英文原文，不翻译。
5. 修改已有类时保留原注释与原 author；发现缺失 `@author` 顺手补齐（新代码用 `kacean-agent`），但不借补注释改动既有行为。
6. 注释解释「为什么 / 业务规则 / 边界约束」，不复述代码；描述要具体，`@author` 不可省略。

## 2. 模板

普通类 / 测试类：

```java
/**
 * 票种类型应用服务：定义并保存票种类型，保存前执行完整性校验。
 *
 * @author kacean-agent
 */
@Service
public class TicketTypeService {
}
```

record（`@author` 在 `@param` 之前）：

```java
/**
 * 票种类型名称值对象，统一校验名称非空白。
 *
 * @author kacean-agent
 * @param value 票种类型名称，非空白
 */
public record TicketTypeName(String value) {
}
```

契约接口（业务方法同样写中文 Javadoc）：

```java
/**
 * 票种类型对外契约：其他子域只依赖本接口，不接触 tickettype 内部实现。
 *
 * @author kacean-agent
 */
public interface TicketTypeFacade {

    /**
     * 按标识查询票种类型，不存在时抛出业务异常。
     *
     * @param id 票种类型标识
     * @return 票种类型视图
     */
    TicketTypeView getRequired(ID id);
}
```

## 3. 易踩的坑

- `<pre>{@code ...}</pre>` 示例代码中的 `@AnalyzeClasses`、`@ArchTest` 等行**不是** Javadoc 标签区：`@author` 必须放在类注释的标签区或描述之后，不要插进示例代码里。
- `@Override` 的实现方法不重复抄写契约注释，业务语义统一写在接口；实现类只补充自身特有约束。
- 批量补注释后必须抽查插入位置，避免脚本把 `@author` 插到 `<pre>` 示例或方法注释中。
- 构建、包结构、启动脚本等约束见 `power-system-build`，本 Skill 只负责注释与 Javadoc。

## 4. 自检

生成或修改代码后执行（输出应为空）：

```bash
find business common share runner -path "*/target" -prune -o -name "*.java" -print | xargs grep -L "@author"
```

新建代码应为 `@author kacean-agent`；再运行 `mvn -pl business/deploy/<域>-deploy -am test`，确认编译、业务测试与架构规则全部通过。
