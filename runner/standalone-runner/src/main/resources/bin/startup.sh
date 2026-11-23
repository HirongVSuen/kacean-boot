#!/bin/bash
# 单体应用启动脚本（Linux / macOS / Git Bash）
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
APP_NAME=standalone-runner
JAR_FILE="$APP_HOME/lib/$APP_NAME.jar"
PID_FILE="$APP_HOME/run/$APP_NAME.pid"
LOG_DIR="$APP_HOME/.logs"

# 1. 检查 lib 目录下是否包含可执行 jar
if [ ! -f "$JAR_FILE" ]; then
    echo "[错误] 未找到可执行 jar：$JAR_FILE"
    echo "       请检查分发包是否完整（lib 目录必须包含 $APP_NAME.jar）。"
    exit 1
fi

# 2. 检查是否已在运行，避免重复启动
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
        echo "应用已在运行，PID：$PID，请勿重复启动。"
        exit 0
    fi
fi

# 3. thin jar + lib 外置依赖：通过 classpath 通配符加载全部依赖与主类
MAIN_CLASS=com.kacean.boot.KaceanBootApplication
CLASS_PATH="$APP_HOME/lib/*"
CONFIG_LOCATION="file:$APP_HOME/conf/"
# 外置日志配置：命令行指定，优先于 application.yml 中的 classpath 默认值
LOGGING_CONFIG="file:$APP_HOME/conf/logback-spring.xml"
# Git Bash / Cygwin 环境下，将路径转换为 Windows 格式供原生 java 使用
if command -v cygpath >/dev/null 2>&1; then
    CLASS_PATH="$(cygpath -m "$APP_HOME")/lib/*"
    CONFIG_LOCATION="file:$(cygpath -m "$APP_HOME")/conf/"
    LOGGING_CONFIG="file:$(cygpath -m "$APP_HOME")/conf/logback-spring.xml"
fi

# 4. 后台启动并记录 PID（工作目录固定为 APP_HOME：外置 web/、.logs 相对路径及归档脚本均依赖它）
mkdir -p "$LOG_DIR" "$APP_HOME/run"
cd "$APP_HOME"
sh "$APP_HOME/bin/archive-logs.sh"
nohup java -cp "$CLASS_PATH" "$MAIN_CLASS" \
    --spring.config.additional-location="$CONFIG_LOCATION" \
    --logging.config="$LOGGING_CONFIG" \
    >> "$LOG_DIR/$APP_NAME.log" 2>&1 &
echo $! > "$PID_FILE"
echo "启动成功，PID：$(cat "$PID_FILE")"
echo "日志文件：$LOG_DIR/$APP_NAME.log"
