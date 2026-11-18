package com.kacean.power.codegen;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.Entity;
import com.baomidou.mybatisplus.generator.config.builder.Mapper;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * MyBatis-Plus 持久化代码生成器：按项目子域边界输出 PO、Mapper 与 XML，不生成应用服务和控制器。
 *
 * @author kacean-agent
 */
final class PersistenceCodeGenerator {

    private static final Pattern BUSINESS_PATTERN = Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");
    private static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("[a-z][a-z0-9]*");
    private static final Pattern TABLE_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern COLUMN_PATTERN = Pattern.compile("[A-Za-z0-9_]*");
    private static final String BASE_ENTITY_CLASS = "com.kacean.power.common.infra.po.BaseEntity";
    private static final String BASE_MAPPER_CLASS = "com.kacean.power.common.infra.mapper.BaseMapper";
    private static final List<String> BASE_ENTITY_COLUMNS = List.of(
            "id", "external_no", "deleted", "create_at", "create_by", "modify_at", "modify_by", "version");

    private PersistenceCodeGenerator() {
    }

    static void generate(Options options) {
        validate(options);
        Path portDirectory = options.projectRoot().resolve("business")
                .resolve(options.businessDomain()).resolve(options.businessDomain() + "-port");
        Path portPom = portDirectory.resolve("pom.xml");
        if (!Files.isRegularFile(portPom)) {
            throw new IllegalArgumentException("目标 port 模块不存在：" + portPom + "，请先执行 module 命令");
        }

        String persistencePackage = "com.kacean.power." + options.subdomain() + ".port.infra.persistence";
        Path javaRoot = portDirectory.resolve("src/main/java");
        Path packageRoot = javaRoot.resolve(persistencePackage.replace('.', '/'));
        Path entityOutput = packageRoot.resolve("po");
        Path mapperOutput = packageRoot.resolve("mapper");
        Path xmlOutput = portDirectory.resolve("src/main/resources/mapper").resolve(options.subdomain());

        DataSourceConfig.Builder dataSource = new DataSourceConfig.Builder(
                options.jdbcUrl(), options.username(), options.password());
        if (!options.schema().isBlank()) {
            dataSource.schema(options.schema());
        }

        Map<OutputFile, String> pathInfo = new EnumMap<>(OutputFile.class);
        pathInfo.put(OutputFile.entity, entityOutput.toString());
        pathInfo.put(OutputFile.mapper, mapperOutput.toString());
        pathInfo.put(OutputFile.xml, xmlOutput.toString());
        pathInfo.put(OutputFile.parent, packageRoot.toString());

        FastAutoGenerator.create(dataSource)
                .globalConfig(builder -> builder.author("kacean-agent")
                        .outputDir(javaRoot.toString())
                        .disableOpenDir())
                .packageConfig(builder -> builder.parent(persistencePackage)
                        .entity("po")
                        .mapper("mapper")
                        .xml("mapper")
                        .pathInfo(pathInfo))
                .strategyConfig(builder -> {
                    builder.addInclude(options.tables());
                    if (!options.tablePrefixes().isEmpty()) {
                        builder.addTablePrefix(options.tablePrefixes());
                    }
                    Entity.Builder entity = builder.entityBuilder()
                            .formatFileName("%sPO")
                            .javaTemplate("/templates/persistence-po.java")
                            .superClass(BASE_ENTITY_CLASS)
                            .addSuperEntityColumns(BASE_ENTITY_COLUMNS)
                            .disableSerialVersionUID()
                            .enableRemoveIsPrefix()
                            .enableTableFieldAnnotation();
                    if (options.overwrite()) {
                        entity.enableFileOverride();
                    }

                    Mapper.Builder mapper = builder.mapperBuilder()
                            .superClass(BASE_MAPPER_CLASS)
                            .mapperAnnotation(org.apache.ibatis.annotations.Mapper.class)
                            .mapperTemplate("/templates/persistence-mapper.java")
                            .mapperXmlTemplate("/templates/persistence-mapper.xml")
                            .enableBaseResultMap()
                            .enableBaseColumnList();
                    if (options.overwrite()) {
                        mapper.enableFileOverride();
                    }
                    builder.serviceBuilder().disable();
                    builder.controllerBuilder().disable();
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        ensurePersistenceDependencies(portPom);
    }

    private static void ensurePersistenceDependencies(Path portPom) {
        try {
            String content = Files.readString(portPom);
            content = addDependencyIfAbsent(content, "common-infra", """
                        <!-- BaseEntity：由代码生成器补充 -->
                        <dependency>
                            <groupId>com.kacean.power</groupId>
                            <artifactId>common-infra</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                    """.stripTrailing());
            content = addDependencyIfAbsent(content, "mybatis-plus-core", """
                        <!-- MyBatis-Plus PO 与 Mapper：由代码生成器补充 -->
                        <dependency>
                            <groupId>com.baomidou</groupId>
                            <artifactId>mybatis-plus-core</artifactId>
                        </dependency>
                    """.stripTrailing());
            Files.writeString(portPom, content);
        } catch (IOException exception) {
            throw new IllegalStateException("更新目标 port POM 失败：" + portPom, exception);
        }
    }

    private static String addDependencyIfAbsent(String content, String artifactId, String dependency) {
        if (content.contains("<artifactId>" + artifactId + "</artifactId>")) {
            return content;
        }
        String marker = "    </dependencies>";
        if (!content.contains(marker)) {
            throw new IllegalArgumentException("目标 port POM 缺少 dependencies 结束标记");
        }
        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        return content.replace(marker, dependency + newline + marker);
    }

    private static void validate(Options options) {
        requirePattern(options.businessDomain(), BUSINESS_PATTERN, "业务域名");
        requirePattern(options.subdomain(), SUBDOMAIN_PATTERN, "子域名");
        if (options.jdbcUrl() == null || !options.jdbcUrl().startsWith("jdbc:")) {
            throw new IllegalArgumentException("JDBC URL 必须以 jdbc: 开头");
        }
        if (options.tables().isEmpty()) {
            throw new IllegalArgumentException("至少指定一张表");
        }
        options.tables().forEach(table -> requirePattern(table, TABLE_PATTERN, "表名"));
        options.tablePrefixes().forEach(prefix -> requirePattern(prefix, COLUMN_PATTERN, "表前缀"));
    }

    private static void requirePattern(String value, Pattern pattern, String label) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(label + "格式非法：" + value + "，允许规则 " + pattern.pattern());
        }
    }

    /**
     * 持久化代码生成参数。
     *
     * @author kacean-agent
     * @param projectRoot 项目根目录
     * @param businessDomain 业务域目录与模块前缀
     * @param subdomain 全局唯一的 Java 子域包名
     * @param jdbcUrl 数据库 JDBC URL
     * @param username 数据库用户名
     * @param password 数据库密码
     * @param schema 可选数据库 schema
     * @param tables 要生成的表名
     * @param tablePrefixes 生成类名时移除的表前缀
     * @param overwrite 是否覆盖已有同名文件
     */
    record Options(Path projectRoot, String businessDomain, String subdomain, String jdbcUrl, String username,
                   String password, String schema, List<String> tables, List<String> tablePrefixes, boolean overwrite) {
        Options {
            tables = List.copyOf(tables);
            tablePrefixes = List.copyOf(tablePrefixes);
        }
    }
}
