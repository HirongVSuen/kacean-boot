package com.kacean.power.codegen;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MyBatis-Plus 代码生成命令行入口：支持交互式操作，也支持脚本化创建业务模块和生成持久化代码。
 *
 * @author kacean-agent
 */
public final class CodeGeneratorApplication {

    private static final String DEFAULT_PASSWORD_ENV = "CODEGEN_DB_PASSWORD";
    private static final Set<String> COMMON_OPTIONS = Set.of("project-root");
    private static final Set<String> MODULE_OPTIONS = Set.of("business", "subdomain");
    private static final Set<String> GENERATE_OPTIONS = Set.of(
            "business", "subdomain", "jdbc-url", "username", "password-env", "schema", "tables",
            "table-prefix", "overwrite");

    private final BufferedReader input;
    private final PrintStream output;
    private final Map<String, String> environment;

    private CodeGeneratorApplication(BufferedReader input, PrintStream output, Map<String, String> environment) {
        this.input = input;
        this.output = output;
        this.environment = environment;
    }

    public static void main(String[] args) throws Exception {
        CodeGeneratorApplication application = new CodeGeneratorApplication(
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), System.out, System.getenv());
        try {
            application.execute(args);
        } catch (IllegalArgumentException exception) {
            System.err.println("参数错误：" + exception.getMessage());
            System.err.println("使用 help 查看命令说明。");
            System.exit(2);
        }
    }

    private void execute(String[] args) throws Exception {
        if (args.length == 0) {
            runInteractive();
            return;
        }
        if (args.length == 1 && (args[0].equals("help") || args[0].equals("--help") || args[0].equals("-h"))) {
            printHelp();
            return;
        }

        String command = args[0];
        Arguments arguments = Arguments.parse(Arrays.copyOfRange(args, 1, args.length));
        Path projectRoot = resolveProjectRoot(arguments);
        switch (command) {
            case "module" -> {
                arguments.ensureAllowed(MODULE_OPTIONS, COMMON_OPTIONS);
                createModule(projectRoot, arguments);
            }
            case "generate" -> {
                arguments.ensureAllowed(GENERATE_OPTIONS, COMMON_OPTIONS);
                generate(projectRoot, arguments);
            }
            default -> throw new IllegalArgumentException("未知命令 " + command + "，仅支持 module、generate、help");
        }
    }

    private void runInteractive() throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        output.println("MyBatis-Plus 代码生成器");
        output.println("1. 创建业务模块（app / port / deploy）");
        output.println("2. 从数据库表生成 PO / Mapper / Mapper XML");
        output.println("0. 退出");
        String choice = prompt("请选择操作", "2");
        switch (choice) {
            case "1" -> {
                String business = prompt("业务域名，对应 business 目录（如 ticket）", "");
                String subdomain = prompt("子域名，对应 Java 包（如 tickettype）", "");
                requireText(business, "business");
                requireText(subdomain, "subdomain");
                output.printf("将在 %s 下创建 business/%s 与 business/deploy/%s-deploy。%n",
                        projectRoot, business, business);
                if (confirm("确认创建", true)) {
                    BusinessModuleScaffolder.scaffold(projectRoot, business, subdomain);
                    printModuleSuccess(business, subdomain);
                }
            }
            case "2" -> {
                String business = prompt("业务域名（如 ticket）", "");
                String subdomain = prompt("子域名（如 tickettype）", "");
                String jdbcUrl = prompt("JDBC URL", "jdbc:mysql://localhost:3306/power_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai");
                String username = prompt("数据库用户名", "");
                String password = readPassword(DEFAULT_PASSWORD_ENV);
                String schema = prompt("数据库 schema（不需要时留空）", "");
                String tables = prompt("表名，多个表使用英文逗号分隔", "");
                String tablePrefix = prompt("表前缀，多个前缀使用英文逗号分隔（可留空）", "");
                boolean overwrite = confirm("覆盖已存在的同名文件", false);
                output.printf("目标模块：business/%s/%s-port，子域包：com.kacean.power.%s。%n",
                        business, business, subdomain);
                output.printf("数据表：%s；覆盖已有文件：%s。%n", tables, overwrite ? "是" : "否");
                if (confirm("确认生成", true)) {
                    PersistenceCodeGenerator.generate(new PersistenceCodeGenerator.Options(
                            projectRoot, business, subdomain, jdbcUrl, username, password, schema,
                            splitList(tables), splitList(tablePrefix), overwrite));
                    printGenerateSuccess(business, subdomain);
                }
            }
            case "0" -> output.println("已退出。");
            default -> throw new IllegalArgumentException("未知菜单项 " + choice);
        }
    }

    private void createModule(Path projectRoot, Arguments arguments) throws IOException {
        String business = arguments.required("business");
        String subdomain = arguments.required("subdomain");
        BusinessModuleScaffolder.scaffold(projectRoot, business, subdomain);
        printModuleSuccess(business, subdomain);
    }

    private void generate(Path projectRoot, Arguments arguments) {
        String passwordEnvironment = arguments.value("password-env", DEFAULT_PASSWORD_ENV);
        String password = environment.getOrDefault(passwordEnvironment, "");
        PersistenceCodeGenerator.generate(new PersistenceCodeGenerator.Options(
                projectRoot,
                arguments.required("business"),
                arguments.required("subdomain"),
                arguments.required("jdbc-url"),
                arguments.value("username", ""),
                password,
                arguments.value("schema", ""),
                splitList(arguments.required("tables")),
                splitList(arguments.value("table-prefix", "")),
                arguments.flag("overwrite")));
        printGenerateSuccess(arguments.required("business"), arguments.required("subdomain"));
    }

    private Path resolveProjectRoot(Arguments arguments) {
        Path projectRoot = Path.of(arguments.value("project-root", "")).toAbsolutePath().normalize();
        if (!Files.isRegularFile(projectRoot.resolve("pom.xml"))) {
            throw new IllegalArgumentException("项目根目录不存在 pom.xml：" + projectRoot);
        }
        return projectRoot;
    }

    private String readPassword(String environmentName) throws IOException {
        String environmentPassword = environment.get(environmentName);
        if (environmentPassword != null) {
            output.println("已从环境变量 " + environmentName + " 读取数据库密码。");
            return environmentPassword;
        }
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword("数据库密码（输入不可见）：");
            return password == null ? "" : new String(password);
        }
        output.println("当前终端不支持隐藏输入，也可先设置环境变量 " + environmentName + "。");
        return prompt("数据库密码", "");
    }

    private String prompt(String label, String defaultValue) throws IOException {
        output.print(defaultValue.isEmpty() ? label + "：" : label + " [" + defaultValue + "]：");
        output.flush();
        String value = input.readLine();
        if (value == null) {
            throw new IllegalArgumentException("标准输入已结束，无法继续交互");
        }
        value = value.trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private boolean confirm(String label, boolean defaultValue) throws IOException {
        String hint = defaultValue ? "Y/n" : "y/N";
        String value = prompt(label + "（" + hint + "）", defaultValue ? "y" : "n");
        return value.equalsIgnoreCase("y") || value.equalsIgnoreCase("yes");
    }

    private void printModuleSuccess(String business, String subdomain) {
        output.println("业务模块创建完成：" + business);
        output.println("下一步：在 docs/domain/glossary.md 登记子域术语，然后执行 generate 生成 " + subdomain + " 的持久化代码。");
    }

    private void printGenerateSuccess(String business, String subdomain) {
        output.println("代码生成完成。");
        output.printf("PO：business/%s/%s-port/src/main/java/com/kacean/power/%s/port/infra/persistence/po%n",
                business, business, subdomain);
        output.printf("Mapper：business/%s/%s-port/src/main/java/com/kacean/power/%s/port/infra/persistence/mapper%n",
                business, business, subdomain);
        output.printf("XML：business/%s/%s-port/src/main/resources/mapper/%s%n", business, business, subdomain);
        output.println("请审查字段映射，并按需补充纯 Java 领域对象、仓储适配与 Web 代码。");
    }

    private void printHelp() {
        output.println("用法：");
        output.println("  无参数：进入交互式菜单");
        output.println("  module --business <业务域> --subdomain <子域>");
        output.println("  generate --business <业务域> --subdomain <子域> --jdbc-url <URL> --tables <表1,表2> [选项]");
        output.println("generate 选项：");
        output.println("  --username <用户>                 数据库用户名，默认空");
        output.println("  --password-env <环境变量名>        密码环境变量，默认 CODEGEN_DB_PASSWORD");
        output.println("  --schema <schema>                数据库 schema");
        output.println("  --table-prefix <前缀1,前缀2>       生成类名时移除表前缀");
        output.println("  --overwrite                       覆盖同名文件；默认保留已有文件");
        output.println("  --project-root <目录>             项目根目录，默认当前目录");
        output.println("持久化约定：PO 继承 common-infra BaseEntity，主键固定为 Long + ASSIGN_ID。");
        output.println("安全约束：不支持通过命令行参数传递数据库密码。");
    }

    private static List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    /** 解析 --key value、--key=value 与布尔开关，不接受位置参数。 */
    private static final class Arguments {
        private final Map<String, String> values = new HashMap<>();
        private final Set<String> flags = new HashSet<>();

        private static Arguments parse(String[] args) {
            Arguments arguments = new Arguments();
            for (int index = 0; index < args.length; index++) {
                String argument = args[index];
                if (!argument.startsWith("--")) {
                    throw new IllegalArgumentException("不支持位置参数 " + argument);
                }
                String option = argument.substring(2);
                if (option.equals("overwrite")) {
                    arguments.flags.add(option);
                    continue;
                }
                int equalsIndex = option.indexOf('=');
                if (equalsIndex > 0) {
                    arguments.values.put(option.substring(0, equalsIndex), option.substring(equalsIndex + 1));
                    continue;
                }
                if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                    throw new IllegalArgumentException("选项 --" + option + " 缺少值");
                }
                arguments.values.put(option, args[++index]);
            }
            return arguments;
        }

        private String required(String name) {
            String value = values.get(name);
            requireText(value, "--" + name);
            return value.trim();
        }

        private String value(String name, String defaultValue) {
            return values.getOrDefault(name, defaultValue);
        }

        private boolean flag(String name) {
            return flags.contains(name);
        }

        private void ensureAllowed(Set<String> commandOptions, Set<String> commonOptions) {
            Set<String> unknown = new HashSet<>(values.keySet());
            unknown.addAll(flags);
            unknown.removeAll(commandOptions);
            unknown.removeAll(commonOptions);
            if (!unknown.isEmpty()) {
                throw new IllegalArgumentException("未知选项：" + String.join("、", unknown));
            }
        }
    }
}
