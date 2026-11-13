#!/bin/bash
# 日志归档脚本：当月（30 天内）日志原样留存，过月日志压缩到 .logs/archive 长期留存
# 启动脚本会在启动前自动执行一次；也可配置 cron 每月执行，例如：
#   0 2 1 * * sh /path/to/bin/archive-logs.sh
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
LOG_DIR="$APP_HOME/.logs"
ARCHIVE_DIR="$LOG_DIR/archive"
RETENTION_DAYS=30

[ -d "$LOG_DIR" ] || exit 0
mkdir -p "$ARCHIVE_DIR"

# 压缩 30 天前的历史日志（不含当前写入中的 power-system.log），压缩后删除原文件
find "$LOG_DIR" -maxdepth 1 -type f -name "power-system.*.log" -mtime +$RETENTION_DAYS | while read -r file; do
    base=$(basename "$file")
    if gzip -c "$file" > "$ARCHIVE_DIR/$base.gz"; then
        rm -f "$file"
        echo "已归档：$base -> archive/$base.gz"
    fi
done
