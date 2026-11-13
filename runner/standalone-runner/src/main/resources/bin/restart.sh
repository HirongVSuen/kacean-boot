#!/bin/bash
# 单体应用重启脚本：先停止再启动
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
sh "$APP_HOME/bin/stop.sh"
sh "$APP_HOME/bin/startup.sh"
