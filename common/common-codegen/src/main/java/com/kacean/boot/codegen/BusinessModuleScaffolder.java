package com.kacean.boot.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 业务模块脚手架：创建 app、port 与 deploy 模块，并注册 Maven 聚合和单体运行依赖。
 *
 * @author kacean-agent
 */
final class BusinessModuleScaffolder {

    private static final Pattern BUSINESS_PATTERN = Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");
    private static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("[a-z][a-z0-9]*");

    private BusinessModuleScaffolder() {
    }

    static void scaffold(Path projectRoot, String business, String subdomain) throws IOException {
        validateIdentifier(business, BUSINESS_PATTERN, "业务域名");
        validateIdentifier(subdomain, SUBDOMAIN_PATTERN, "子域名");

        Path businessDirectory = projectRoot.resolve("business").resolve(business);
        Path deployDirectory = projectRoot.resolve("business/deploy").resolve(business + "-deploy");
        if (Files.exists(businessDirectory)) {
            throw new IllegalArgumentException("业务目录已存在，为避免覆盖已停止：" + businessDirectory);
        }
        if (Files.exists(deployDirectory)) {
            throw new IllegalArgumentException("部署目录已存在，为避免覆盖已停止：" + deployDirectory);
        }

        Path businessAggregator = projectRoot.resolve("business/pom.xml");
        Path deployAggregator = projectRoot.resolve("business/deploy/pom.xml");
        Path runnerPom = projectRoot.resolve("runner/standalone-runner/pom.xml");
        requireFile(businessAggregator);
        requireFile(deployAggregator);
        requireFile(runnerPom);

        Map<Path, String> existingUpdates = new LinkedHashMap<>();
        existingUpdates.put(businessAggregator,
                insertModule(read(businessAggregator), business, "        <module>deploy</module>"));
        existingUpdates.put(deployAggregator, insertModule(read(deployAggregator), business + "-deploy", null));
        existingUpdates.put(runnerPom, insertRunnerDependency(read(runnerPom), business));

        Map<Path, String> newFiles = new LinkedHashMap<>();
        newFiles.put(businessDirectory.resolve("pom.xml"), businessPom(business));
        newFiles.put(businessDirectory.resolve(business + "-app/pom.xml"), appPom(business));
        newFiles.put(businessDirectory.resolve(business + "-port/pom.xml"), portPom(business));
        newFiles.put(deployDirectory.resolve("pom.xml"), deployPom(business));
        newFiles.put(deployDirectory.resolve("src/test/java/com/kacean/boot/deploy/ArchitectureTest.java"),
                architectureTest(business));

        for (Map.Entry<Path, String> entry : newFiles.entrySet()) {
            Files.createDirectories(entry.getKey().getParent());
            Files.writeString(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Path, String> entry : existingUpdates.entrySet()) {
            Files.writeString(entry.getKey(), entry.getValue());
        }
    }

    private static String insertModule(String content, String module, String beforeMarker) {
        String moduleLine = "        <module>" + module + "</module>";
        if (content.contains(moduleLine)) {
            throw new IllegalArgumentException("Maven 聚合中已注册模块 " + module);
        }
        String newline = newlineOf(content);
        if (beforeMarker != null) {
            if (!content.contains(beforeMarker)) {
                throw new IllegalArgumentException("聚合 POM 缺少定位标记 " + beforeMarker.trim());
            }
            return content.replace(beforeMarker, moduleLine + newline + beforeMarker);
        }
        String closingMarker = "    </modules>";
        if (!content.contains(closingMarker)) {
            throw new IllegalArgumentException("聚合 POM 缺少 modules 结束标记");
        }
        return content.replace(closingMarker, moduleLine + newline + closingMarker);
    }

    private static String insertRunnerDependency(String content, String business) {
        if (content.contains("<artifactId>" + business + "-deploy</artifactId>")) {
            throw new IllegalArgumentException("standalone-runner 已依赖 " + business + "-deploy");
        }
        String marker = "    </dependencies>";
        if (!content.contains(marker)) {
            throw new IllegalArgumentException("standalone-runner POM 缺少 dependencies 结束标记");
        }
        String dependency = """
                        <!-- 新增业务通过部署模块进入单体运行时 -->
                        <dependency>
                            <groupId>com.kacean.boot</groupId>
                            <artifactId>%s-deploy</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                """.formatted(business).stripTrailing();
        return content.replace(marker, dependency + newlineOf(content) + marker);
    }

    private static String businessPom(String business) {
        return pomHeader("business", "../pom.xml", business,
                business + " 业务模块：聚合 app / port 两个子模块")
                + """
                    <modules>
                        <module>%1$s-app</module>
                        <module>%1$s-port</module>
                    </modules>

                    <properties>
                        <maven.compiler.source>21</maven.compiler.source>
                        <maven.compiler.target>21</maven.compiler.target>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>

                </project>
                """.formatted(business);
    }

    private static String appPom(String business) {
        return pomHeader(business, "../pom.xml", business + "-app",
                business + " 应用层：应用服务、领域模型、仓储接口，不依赖同业务 port")
                + """
                    <dependencies>
                        <dependency>
                            <groupId>com.kacean.boot</groupId>
                            <artifactId>common-app</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                    </dependencies>

                </project>
                """;
    }

    private static String portPom(String business) {
        return pomHeader(business, "../pom.xml", business + "-port",
                business + " 端口层：承载 Web 与 MyBatis-Plus 持久化适配")
                + """
                    <dependencies>
                        <dependency>
                            <groupId>com.kacean.boot</groupId>
                            <artifactId>common-web</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>com.kacean.boot</groupId>
                            <artifactId>common-infra</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>com.kacean.boot</groupId>
                            <artifactId>%s-app</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                    </dependencies>

                </project>
                """.formatted(business);
    }

    private static String deployPom(String business) {
        return pomHeader("deploy", "../pom.xml", business + "-deploy",
                business + " 业务部署子模块：组合 app/port，供 runner 单体启动模块依赖")
                + """
                    <dependencies>
                        <dependency>
                            <groupId>com.kacean.boot</groupId>
                            <artifactId>common-test</artifactId>
                            <version>${project.version}</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>com.kacean.boot</groupId>
                            <artifactId>%1$s-app</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>com.kacean.boot</groupId>
                            <artifactId>%1$s-port</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                    </dependencies>

                </project>
                """.formatted(business);
    }

    private static String pomHeader(String parentArtifact, String relativePath, String artifactId, String description) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.kacean.boot</groupId>
                        <artifactId>%s</artifactId>
                        <version>1.0-SNAPSHOT</version>
                        <relativePath>%s</relativePath>
                    </parent>

                    <artifactId>%s</artifactId>
                    %s
                    <description>%s</description>

                """.formatted(parentArtifact, relativePath, artifactId,
                parentArtifact.equals("business") ? "<packaging>pom</packaging>" : "", description);
    }

    private static String architectureTest(String business) {
        return """
                package com.kacean.boot.deploy;

                import com.kacean.boot.common.test.GlobalArchitectureTest;
                import com.tngtech.archunit.core.importer.ImportOption;
                import com.tngtech.archunit.junit.AnalyzeClasses;

                /**
                 * %s 业务部署单元的架构守护：执行 common-test 中的全局依赖与分包规则。
                 *
                 * @author kacean-agent
                 */
                @AnalyzeClasses(packages = "com.kacean.boot", importOptions = ImportOption.DoNotIncludeTests.class)
                class ArchitectureTest extends GlobalArchitectureTest {
                }
                """.formatted(business);
    }

    private static void validateIdentifier(String value, Pattern pattern, String label) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(label + "格式非法：" + value + "，允许规则 " + pattern.pattern());
        }
    }

    private static void requireFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("缺少项目文件：" + path);
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }

    private static String newlineOf(String content) {
        return content.contains("\r\n") ? "\r\n" : "\n";
    }
}
