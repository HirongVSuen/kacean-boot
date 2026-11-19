#!/usr/bin/env bash
# 扫描基础架构与通用组件事实，供 power-system-business skill 在业务开发前使用。
# 只读扫描，不修改任何文件；在 Git Bash / Linux / macOS 下执行。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
cd "$PROJECT_ROOT"

section() {
  printf '\n===== %s =====\n' "$1"
}

section "1. 未提交变更（识别进行中的重构）"
if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git status --short || true
else
  echo "当前目录不是 Git 仓库，跳过。"
fi

section "2. 通用组件公共类清单（common-web / common-app / common-infra / common-util / share）"
for module in common/common-web common/common-app common/common-infra common/common-util share; do
  if [[ -d "$module" ]]; then
    find "$module" -type d -name target -prune -o -type f -path '*/src/main/java/*' -name '*.java' -print | sort
  fi
done

section "3. ArchUnit 全局规则"
ARCH_RULE_FILE="$(find common/common-test -type f -name 'GlobalArchitectureTest.java' -print -quit 2>/dev/null || true)"
if [[ -n "$ARCH_RULE_FILE" ]]; then
  RULE_COUNT="$(grep -cE '^[[:space:]]*@ArchTest[[:space:]]*$' "$ARCH_RULE_FILE" || true)"
  echo "规则文件：$ARCH_RULE_FILE"
  echo "@ArchTest 规则数：$RULE_COUNT（构建验证时报告条数应与此一致）"
else
  echo "未找到 GlobalArchitectureTest.java，请人工核对 common-test 结构。"
fi

section "4. 业务域代码分布（business/*/）"
if [[ -d business ]]; then
  for domain_dir in business/*/; do
    [[ -d "$domain_dir" ]] || continue
    count="$(find "$domain_dir" -type d -name target -prune -o -type f -path '*/src/main/java/*' -name '*.java' -print | wc -l | tr -d ' ')"
    echo "${domain_dir%/}：${count} 个主源码文件"
    if [[ "$count" != "0" ]]; then
      find "$domain_dir" -type d -name target -prune -o -type f -path '*/src/main/java/*' -name '*.java' -print | sort
    fi
  done
else
  echo "business 目录不存在。"
fi

section "5. skill 引用类存在性校验（全限定名与 common 包简写，防虚构）"
MISSING=0
if [[ -d .agents/skills ]]; then
  while IFS= read -r fqn; do
    [[ -n "$fqn" ]] || continue
    # 将 skill 中常用的 common 包简写归一化为真实源码包名。
    case "$fqn" in
      com.kacean.power.*) ;;
      common.app.*|common.infra.*|common.web.*|common.util.*)
        fqn="com.kacean.power.${fqn}" ;;
      app.*|infra.*|web.*|util.*)
        fqn="com.kacean.power.common.${fqn}" ;;
      *) continue ;;
    esac
    rel="${fqn//./\/}.java"
    if ! find . -type d -name target -prune -o -type f -path "*/src/main/java/$rel" -print -quit | grep -q .; then
      echo "缺失：$fqn"
      MISSING=$((MISSING + 1))
    fi
  done < <(grep -rhoE '(com\.kacean\.power\.[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)*|`(common\.)?(app|infra|web|util)(\.[A-Za-z0-9_]+)+`)' .agents/skills --include='*.md' \
    | tr -d '`' | grep -E '\.[A-Z][A-Za-z0-9_]*$' | sort -u)
fi
if [[ "$MISSING" == "0" ]]; then
  echo "全部存在。"
else
  echo "共 $MISSING 个引用类缺失：若是文档漂移请同步对应 skill，若是代码缺陷请报告用户。"
fi

section "扫描完成"
echo "请结合调用方 skill（power-system-business / power-system-skill-sync）补读原始事实文件后再设计或同步。"
