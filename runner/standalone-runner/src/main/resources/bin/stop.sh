#!/bin/bash
# 单体应用停止脚本（Linux / macOS / Git Bash）
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
APP_NAME=standalone-runner
PID_FILE="$APP_HOME/run/$APP_NAME.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "未找到 PID 文件：$PID_FILE，应用未运行或非脚本启动。"
    exit 0
fi

PID=$(cat "$PID_FILE")
if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    kill "$PID"
    echo "已发送停止信号，PID：$PID，等待进程退出（最长 30 秒）..."
    for i in $(seq 1 30); do
        kill -0 "$PID" 2>/dev/null || break
        sleep 1
    done
    if kill -0 "$PID" 2>/dev/null; then
        echo "等待超时，强制结束进程。"
        kill -9 "$PID"
    fi
else
    echo "进程已不存在，PID：$PID"
fi

rm -f "$PID_FILE"
echo "应用已停止。"
