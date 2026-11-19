package com.kacean.power.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务模块脚手架测试：验证 Maven 聚合、部署组合、运行依赖与重复创建保护。
 *
 * @author kacean-agent
 */
class BusinessModuleScaffolderTest {

    @TempDir
    Path projectRoot;

    @Test
    void 创建业务模块成功_注册聚合部署与运行依赖() throws IOException {
        prepareProject();

        BusinessModuleScaffolder.scaffold(projectRoot, "asset", "assetdevice");

        assertTrue(Files.isRegularFile(projectRoot.resolve("business/asset/pom.xml")));
        assertTrue(Files.isRegularFile(projectRoot.resolve("business/asset/asset-app/pom.xml")));
        assertTrue(Files.isRegularFile(projectRoot.resolve("business/asset/asset-port/pom.xml")));
        assertTrue(Files.isRegularFile(projectRoot.resolve("business/deploy/asset-deploy/pom.xml")));
        assertTrue(Files.isRegularFile(projectRoot.resolve(
                "business/deploy/asset-deploy/src/test/java/com/kacean/power/deploy/ArchitectureTest.java")));

        String businessPom = Files.readString(projectRoot.resolve("business/asset/pom.xml"));
        assertTrue(businessPom.contains("<packaging>pom</packaging>"));
        assertTrue(businessPom.contains("<module>asset-app</module>"));
        assertTrue(businessPom.contains("<module>asset-port</module>"));
        assertTrue(Files.readString(projectRoot.resolve("business/pom.xml")).contains("<module>asset</module>"));
        assertTrue(Files.readString(projectRoot.resolve("business/deploy/pom.xml"))
                .contains("<module>asset-deploy</module>"));
        assertTrue(Files.readString(projectRoot.resolve("runner/standalone-runner/pom.xml"))
                .contains("<artifactId>asset-deploy</artifactId>"));
        String portPom = Files.readString(projectRoot.resolve("business/asset/asset-port/pom.xml"));
        assertTrue(portPom.contains("<artifactId>common-infra</artifactId>"));
        assertTrue(portPom.contains("<artifactId>common-web</artifactId>"));
        assertTrue(portPom.contains("<artifactId>asset-app</artifactId>"));
        assertFalse(portPom.contains("<artifactId>mybatis-plus-core</artifactId>"));
        assertFalse(portPom.contains("<artifactId>spring-webmvc</artifactId>"));
    }

    @Test
    void 重复创建业务模块被拒绝_不修改已有聚合配置() throws IOException {
        prepareProject();
        BusinessModuleScaffolder.scaffold(projectRoot, "asset", "assetdevice");
        String original = Files.readString(projectRoot.resolve("business/pom.xml"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> BusinessModuleScaffolder.scaffold(projectRoot, "asset", "assetdevice"));

        assertTrue(exception.getMessage().contains("业务目录已存在"));
        assertEquals(original, Files.readString(projectRoot.resolve("business/pom.xml")));
    }

    private void prepareProject() throws IOException {
        write("pom.xml", "<project/>\n");
        write("business/pom.xml", """
                <project>
                    <modules>
                        <module>ticket</module>
                        <module>deploy</module>
                    </modules>
                </project>
                """);
        write("business/deploy/pom.xml", """
                <project>
                    <modules>
                        <module>ticket-deploy</module>
                    </modules>
                </project>
                """);
        write("runner/standalone-runner/pom.xml", """
                <project>
                    <dependencies>
                        <dependency>
                            <artifactId>ticket-deploy</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """);
    }

    private void write(String relativePath, String content) throws IOException {
        Path path = projectRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
