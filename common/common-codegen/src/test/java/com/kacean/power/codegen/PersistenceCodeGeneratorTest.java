package com.kacean.power.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 持久化代码生成器集成测试：使用 H2 元数据验证子域输出位置、MyBatis-Plus 映射与覆盖保护。
 *
 * @author kacean-agent
 */
class PersistenceCodeGeneratorTest {

    @TempDir
    Path projectRoot;

    @Test
    void 根据数据表生成持久化代码_文件落入对应子域且不污染应用层() throws Exception {
        prepareProject();
        String jdbcUrl = createDatabase();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table ticket_type (
                        id varchar(36) primary key,
                        type_name varchar(64) not null,
                        version bigint,
                        deleted boolean
                    )
                    """);
        }

        PersistenceCodeGenerator.generate(options(jdbcUrl, false));

        Path po = generatedPo();
        Path mapper = generatedMapper();
        Path xml = projectRoot.resolve(
                "business/ticket/ticket-port/src/main/resources/mapper/tickettype/TicketTypeMapper.xml");
        assertTrue(Files.isRegularFile(po));
        assertTrue(Files.isRegularFile(mapper));
        assertTrue(Files.isRegularFile(xml));

        String poSource = Files.readString(po);
        assertTrue(poSource.contains("public class TicketTypePO"));
        assertTrue(poSource.contains("@TableName(\"ticket_type\")"));
        assertTrue(poSource.contains("IdType.ASSIGN_UUID"));
        assertTrue(poSource.contains("@Version"));
        assertTrue(poSource.contains("@TableLogic"));
        assertTrue(poSource.contains("@author kacean-agent"));

        String mapperSource = Files.readString(mapper);
        assertTrue(mapperSource.contains("package com.kacean.power.tickettype.port.infra.persistence.mapper;"));
        assertTrue(mapperSource.contains("import com.kacean.power.tickettype.port.infra.persistence.po.TicketTypePO;"));
        assertTrue(mapperSource.contains("public interface TicketTypeMapper extends BaseMapper<TicketTypePO>"));
        assertTrue(mapperSource.contains("@Mapper"));
        assertTrue(Files.readString(xml).contains(
                "namespace=\"com.kacean.power.tickettype.port.infra.persistence.mapper.TicketTypeMapper\""));

        assertFalse(Files.exists(projectRoot.resolve(
                "business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/TicketTypePO.java")));
        assertFalse(Files.exists(projectRoot.resolve(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web")));
        String portPom = Files.readString(projectRoot.resolve("business/ticket/ticket-port/pom.xml"));
        assertEquals(1, occurrences(portPom, "<artifactId>mybatis-plus-core</artifactId>"));
    }

    @Test
    void 覆盖开关控制同名文件_重复执行不重复添加依赖() throws Exception {
        prepareProject();
        String jdbcUrl = createDatabase();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("create table ticket_type (id varchar(36) primary key, version bigint, deleted boolean)");
        }
        PersistenceCodeGenerator.generate(options(jdbcUrl, false));
        Path po = generatedPo();
        Files.writeString(po, "保留手工修改\n");

        PersistenceCodeGenerator.generate(options(jdbcUrl, false));

        assertEquals("保留手工修改\n", Files.readString(po));
        String portPom = Files.readString(projectRoot.resolve("business/ticket/ticket-port/pom.xml"));
        assertEquals(1, occurrences(portPom, "<artifactId>mybatis-plus-core</artifactId>"));

        PersistenceCodeGenerator.generate(options(jdbcUrl, true));
        assertTrue(Files.readString(po).contains("public class TicketTypePO"));
        assertEquals(1, occurrences(Files.readString(projectRoot.resolve("business/ticket/ticket-port/pom.xml")),
                "<artifactId>mybatis-plus-core</artifactId>"));
    }

    private PersistenceCodeGenerator.Options options(String jdbcUrl, boolean overwrite) {
        return new PersistenceCodeGenerator.Options(projectRoot, "ticket", "tickettype", jdbcUrl, "sa", "", "",
                List.of("ticket_type"), List.of(), "ASSIGN_UUID", "deleted", "version", overwrite);
    }

    private Path generatedPo() {
        return projectRoot.resolve(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/persistence/po/TicketTypePO.java");
    }

    private Path generatedMapper() {
        return projectRoot.resolve(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/persistence/mapper/TicketTypeMapper.java");
    }

    private String createDatabase() {
        return "jdbc:h2:mem:codegen_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
    }

    private void prepareProject() throws IOException {
        write("pom.xml", "<project/>\n");
        write("business/ticket/ticket-app/pom.xml", "<project/>\n");
        write("business/ticket/ticket-port/pom.xml", """
                <project>
                    <dependencies>
                        <dependency>
                            <artifactId>ticket-app</artifactId>
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

    private static int occurrences(String content, String target) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
