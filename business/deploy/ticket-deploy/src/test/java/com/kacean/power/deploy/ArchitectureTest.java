package com.kacean.power.deploy;

import com.kacean.power.common.test.GlobalArchitectureTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

/**
 * ticket 业务部署单元的架构守护：继承 common-test 的全局规则基类，自动执行父类全部 @ArchTest 规则。
 * 注意：@AnalyzeClasses 不支持继承，子类必须显式声明扫描范围。
 * 本模块特有规则可在此追加；全局规则请维护在 GlobalArchitectureTest。
 *
 * @author kacean-agent
 */
@AnalyzeClasses(packages = "com.kacean.power", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest extends GlobalArchitectureTest {
}
