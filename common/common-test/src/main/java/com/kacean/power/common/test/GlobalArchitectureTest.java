package com.kacean.power.common.test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 全局架构守护规则基类（可复用）：放在 common-test 的 main 源码中打包。
 * 各业务部署子模块（deploy/&lt;业务&gt;-deploy）以 test scope 依赖 common-test，
 * 并声明一个空壳测试类继承本基类即可执行全部规则：
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.kacean.power", importOptions = ImportOption.DoNotIncludeTests.class)
 * class ArchitectureTest extends GlobalArchitectureTest {
 * }
 * }</pre>
 *
 * （@ArchTest 字段随继承自动执行；@AnalyzeClasses 不支持继承，子类必须显式声明扫描范围。）
 * 包段约定：应用层 ..app..（独立 app 模块）、Web MVC 层 ..port.web..、基础设施层 ..port.infra..（同属 port 模块）。
 * 子域分包约定：com.kacean.power.&lt;子域&gt;.app 与 com.kacean.power.&lt;子域&gt;.port.&lt;web|infra&gt;，
 * 层下再按技术分包（service/domain、controller、entity/mapper 等），如 tickettype/app、tickettype/port/web、tickettype/port/infra；
 * 不同子域的实现包之间不能调用。调用策略：DDD 前期贫血模式下 port.web 可直接调用 port.infra（简单 CRUD 不绕道 app），
 * 业务复杂度上升后才由 app 承载用例编排；app 不得反向依赖 port。
 * 子域间协作约定：同步调用经由 share/&lt;子域&gt;-contract 模块中 &lt;子域&gt;.contract 包的 XxxFacade 接口
 * （消费方注入接口、提供方在 app 子域以 XxxFacadeImpl 实现），DTO 与领域事件同包；
 * share/core（com.kacean.power.core.share）只放稳定跨业务共享领域类型，不承担子域契约。
 * 骨架阶段空代码通过（archunit.properties: archRule.failOnEmptyShould=false），业务代码接入后自动生效。
 *
 * @author kacean-agent
 */
public abstract class GlobalArchitectureTest {

    /** app 为独立模块：不得反向依赖 port 模块的 web/infra 包 */
    @ArchTest
    public static final ArchRule app不得依赖web与infra = noClasses().that().resideInAPackage("..app..")
            .should().dependOnClassesThat().resideInAnyPackage("..web..", "..infra..")
            .as("依赖方向约束：app 只允许被 port（web/infra）依赖，不得反向依赖");

    /** infra 不得依赖 web；反向的 web → infra 在贫血阶段允许（简单 CRUD 可直接调用仓储实现，不强制经过 app） */
    @ArchTest
    public static final ArchRule infra不得依赖web = noClasses().that().resideInAPackage("..infra..")
            .should().dependOnClassesThat().resideInAPackage("..web..")
            .as("infra 不得依赖 web；web 可直接调用 infra，是否经过 app 由业务复杂度决定");

    /** 子域实现包：com.kacean.power.&lt;子域&gt;.app 或 &lt;子域&gt;.port.&lt;web|infra&gt;（可含更深技术分包）；&lt;子域&gt;.contract 契约包不在其内 */
    private static final Pattern 子域实现包 = Pattern.compile(
            "com\\.kacean\\.power\\.(?<subdomain>[^.]+)\\.(?:app|port\\.(?:web|infra))(\\..*)?");

    /** 技术公共模块包前缀：common-web/common-app/common-infra/common-util/common-test，不是业务子域 */
    private static final String 公共模块包前缀 = "com.kacean.power.common.";

    /**
     * 按子域切片：同一子域的 app 与 port.web/port.infra 属于同一切片，切片内部调用方向仍由上面的分层规则约束。
     * &lt;子域&gt;.contract、core.share 不匹配本结构，返回 ignore 不参与切片；
     * common 技术公共模块中 common.app 等包同样匹配 app 段（如 common.app.domain 的实体基类），
     * 会被全部业务子域依赖，必须显式 ignore，否则误判为跨子域调用，
     * 因此「跨子域仅允许依赖对方 contract 契约包」由本切片结构与契约独立性规则共同保证。
     */
    private static final SliceAssignment 业务子域切片 = new SliceAssignment() {
        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            String packageName = javaClass.getPackageName();
            if (packageName.startsWith(公共模块包前缀)) {
                return SliceIdentifier.ignore();
            }
            Matcher matcher = 子域实现包.matcher(packageName);
            if (matcher.matches()) {
                return SliceIdentifier.of(matcher.group("subdomain"));
            }
            return SliceIdentifier.ignore();
        }

        @Override
        public String getDescription() {
            return "子域实现包 com.kacean.power.<子域>.app 与 <子域>.port.<web|infra>";
        }
    };

    /** 子域下 app 与 port.web/port.infra 层根包不得直接建类，必须再按技术分包（controller/service/domain 等） */
    @ArchTest
    public static final ArchRule 子域层根包必须按技术分包 = noClasses()
            .should().resideInAnyPackage("..web", "..app", "..infra")
            .as("子域包结构约束：com.kacean.power.<子域>.app 与 <子域>.port.<web|infra> 层根包下不得直接建类，"
                    + "port.web 按 controller/dto/assembler/provider/config、app 按 service/domain/repo、"
                    + "port.infra 按 entity/mapper/dao/curd/repo/convert/query/client/config 分包");

    /** 子域实现包之间不能调用：不同子域切片（含跨层）禁止依赖；对方 &lt;子域&gt;.contract 契约包不参与切片，天然放行 */
    @ArchTest
    public static final ArchRule 业务子域分包间不得相互调用 = slices()
            .assignedFrom(业务子域切片)
            .should().notDependOnEachOther()
            .as("子域实现隔离约束：不同子域的 app/port 实现包之间不能调用（如 tickettype 与 ticketorder），"
                    + "同层或跨层依赖均禁止；同一子域内 port.web 可直接调用 port.infra，app 按需承载复杂用例；"
                    + "跨子域协作经由 share/<子域>-contract 模块的 <子域>.contract 契约包（Facade/DTO/事件）");

    /** 契约与共享包：&lt;子域&gt;.contract（share/&lt;子域&gt;-contract 模块）与 core.share（share/core 共享内核） */
    private static final String[] 契约与共享包 = {"..contract..", "..core.share.."};

    /** 契约不得反向依赖业务分层实现：消费方与提供方都只面向契约编程，契约可独立演化 */
    @ArchTest
    public static final ArchRule 契约包不得依赖业务分层实现 = noClasses().that().resideInAnyPackage(契约与共享包)
            .should().dependOnClassesThat().resideInAnyPackage("..web..", "..app..", "..infra..")
            .as("契约独立性约束：<子域>.contract 与 core.share 只承载契约与共享类型，"
                    + "不得依赖任何 app/port（web/infra）实现，避免契约反向耦合提供方");

    /** 跨子域同步调用契约统一以 XxxFacade 接口命名，且只能定义在子域 contract 包（share/&lt;子域&gt;-contract 模块） */
    @ArchTest
    public static final ArchRule Facade契约必须定义在契约包 = classes().that().areInterfaces()
            .and().haveSimpleNameEndingWith("Facade")
            .should().resideInAPackage("..contract..")
            .as("Facade 契约位置约束：跨子域同步调用契约以 XxxFacade 接口命名，只能定义在 <子域>.contract"
                    + "（share/<子域>-contract 模块）；子域内部服务请使用用例动词命名，不要占用 Facade");

    /** Facade 实现以 XxxFacadeImpl 命名，放在 app 层对应子域，由 Spring 注入给消费方子域 */
    @ArchTest
    public static final ArchRule Facade实现必须位于app子域 = classes().that().haveSimpleNameEndingWith("FacadeImpl")
            .should().resideInAPackage("..app..")
            .as("Facade 实现位置约束：XxxFacadeImpl 必须位于 app 层对应子域"
                    + "（如 tickettype/app），不得放在 port 或契约包");

}
