#!/bin/bash
# power-system 全量构建 + 分发包冒烟验证 + 自动清理
# 用法：sh .agents/skills/power-system-build/scripts/verify.sh [端口，默认 8080]
# 失败时自动停止已启动应用并清理临时目录
set -euo pipefail

PORT=${1:-8080}
PROJECT_ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
DIST_DIR="$PROJECT_ROOT/runner/standalone-runner/target/dist-verify"
APP_DIR="$DIST_DIR/standalone-runner-1.0-SNAPSHOT"
STARTED=0

cleanup() {
    if [ "$STARTED" = "1" ] && [ -f "$APP_DIR/bin/stop.sh" ]; then
        sh "$APP_DIR/bin/stop.sh" || true
    fi
    rm -rf "$DIST_DIR"
}
trap cleanup EXIT

cd "$PROJECT_ROOT"
echo "==> 1/4 全量构建"
mvn clean package -q

echo "==> 2/4 解压分发包"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"
cd "$DIST_DIR"
unzip -q ../standalone-runner-1.0-SNAPSHOT.zip

echo "==> 3/4 启动并断言"
SERVER_PORT="$PORT" sh "$APP_DIR/bin/startup.sh"
STARTED=1
sleep 12
grep -q "Started PowerSystemApplication" "$APP_DIR/.logs/power-system.log"
code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/")
[ "$code" = "200" ] || { echo "[失败] / 返回 $code"; exit 1; }
code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/index.html")
[ "$code" = "200" ] || { echo "[失败] /index.html 返回 $code"; exit 1; }
echo "断言通过：启动日志正常，/ 与 /index.html 均 200，.logs/power-system.log 已生成"

echo "==> 4/4 停止并清理"
sh "$APP_DIR/bin/stop.sh"
STARTED=0
echo "验证通过"
