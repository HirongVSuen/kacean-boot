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
            createTicketTypeTable(statement);
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
        assertTrue(poSource.contains("import com.kacean.power.common.infra.po.BaseEntity;"));
        assertTrue(poSource.contains("public class TicketTypePO extends BaseEntity"));
        assertTrue(poSource.contains("@TableName(\"ticket_type\")"));
        assertTrue(poSource.contains("private String typeName;"));
        assertTrue(poSource.contains("@author kacean-agent"));
        assertCommonFieldsInherited(poSource);
        assertFalse(poSource.contains("@TableId"));
        assertFalse(poSource.contains("IdType."));
        assertFalse(poSource.contains("@Version"));
        assertFalse(poSource.contains("@TableLogic"));

        String mapperSource = Files.readString(mapper);
        assertTrue(mapperSource.contains("package com.kacean.power.tickettype.port.infra.persistence.mapper;"));
        assertTrue(mapperSource.contains("import com.kacean.power.tickettype.port.infra.persistence.po.TicketTypePO;"));
        assertTrue(mapperSource.contains("import com.kacean.power.common.infra.mapper.BaseMapper;"));
        assertTrue(mapperSource.contains("public interface TicketTypeMapper extends BaseMapper<TicketTypePO>"));
        assertTrue(mapperSource.contains("@Mapper"));
        String xmlSource = Files.readString(xml);
        assertTrue(xmlSource.contains(
                "namespace=\"com.kacean.power.tickettype.port.infra.persistence.mapper.TicketTypeMapper\""));
        assertTrue(xmlSource.contains("<id column=\"id\" property=\"id\" />"));
        for (String column : List.of("external_no", "deleted", "create_at", "create_by",
                "modify_at", "modify_by", "version", "type_name")) {
            assertTrue(xmlSource.contains("column=\"" + column + "\""), "XML 缺少字段映射：" + column);
        }

        assertFalse(Files.exists(projectRoot.resolve(
                "business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/TicketTypePO.java")));
        assertFalse(Files.exists(projectRoot.resolve(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web")));
        String portPom = Files.readString(projectRoot.resolve("business/ticket/ticket-port/pom.xml"));
        assertEquals(1, occurrences(portPom, "<artifactId>common-infra</artifactId>"));
        assertEquals(1, occurrences(portPom, "<artifactId>mybatis-plus-core</artifactId>"));
    }

    @Test
    void 覆盖开关控制同名文件_重复执行不重复添加依赖() throws Exception {
        prepareProject();
        String jdbcUrl = createDatabase();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            createTicketTypeTable(statement);
        }
        PersistenceCodeGenerator.generate(options(jdbcUrl, false));
        Path po = generatedPo();
        Files.writeString(po, "保留手工修改\n");

        PersistenceCodeGenerator.generate(options(jdbcUrl, false));

        assertEquals("保留手工修改\n", Files.readString(po));
        String portPom = Files.readString(projectRoot.resolve("business/ticket/ticket-port/pom.xml"));
        assertEquals(1, occurrences(portPom, "<artifactId>common-infra</artifactId>"));
        assertEquals(1, occurrences(portPom, "<artifactId>mybatis-plus-core</artifactId>"));

        PersistenceCodeGenerator.generate(options(jdbcUrl, true));
        assertTrue(Files.readString(po).contains("public class TicketTypePO extends BaseEntity"));
        portPom = Files.readString(projectRoot.resolve("business/ticket/ticket-port/pom.xml"));
        assertEquals(1, occurrences(portPom, "<artifactId>common-infra</artifactId>"));
        assertEquals(1, occurrences(portPom, "<artifactId>mybatis-plus-core</artifactId>"));
    }

    private void createTicketTypeTable(Statement statement) throws Exception {
        statement.execute("""
                create table ticket_type (
                    id bigint primary key,
                    external_no varchar(40) not null,
                    deleted integer not null default 0,
                    create_at timestamp(3) not null,
                    create_by varchar(64) not null,
                    modify_at timestamp(3) not null,
                    modify_by varchar(64) not null,
                    version bigint not null default 0,
                    type_name varchar(64) not null
                )
                """);
    }

    private PersistenceCodeGenerator.Options options(String jdbcUrl, boolean overwrite) {
        return new PersistenceCodeGenerator.Options(projectRoot, "ticket", "tickettype", jdbcUrl, "sa", "", "",
                List.of("ticket_type"), List.of(), overwrite);
    }

    private void assertCommonFieldsInherited(String poSource) {
        for (String property : List.of("id", "externalNo", "deleted", "createAt", "createBy",
                "modifyAt", "modifyBy", "version")) {
            assertFalse(poSource.matches("(?s).*\\bprivate\\s+\\S+\\s+" + property + "\\s*;.*"),
                    "公共字段不应重复生成：" + property);
        }
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
