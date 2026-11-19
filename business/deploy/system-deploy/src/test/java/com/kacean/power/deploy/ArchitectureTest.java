package com.kacean.power.deploy;

import com.kacean.power.common.test.GlobalArchitectureTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

/**
 * 系统业务部署单元的架构守护：执行 common-test 中的全局依赖与分包规则。
 *
 * @author kacean-agent
 */
@AnalyzeClasses(packages = "com.kacean.power", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest extends GlobalArchitectureTest {
}
