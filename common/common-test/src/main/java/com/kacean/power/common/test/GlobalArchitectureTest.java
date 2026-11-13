package com.kacean.power.common.test;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
 * 包段约定：Web MVC 层 ..web..、应用层 ..app..、基础设施层 ..infra..。
 * 骨架阶段空代码通过（archunit.properties: archRule.failOnEmptyShould=false），业务代码接入后自动生效。
 */
public abstract class GlobalArchitectureTest {

    /** 依赖方向为 web → app、infra → app：app 不得反向依赖 web/infra */
    @ArchTest
    public static final ArchRule app不得依赖web与infra = noClasses().that().resideInAPackage("..app..")
            .should().dependOnClassesThat().resideInAnyPackage("..web..", "..infra..")
            .as("依赖方向约束：app 只允许被 web/infra 依赖，不得反向依赖");

    /** web 不得依赖 infra（Controller 只允许调用 app 应用服务） */
    @ArchTest
    public static final ArchRule web不得依赖infra = noClasses().that().resideInAPackage("..web..")
            .should().dependOnClassesThat().resideInAPackage("..infra..")
            .as("web 只允许依赖 app，不得直接触碰基础设施实现");

    /** infra 不得依赖 web */
    @ArchTest
    public static final ArchRule infra不得依赖web = noClasses().that().resideInAPackage("..infra..")
            .should().dependOnClassesThat().resideInAPackage("..web..");

}
