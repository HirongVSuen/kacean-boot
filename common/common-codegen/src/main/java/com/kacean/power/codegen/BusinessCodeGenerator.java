package com.kacean.power.codegen;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import com.baomidou.mybatisplus.generator.fill.Property;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 业务代码生成器：按表生成持久化代码，并按贫血或 DDD 模式补齐业务分包代码。
 * 默认不覆盖已存在文件；--overwrite 由使用者显式开启。
 *
 * @author kacean-agent
 */
public final class BusinessCodeGenerator {

    private static final String BUSINESS_AUTHOR = "kacean-agent";
    private static final Pattern BUSINESS_PATTERN = Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");
    private static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("[a-z][a-z0-9]*");
    private static final Pattern TABLE_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern COLUMN_PATTERN = Pattern.compile("[A-Za-z0-9_]*");
    private static final String[] SUPER_ENTITY_COLUMNS = {
        "id", "external_no", "deleted", "create_at", "create_by",
        "modify_at", "modify_by", "version"
    };

    private BusinessCodeGenerator() {
    }

    /**
     * 业务代码生成模式。
     *
     * @author kacean-agent
     */
    public enum GenerationMode {
        /** 贫血模式：持久化、CURD、DTO、Assembler 与 Controller。 */
        ANEMIC,
        /** DDD 模式：贫血模式全部产物，加领域模型、仓储契约与仓储适配。 */
        DDD;

        /**
         * 解析命令行模式名称。
         *
         * @param value anemic 或 ddd
         * @return 生成模式
         */
        public static GenerationMode from(String value) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("无效 --mode: " + value + "，可选值: anemic、ddd", exception);
            }
        }
    }

    /**
     * 业务代码生成选项。
     *
     * @author kacean-agent
     * @param projectRoot 项目根目录
     * @param businessDomain 业务域目录与模块前缀
     * @param subdomain 全局唯一的 Java 子域包名
     * @param jdbcUrl 数据库 JDBC URL
     * @param username 数据库用户名
     * @param password 数据库密码，可来自环境变量
     * @param schema 可选数据库 schema
     * @param tables 要生成的表
     * @param tablePrefixes 生成类名时移除的表前缀
     * @param mode 生成模式
     * @param overwrite 是否覆盖已有生成文件
     */
    public record Options(
        Path projectRoot,
        String businessDomain,
        String subdomain,
        String jdbcUrl,
        String username,
        String password,
        String schema,
        List<String> tables,
        List<String> tablePrefixes,
        GenerationMode mode,
        boolean overwrite
    ) {
        /**
         * 防御性复制集合并校验必填选项。
         */
        public Options {
            Objects.requireNonNull(projectRoot, "projectRoot 不能为空");
            Objects.requireNonNull(jdbcUrl, "jdbcUrl 不能为空");
            Objects.requireNonNull(username, "username 不能为空");
            Objects.requireNonNull(password, "password 不能为空");
            Objects.requireNonNull(schema, "schema 不能为空");
            Objects.requireNonNull(mode, "mode 不能为空");
            tables = List.copyOf(tables);
            tablePrefixes = List.copyOf(tablePrefixes);
        }
    }

    /**
     * 生成业务代码。
     *
     * @param options 生成选项
     */
    public static void generate(Options options) {
        Objects.requireNonNull(options, "options 不能为空");
        validate(options);
        Path portDirectory = options.projectRoot().resolve("business")
            .resolve(options.businessDomain()).resolve(options.businessDomain() + "-port");
        Path appDirectory = options.projectRoot().resolve("business")
            .resolve(options.businessDomain()).resolve(options.businessDomain() + "-app");
        Path portPom = portDirectory.resolve("pom.xml");
        if (!Files.isRegularFile(portPom)) {
            throw new IllegalArgumentException("目标 port 模块不存在：" + portPom + "，请先执行 module 命令");
        }
        String moduleBasePackage = "com.kacean.power." + options.subdomain();
        String infrastructurePackage = moduleBasePackage + ".port.infra";
        String webPackage = moduleBasePackage + ".port.web";
        String appPackage = moduleBasePackage + ".app";

        Path portJavaRoot = prepareSourceRoot(portDirectory);
        Path infrastructureRoot = portJavaRoot.resolve(infrastructurePackage.replace('.', '/'));
        Path webRoot = portJavaRoot.resolve(webPackage.replace('.', '/'));
        Path appRoot = null;
        if (options.mode() == GenerationMode.DDD) {
            appRoot = prepareSourceRoot(appDirectory).resolve(appPackage.replace('.', '/'));
            ensureDependency(appDirectory.resolve("pom.xml"),
                "com.kacean.power", "common-app", null, "app 模块");
            ensureDependency(portPom, "com.kacean.power", options.businessDomain() + "-app", null, "port 模块");
        }
        ensurePortDependencies(portPom);

        List<CustomFile> customFiles = createCustomFiles(
            options.mode(), infrastructureRoot, webRoot, appRoot, options.overwrite());
        Map<String, Object> customMap = new LinkedHashMap<>();
        customMap.put("infrastructurePackage", infrastructurePackage);
        customMap.put("webPackage", webPackage);
        customMap.put("appPackage", appPackage);

        DataSourceConfig.Builder dataSource = new DataSourceConfig.Builder(
            options.jdbcUrl(), options.username(), options.password());
        if (!options.schema().isBlank()) {
            dataSource.schema(options.schema());
        }

        Map<OutputFile, String> pathInfo = new EnumMap<>(OutputFile.class);
        pathInfo.put(OutputFile.entity, infrastructureRoot.resolve("entity").toString());
        pathInfo.put(OutputFile.mapper, infrastructureRoot.resolve("mapper").toString());
        pathInfo.put(OutputFile.xml, portDirectory.resolve("src/main/resources/mapper")
            .resolve(options.subdomain()).toString());
        pathInfo.put(OutputFile.parent, infrastructureRoot.toString());

        FastAutoGenerator.create(dataSource)
            .globalConfig(builder -> builder
                .author(BUSINESS_AUTHOR)
                .dateType(DateType.TIME_PACK)
                .commentDate("yyyy-MM-dd")
                .outputDir(portJavaRoot.toString())
                .disableOpenDir())
            .packageConfig(builder -> builder
                .parent(infrastructurePackage)
                .entity("entity")
                .mapper("mapper")
                .xml("mapper")
                .pathInfo(pathInfo))
            .strategyConfig(builder -> {
                builder.addInclude(options.tables())
                    .addTablePrefix(options.tablePrefixes())
                    .enableSkipView();
                builder.serviceBuilder().disable();
                builder.controllerBuilder().disable();
                var strategy = builder.entityBuilder()
                    .javaTemplate("/templates/persistence-entity.java")
                    .superClass("com.kacean.power.common.infra.entity.BaseEntity")
                    .addSuperEntityColumns(SUPER_ENTITY_COLUMNS)
                    .disableSerialVersionUID()
                    .enableRemoveIsPrefix()
                    .enableTableFieldAnnotation()
                    .versionColumnName("version")
                    .versionPropertyName("version")
                    .logicDeleteColumnName("deleted")
                    .logicDeletePropertyName("deleted")
                    .addTableFills(
                        new Column("create_at", FieldFill.INSERT),
                        new Column("create_by", FieldFill.INSERT),
                        new Column("modify_at", FieldFill.INSERT_UPDATE),
                        new Column("modify_by", FieldFill.INSERT_UPDATE),
                        new Property("deleted", FieldFill.INSERT),
                        new Property("version", FieldFill.INSERT)
                    )
                    .idType(IdType.ASSIGN_ID)
                    .formatFileName("%sEntity")
                    .mapperBuilder()
                    .mapperTemplate("/templates/persistence-mapper.java")
                    .mapperXmlTemplate("/templates/persistence-mapper.xml")
                    .superClass("com.kacean.power.common.infra.mapper.BaseMapper")
                    .mapperAnnotation(org.apache.ibatis.annotations.Mapper.class)
                    .enableBaseResultMap()
                    .enableBaseColumnList()
                    .formatMapperFileName("%sMapper")
                    .formatXmlFileName("%sMapper");
                if (options.overwrite()) {
                    builder.entityBuilder().enableFileOverride();
                    strategy.enableFileOverride();
                }
            })
            .injectionConfig(builder -> {
                builder.customMap(customMap);
                builder.beforeOutputFile(BusinessCodeGenerator::enrichCustomModel);
                customFiles.forEach(builder::customFile);
            })
            .templateEngine(new FreemarkerTemplateEngine())
            .execute();

        System.out.printf("业务代码生成完成：mode=%s, port=%s%s%n", options.mode().name().toLowerCase(Locale.ROOT),
            portDirectory.toAbsolutePath().normalize(),
            options.mode() == GenerationMode.DDD
                ? ", app=" + appDirectory.toAbsolutePath().normalize()
                : "");
    }

    /**
     * 创建贫血与 DDD 自定义模板。
     *
     * @param mode 生成模式
     * @param infrastructureRoot infra 包目录
     * @param webRoot web 包目录
     * @param appRoot app 包目录，贫血模式为 null
     * @param overwrite 是否覆盖已有文件
     * @return 自定义文件配置
     */
    private static List<CustomFile> createCustomFiles(
        GenerationMode mode, Path infrastructureRoot, Path webRoot, Path appRoot, boolean overwrite) {
        List<CustomFile> files = new ArrayList<>();
        files.add(customFile(infrastructureRoot.resolve("dao"), "anemic-dao", "Dao.java", overwrite));
        files.add(customFile(infrastructureRoot.resolve("dao"), "anemic-dao-impl", "DaoImpl.java", overwrite));
        files.add(customFile(infrastructureRoot.resolve("curd"), "anemic-curd", "Curd.java", overwrite));
        files.add(customFile(webRoot.resolve("dto"), "anemic-dto", "DTO.java", overwrite));
        files.add(customFile(webRoot.resolve("assembler"),
            mode == GenerationMode.DDD ? "ddd-assembler" : "anemic-assembler", "Assembler.java", overwrite));
        files.add(customFile(webRoot.resolve("controller"),
            mode == GenerationMode.DDD ? "ddd-controller" : "anemic-controller", "Controller.java", overwrite));
        if (mode == GenerationMode.DDD) {
            files.add(customFile(appRoot.resolve("domain"), "ddd-domain", ".java", overwrite));
            files.add(customFile(appRoot.resolve("repo"), "ddd-repository", "Repository.java", overwrite));
            files.add(customFile(infrastructureRoot.resolve("repo"), "ddd-repository-impl", "RepositoryImpl.java", overwrite));
            files.add(customFile(infrastructureRoot.resolve("convert"), "ddd-convert", "Convert.java", overwrite));
        }
        return files;
    }

    /**
     * 创建以表实体名为前缀的自定义文件。
     *
     * @param outputDirectory 输出目录
     * @param templateName 模板名
     * @param fileNameSuffix 文件名后缀
     * @param overwrite 是否覆盖
     * @return 自定义文件
     */
    private static CustomFile customFile(
        Path outputDirectory, String templateName, String fileNameSuffix, boolean overwrite) {
        CustomFile.Builder builder = new CustomFile.Builder()
            .filePath(outputDirectory.toString())
            .templatePath("/templates/" + templateName + ".java.ftl")
            .formatNameFunction(BusinessCodeGenerator::businessName)
            .fileName(fileNameSuffix);
        if (overwrite) {
            builder.enableFileOverride();
        }
        return builder.build();
    }

    /**
     * 补充每种模板共享的模型数据。
     *
     * @param tableInfo 表元数据
     * @param customMap 模板变量
     */
    private static void enrichCustomModel(TableInfo tableInfo, Map<String, Object> customMap) {
        String businessName = businessName(tableInfo);
        customMap.put("businessName", businessName);
        customMap.put("requestPath", requestPath(businessName));
        customMap.put("modelImportPackages", modelImportPackages(tableInfo));
        customMap.put("dtoJsonFormatRequired", dtoJsonFormatRequired(tableInfo));
    }

    /**
     * DTO 中存在 LocalDateTime 字段时需要导入 JsonFormat 注解。
     *
     * @param tableInfo 表元数据
     * @return 是否需要 JsonFormat
     */
    private static boolean dtoJsonFormatRequired(TableInfo tableInfo) {
        return Stream.concat(tableInfo.getCommonFields().stream(), tableInfo.getFields().stream())
            .anyMatch(field -> "LocalDateTime".equals(field.getPropertyType()));
    }

    /**
     * 从 Entity 类名还原业务类名。
     *
     * @param tableInfo 表元数据
     * @return 业务类名
     */
    private static String businessName(TableInfo tableInfo) {
        String entityName = tableInfo.getEntityName();
        return entityName.endsWith("Entity")
            ? entityName.substring(0, entityName.length() - "Entity".length())
            : entityName;
    }

    /**
     * 生成 kebab-case 控制器路径。
     *
     * @param businessName 业务类名
     * @return 请求根路径
     */
    private static String requestPath(String businessName) {
        String path = businessName
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
            .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
            .toLowerCase(Locale.ROOT);
        return "/" + path;
    }

    /**
     * 收集 DTO 与领域模型字段需要显式导入的 Java 类型，包括 BaseEntity 公共字段。
     *
     * @param tableInfo 表元数据
     * @return 排序后的导入包
     */
    private static List<String> modelImportPackages(TableInfo tableInfo) {
        TreeSet<String> packages = new TreeSet<>();
        Stream.concat(tableInfo.getCommonFields().stream(), tableInfo.getFields().stream())
            .map(TableField::getColumnType)
            .filter(Objects::nonNull)
            .forEach(columnType -> {
                if (columnType.getPkg() != null && !columnType.getPkg().isBlank()) {
                    packages.add(columnType.getPkg());
                }
            });
        return List.copyOf(packages);
    }

    /**
     * 校验模块命名、JDBC 地址与表名，防止生成到非预期目录。
     *
     * @param options 生成选项
     */
    private static void validate(Options options) {
        requirePattern(options.businessDomain(), BUSINESS_PATTERN, "业务域名");
        requirePattern(options.subdomain(), SUBDOMAIN_PATTERN, "子域名");
        if (!options.jdbcUrl().startsWith("jdbc:")) {
            throw new IllegalArgumentException("JDBC URL 必须以 jdbc: 开头");
        }
        if (options.tables().isEmpty()) {
            throw new IllegalArgumentException("至少指定一张表");
        }
        options.tables().forEach(table -> requirePattern(table, TABLE_PATTERN, "表名"));
        options.tablePrefixes().forEach(prefix -> requirePattern(prefix, COLUMN_PATTERN, "表前缀"));
    }

    /**
     * 校验正则格式。
     *
     * @param value 输入值
     * @param pattern 允许规则
     * @param label 参数名称
     */
    private static void requirePattern(String value, Pattern pattern, String label) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(label + "格式非法：" + value + "，允许规则 " + pattern.pattern());
        }
    }

    /**
     * 准备源码根目录，并显式放置 .gitkeep。
     *
     * @param moduleDirectory 模块根目录
     * @return src/main/java 路径
     */
    private static Path prepareSourceRoot(Path moduleDirectory) {
        Path javaRoot = moduleDirectory.resolve("src/main/java");
        try {
            Files.createDirectories(javaRoot);
            Path gitKeep = javaRoot.resolve(".gitkeep");
            if (Files.notExists(gitKeep)) {
                Files.createFile(gitKeep);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建源码目录: " + javaRoot, exception);
        }
        return javaRoot;
    }

    /**
     * 校验并补齐 port 模块直接使用的编译依赖。
     *
     * @param pomPath 业务 port 的 pom.xml
     */
    private static void ensurePortDependencies(Path pomPath) {
        ensureDependency(pomPath, "com.kacean.power", "common-infra", null, "port 模块");
        ensureDependency(pomPath, "com.kacean.power", "common-web", null, "port 模块");
    }

    /**
     * 在不改动既有 POM 结构的前提下追加缺失依赖；已有依赖不重复插入。
     *
     * @param pomPath pom.xml 路径
     * @param groupId 依赖 groupId
     * @param artifactId 依赖 artifactId
     * @param scope 依赖 scope，null 表示默认 compile
     * @param moduleRole 报错时显示的模块角色
     */
    private static void ensureDependency(
        Path pomPath, String groupId, String artifactId, String scope, String moduleRole) {
        try {
            String pom = Files.readString(pomPath, StandardCharsets.UTF_8);
            parsePom(pomPath, pom);
            if (Pattern.compile("<artifactId>\\s*" + Pattern.quote(artifactId) + "\\s*</artifactId>")
                .matcher(pom).find()) {
                return;
            }
            String versionXml = "com.kacean.power".equals(groupId)
                ? "\n                <version>${project.version}</version>"
                : "";
            String scopeXml = scope == null ? "" : "\n                <scope>" + scope + "</scope>";
            String dependency = """
                        <dependency>
                            <groupId>%s</groupId>
                            <artifactId>%s</artifactId>%s%s
                        </dependency>
                """.formatted(groupId, artifactId, versionXml, scopeXml);
            int end = pom.indexOf("    </dependencies>");
            if (end < 0) {
                throw new IllegalArgumentException(moduleRole + " pom.xml 缺少顶层 dependencies: " + pomPath);
            }
            String updated = pom.substring(0, end) + dependency + "\n" + pom.substring(end);
            Files.writeString(pomPath, updated, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("无法更新模块依赖: " + pomPath, exception);
        }
    }

    /**
     * 校验目标 POM 是合法 XML。
     *
     * @param pomPath POM 路径
     * @param pom POM 文本
     */
    private static void parsePom(Path pomPath, String pom) {
        try (InputStream input = new java.io.ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8))) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
        } catch (Exception exception) {
            throw new IllegalArgumentException("目标 pom.xml 不是合法 XML: " + pomPath, exception);
        }
    }
}
