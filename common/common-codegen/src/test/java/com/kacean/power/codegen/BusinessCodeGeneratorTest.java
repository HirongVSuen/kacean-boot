package com.kacean.power.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务代码生成器集成测试：使用 H2 元数据验证贫血/DDD 产物、覆盖保护与生成源码可编译性。
 *
 * @author kacean-agent
 */
class BusinessCodeGeneratorTest {

    @TempDir
    Path projectRoot;

    /**
     * 贫血模式生成完整 web/infra 代码，不生成 DDD 专属文件。
     */
    @Test
    void 根据数据表生成贫血代码_文件落入对应分包且不生成领域代码() throws Exception {
        prepareProject();
        String jdbcUrl = createTicketTypeDatabase();

        BusinessCodeGenerator.generate(options(jdbcUrl, BusinessCodeGenerator.GenerationMode.ANEMIC, false));

        Path entity = generatedEntity();
        Path mapper = generatedMapper();
        Path xml = path("business/ticket/ticket-port/src/main/resources/mapper/tickettype/TicketTypeMapper.xml");
        assertTrue(Files.isRegularFile(entity));
        assertTrue(Files.isRegularFile(mapper));
        assertTrue(Files.isRegularFile(xml));
        for (String relativePath : anemicJavaFiles()) {
            assertTrue(Files.isRegularFile(path(relativePath)), "缺少贫血模式产物：" + relativePath);
        }
        for (String relativePath : dddOnlyJavaFiles()) {
            assertFalse(Files.exists(path(relativePath)), "贫血模式不应生成 DDD 专属产物：" + relativePath);
        }

        String entitySource = Files.readString(entity);
        assertTrue(entitySource.contains("package com.kacean.power.tickettype.port.infra.entity;"));
        assertTrue(entitySource.contains("import com.kacean.power.common.infra.entity.BaseEntity;"));
        assertTrue(entitySource.contains("public class TicketTypeEntity extends BaseEntity"));
        assertTrue(entitySource.contains("@TableName(\"ticket_type\")"));
        assertTrue(entitySource.contains("import lombok.Getter;"));
        assertTrue(entitySource.contains("import lombok.Setter;"));
        assertTrue(entitySource.contains("@Getter"));
        assertTrue(entitySource.contains("@Setter"));
        assertTrue(entitySource.contains("private String typeName;"));
        assertTrue(entitySource.contains("@author kacean-agent"));
        assertFalse(entitySource.contains("public String getTypeName()"));
        assertFalse(entitySource.contains("public void setTypeName("));
        assertCommonFieldsInherited(entitySource);
        assertFalse(entitySource.contains("@TableId"));
        assertFalse(entitySource.contains("IdType."));
        assertFalse(entitySource.contains("@Version"));
        assertFalse(entitySource.contains("@TableLogic"));

        String mapperSource = Files.readString(mapper);
        assertTrue(mapperSource.contains("package com.kacean.power.tickettype.port.infra.mapper;"));
        assertTrue(mapperSource.contains("import com.kacean.power.tickettype.port.infra.entity.TicketTypeEntity;"));
        assertTrue(mapperSource.contains("import com.kacean.power.common.infra.mapper.BaseMapper;"));
        assertTrue(mapperSource.contains("public interface TicketTypeMapper extends BaseMapper<TicketTypeEntity>"));
        assertTrue(mapperSource.contains("@Mapper"));
        String xmlSource = Files.readString(xml);
        assertTrue(xmlSource.contains(
                "namespace=\"com.kacean.power.tickettype.port.infra.mapper.TicketTypeMapper\""));
        assertTrue(xmlSource.contains(
                "type=\"com.kacean.power.tickettype.port.infra.entity.TicketTypeEntity\""));
        assertTrue(xmlSource.contains("<id column=\"id\" property=\"id\" />"), xmlSource);
        for (String column : List.of("external_no", "deleted", "create_at", "create_by",
                "modify_at", "modify_by", "version", "type_name")) {
            assertTrue(xmlSource.contains("column=\"" + column + "\""), "XML 缺少字段映射：" + column);
        }

        String dtoSource = Files.readString(path(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web/dto/TicketTypeDTO.java"));
        assertTrue(dtoSource.contains("public class TicketTypeDTO extends BaseDTO"));
        assertTrue(dtoSource.contains("import lombok.Getter;"));
        assertTrue(dtoSource.contains("import lombok.Setter;"));
        assertTrue(dtoSource.contains("@Getter"));
        assertTrue(dtoSource.contains("@Setter"));
        assertTrue(dtoSource.contains("private Long id;"));
        assertTrue(dtoSource.contains("private String externalNo;"));
        assertTrue(dtoSource.contains("private String typeName;"));
        assertTrue(dtoSource.contains("private LocalDateTime createAt;"));
        assertFalse(dtoSource.contains("deleted"), "DTO 不应生成逻辑删除字段");
        assertTrue(dtoSource.contains("import io.swagger.v3.oas.annotations.media.Schema;"));
        assertTrue(dtoSource.contains("@Schema(description = "));
        assertTrue(dtoSource.contains("@Schema(description = \"业务字段\", example = \"2026-01-01 12:00:00\")"));
        assertTrue(dtoSource.contains("import com.fasterxml.jackson.annotation.JsonFormat;"));
        assertTrue(dtoSource.contains("@JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")"));
        assertFalse(dtoSource.contains("public String getTypeName()"));
        assertFalse(dtoSource.contains("public void setTypeName("));
        String controllerSource = Files.readString(path(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web/controller/TicketTypeController.java"));
        assertTrue(controllerSource.contains("@RequestMapping(\"/ticket-type\")"));
        assertTrue(controllerSource.contains("extends CurdController<TicketTypeDTO, TicketTypeEntity, TicketTypeCurd>"));
        assertFalse(controllerSource.contains("public TicketTypeController("), "控制器由父类字段注入，不再生成构造器");

        assertGeneratedDependencies("anemic");
        assertFalse(Files.readString(path("business/ticket/ticket-app/pom.xml")).contains("common-app"));
        compileGeneratedSources();
    }

    /**
     * DDD 模式保留贫血产物，并追加 app domain/repo 与 infra convert/repo。
     */
    @Test
    void DDD模式在贫血产物上追加领域与仓储代码_生成源码可直接编译() throws Exception {
        prepareProject();
        write("business/ticket/ticket-port/pom.xml", """
                <project>
                    <dependencies>
                    </dependencies>
                </project>
                """);
        String jdbcUrl = createTicketTypeDatabase();

        BusinessCodeGenerator.generate(options(jdbcUrl, BusinessCodeGenerator.GenerationMode.DDD, false));

        for (String relativePath : Stream.concat(anemicJavaFiles().stream(), dddOnlyJavaFiles().stream()).toList()) {
            assertTrue(Files.isRegularFile(path(relativePath)), "缺少 DDD 模式产物：" + relativePath);
        }
        String domainSource = Files.readString(path(
                "business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/TicketType.java"));
        assertTrue(domainSource.contains("public class TicketType implements BaseDomain"));
        assertTrue(domainSource.contains("import lombok.Getter;"));
        assertTrue(domainSource.contains("import lombok.Setter;"));
        assertTrue(domainSource.contains("@Getter"));
        assertTrue(domainSource.contains("@Setter"));
        assertTrue(domainSource.contains("private Long id;"));
        assertTrue(domainSource.contains("private String typeName;"));
        assertTrue(domainSource.contains("private LocalDateTime createAt;"));
        assertFalse(domainSource.contains("deleted"), "领域对象不应生成逻辑删除字段");
        assertFalse(domainSource.contains("JsonFormat"), "领域对象不应引入 Jackson 注解");
        assertFalse(domainSource.contains("Schema"), "领域对象不应引入 Swagger 注解");
        assertFalse(domainSource.contains("public String getTypeName()"));
        assertFalse(domainSource.contains("public void setTypeName("));
        String repositorySource = Files.readString(path(
                "business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/repo/TicketTypeRepository.java"));
        assertTrue(repositorySource.contains("extends BaseRepository<TicketType>"));
        String convertSource = Files.readString(path(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/convert/TicketTypeConvert.java"));
        assertTrue(convertSource.contains("import com.kacean.power.common.infra.repo.BaseConvert;"));
        assertTrue(convertSource.contains("extends BaseConvert<TicketType, TicketTypeEntity>"));
        String repositoryImplSource = Files.readString(path(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/repo/TicketTypeRepositoryImpl.java"));
        assertTrue(repositoryImplSource.contains("package com.kacean.power.tickettype.port.infra.repo;"));
        assertTrue(repositoryImplSource.contains("implements TicketTypeRepository"));
        assertTrue(repositoryImplSource.contains("extends BaseRepositoryImpl<TicketType, TicketTypeEntity, TicketTypeDao>"));
        assertFalse(repositoryImplSource.contains("super(dao, converter);"), "仓储实现由父类字段注入，不再生成构造器");
        String assemblerSource = Files.readString(path(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web/assembler/TicketTypeAssembler.java"));
        assertTrue(assemblerSource.contains("extends DddAssembler<TicketTypeDTO, TicketType, TicketTypeEntity>"));
        String controllerSource = Files.readString(path(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web/controller/TicketTypeController.java"));
        assertTrue(controllerSource.contains(
                "extends DddController<TicketTypeDTO, TicketType, TicketTypeEntity, TicketTypeCurd>"));

        assertGeneratedDependencies("ddd");
        String appPom = Files.readString(path("business/ticket/ticket-app/pom.xml"));
        assertEquals(1, occurrences(appPom, "<artifactId>common-app</artifactId>"));
        compileGeneratedSources();
    }

    /**
     * 默认保留手工修改，显式覆盖时重新生成，重复执行不重复添加依赖。
     */
    @Test
    void 覆盖开关控制同名文件_重复执行不重复添加依赖() throws Exception {
        prepareProject();
        String jdbcUrl = createTicketTypeDatabase();
        BusinessCodeGenerator.generate(options(jdbcUrl, BusinessCodeGenerator.GenerationMode.ANEMIC, false));
        Path entity = generatedEntity();
        Path controller = path(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web/controller/TicketTypeController.java");
        Path mapper = generatedMapper();
        Path xml = path("business/ticket/ticket-port/src/main/resources/mapper/tickettype/TicketTypeMapper.xml");
        Files.writeString(entity, "保留手工修改\n");
        Files.writeString(controller, "保留控制器修改\n");
        Files.writeString(mapper, "保留 Mapper 修改\n");
        Files.writeString(xml, "保留 XML 修改\n");

        BusinessCodeGenerator.generate(options(jdbcUrl, BusinessCodeGenerator.GenerationMode.ANEMIC, false));

        assertEquals("保留手工修改\n", Files.readString(entity));
        assertEquals("保留控制器修改\n", Files.readString(controller));
        assertEquals("保留 Mapper 修改\n", Files.readString(mapper));
        assertEquals("保留 XML 修改\n", Files.readString(xml));
        assertGeneratedDependencies("anemic");

        BusinessCodeGenerator.generate(options(jdbcUrl, BusinessCodeGenerator.GenerationMode.ANEMIC, true));
        assertTrue(Files.readString(entity).contains("public class TicketTypeEntity extends BaseEntity"));
        assertTrue(Files.readString(controller).contains("public class TicketTypeController"));
        assertTrue(Files.readString(mapper).contains("public interface TicketTypeMapper"));
        assertTrue(Files.readString(xml).contains("<mapper namespace="));
        assertGeneratedDependencies("anemic");
    }

    /**
     * 贫血代码可增量追加 DDD 产物，已有 Web 文件只有显式覆盖时才切换父类。
     */
    @Test
    void 贫血升级DDD_默认保留已有Web修改_显式覆盖后切换DDD父类() throws Exception {
        prepareProject();
        String jdbcUrl = createTicketTypeDatabase();
        BusinessCodeGenerator.generate(options(jdbcUrl, BusinessCodeGenerator.GenerationMode.ANEMIC, false));
        Path controller = path(
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web/controller/TicketTypeController.java");

        BusinessCodeGenerator.generate(options(jdbcUrl, BusinessCodeGenerator.GenerationMode.DDD, false));

        for (String relativePath : dddOnlyJavaFiles()) {
            assertTrue(Files.isRegularFile(path(relativePath)), "升级缺少 DDD 产物：" + relativePath);
        }
        assertTrue(Files.readString(controller).contains("extends CurdController<"));
        assertGeneratedDependencies("ddd");

        BusinessCodeGenerator.generate(options(jdbcUrl, BusinessCodeGenerator.GenerationMode.DDD, true));

        assertTrue(Files.readString(controller).contains(
                "extends DddController<TicketTypeDTO, TicketType, TicketTypeEntity, TicketTypeCurd>"));
        assertGeneratedDependencies("ddd");
        compileGeneratedSources();
    }

    /**
     * 创建 H2 表并返回独立数据库 URL。
     *
     * @return JDBC URL
     */
    private String createTicketTypeDatabase() throws Exception {
        String jdbcUrl = createDatabase();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
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
        return jdbcUrl;
    }

    /**
     * 构造生成选项。
     *
     * @param jdbcUrl JDBC URL
     * @param mode 生成模式
     * @param overwrite 是否覆盖
     * @return 生成选项
     */
    private BusinessCodeGenerator.Options options(
            String jdbcUrl, BusinessCodeGenerator.GenerationMode mode, boolean overwrite) {
        return new BusinessCodeGenerator.Options(projectRoot, "ticket", "tickettype", jdbcUrl, "sa", "", "",
                List.of("ticket_type"), List.of(), mode, overwrite);
    }

    /**
     * 公共持久化字段只能由 BaseEntity 继承。
     *
     * @param entitySource Entity 源码
     */
    private void assertCommonFieldsInherited(String entitySource) {
        for (String property : List.of("id", "externalNo", "deleted", "createAt", "createBy",
                "modifyAt", "modifyBy", "version")) {
            assertFalse(entitySource.matches("(?s).*\\bprivate\\s+\\S+\\s+" + property + "\\s*;.*"),
                    "公共字段不应重复生成：" + property);
        }
    }

    /**
     * 编译所有生成 Java 源码，校验模板 import、泛型和继承关系。
     */
    private void compileGeneratedSources() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "测试需要 JDK 编译器");
        List<Path> sources;
        try (Stream<Path> paths = Files.walk(projectRoot.resolve("business/ticket"))) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        assertFalse(sources.isEmpty());
        Path classes = Files.createDirectories(projectRoot.resolve("generated-classes"));
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sources);
            List<String> compilerOptions = List.of("-proc:none", "-classpath", classPath, "-d", classes.toString());
            boolean success = compiler.getTask(null, fileManager, diagnostics, compilerOptions, null, units).call();
            String messages = diagnostics.getDiagnostics().stream()
                    .map(diagnostic -> formatDiagnostic(diagnostic, sources))
                    .collect(Collectors.joining("\n"));
            assertTrue(success, "生成源码编译失败：\n" + messages);
        }
    }

    /**
     * 格式化编译诊断。
     *
     * @param diagnostic 编译诊断
     * @param sources 生成源码
     * @return 可读诊断
     */
    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic, List<Path> sources) {
        String source = diagnostic.getSource() == null ? "unknown" : diagnostic.getSource().getName();
        return source + ":" + diagnostic.getLineNumber() + " " + diagnostic.getMessage(null);
    }

    private Path generatedEntity() {
        return path("business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/entity/TicketTypeEntity.java");
    }

    private Path generatedMapper() {
        return path("business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/mapper/TicketTypeMapper.java");
    }

    private Path path(String relativePath) {
        return projectRoot.resolve(relativePath);
    }

    private List<String> anemicJavaFiles() {
        String infra = "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/";
        String web = "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/web/";
        return List.of(
                infra + "entity/TicketTypeEntity.java",
                infra + "mapper/TicketTypeMapper.java",
                infra + "dao/TicketTypeDao.java",
                infra + "dao/TicketTypeDaoImpl.java",
                infra + "curd/TicketTypeCurd.java",
                web + "dto/TicketTypeDTO.java",
                web + "assembler/TicketTypeAssembler.java",
                web + "controller/TicketTypeController.java");
    }

    private List<String> dddOnlyJavaFiles() {
        return List.of(
                "business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/domain/TicketType.java",
                "business/ticket/ticket-app/src/main/java/com/kacean/power/tickettype/app/repo/TicketTypeRepository.java",
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/convert/TicketTypeConvert.java",
                "business/ticket/ticket-port/src/main/java/com/kacean/power/tickettype/port/infra/repo/TicketTypeRepositoryImpl.java");
    }

    private void assertGeneratedDependencies(String mode) throws IOException {
        String portPom = Files.readString(path("business/ticket/ticket-port/pom.xml"));
        for (String artifact : List.of("ticket-app", "common-infra", "common-web")) {
            assertEquals(1, occurrences(portPom, "<artifactId>" + artifact + "</artifactId>"),
                    mode + " 模式依赖重复或缺失：" + artifact);
        }
        for (String artifact : List.of("mybatis-plus-core", "spring-webmvc")) {
            assertFalse(portPom.contains("<artifactId>" + artifact + "</artifactId>"),
                    mode + " 模式不应重复声明传递依赖：" + artifact);
        }
    }

    private String createDatabase() {
        return "jdbc:h2:mem:codegen_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
    }

    private void prepareProject() throws IOException {
        write("pom.xml", "<project/>\n");
        write("business/ticket/ticket-app/pom.xml", """
                <project>
                    <dependencies>
                    </dependencies>
                </project>
                """);
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
        Path path = path(relativePath);
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
