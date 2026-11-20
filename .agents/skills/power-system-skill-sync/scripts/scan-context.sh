#!/usr/bin/env bash
# 只读扫描基础架构、Skill 路由与文档引用，供 power-system-business / power-system-skill-sync 使用。
set -euo pipefail

VERBOSE=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --verbose|-v) VERBOSE=1 ;;
    *) echo "未知参数：$1（仅支持 --verbose）" >&2; exit 2 ;;
  esac
  shift
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
cd "$PROJECT_ROOT"

section() {
  printf '\n===== %s =====\n' "$1"
}

main_java_files() {
  local root="$1"
  [[ -d "$root" ]] || return 0
  find "$root" -type d -name target -prune -o -type f -path '*/src/main/java/*' -name '*.java' -print | sort
}

file_count() {
  local files="$1"
  if [[ -z "$files" ]]; then
    echo 0
  else
    printf '%s\n' "$files" | wc -l | tr -d ' '
  fi
}

section "1. 未提交变更"
if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  GIT_STATUS="$(git status --short)"
  echo "[GIT] 未提交路径 $(file_count "$GIT_STATUS") 个。"
  if [[ "$VERBOSE" == "1" && -n "$GIT_STATUS" ]]; then
    printf '%s\n' "$GIT_STATUS"
  fi
else
  echo "[GIT] 当前目录不是 Git 仓库，跳过。"
fi

section "2. 通用组件主源码"
for module in common/common-web common/common-app common/common-infra common/common-util share; do
  files="$(main_java_files "$module")"
  echo "[COMMON] ${module}: $(file_count "$files") 个主源码文件。"
  if [[ "$VERBOSE" == "1" && -n "$files" ]]; then
    printf '%s\n' "$files"
  fi
done

section "3. ArchUnit 全局规则"
ARCH_RULE_FILE="$(find common/common-test -type d -name target -prune -o -type f -name 'GlobalArchitectureTest.java' -print -quit 2>/dev/null || true)"
if [[ -n "$ARCH_RULE_FILE" ]]; then
  RULE_COUNT="$(grep -cE '^[[:space:]]*@ArchTest[[:space:]]*$' "$ARCH_RULE_FILE" || true)"
  echo "[ARCH] $ARCH_RULE_FILE"
  echo "[ARCH] @ArchTest 规则数：$RULE_COUNT（构建验证时报告条数应与此一致）。"
else
  echo "[ARCH] 未找到 GlobalArchitectureTest.java，请人工核对 common-test 结构。"
fi

section "4. 业务域主源码"
if [[ -d business ]]; then
  for domain_dir in business/*/; do
    [[ -d "$domain_dir" ]] || continue
    files="$(main_java_files "$domain_dir")"
    count="$(file_count "$files")"
    echo "[BUSINESS] ${domain_dir%/}: ${count} 个主源码文件。"
    if [[ "$VERBOSE" == "1" && "$count" != "0" ]]; then
      printf '%s\n' "$files"
    fi
  done
else
  echo "[BUSINESS] business 目录不存在。"
fi

section "5. Skill 引用、路由与链接校验"
MISSING_CLASSES=0
UNREGISTERED_SKILLS=0
BROKEN_ROUTES=0
BROKEN_LINKS=0
if [[ -d .agents/skills ]]; then
  if [[ -f AGENTS.md ]]; then
    ROUTE_NAMES="$(grep -oE 'power-system-[a-z0-9-]+' AGENTS.md | sort -u)"
    while IFS= read -r route_name; do
      [[ -n "$route_name" ]] || continue
      if [[ ! -f ".agents/skills/$route_name/SKILL.md" ]]; then
        echo "[失效路由] $route_name"
        BROKEN_ROUTES=$((BROKEN_ROUTES + 1))
      fi
    done <<< "$ROUTE_NAMES"

    while IFS= read -r skill_file; do
      skill_name="$(basename "$(dirname "$skill_file")")"
      if ! grep -Fxq "$skill_name" <<< "$ROUTE_NAMES"; then
        echo "[未注册 Skill] $skill_name"
        UNREGISTERED_SKILLS=$((UNREGISTERED_SKILLS + 1))
      fi
    done < <(find .agents/skills -mindepth 2 -maxdepth 2 -type f -name 'SKILL.md' -print | sort)
  else
    echo "[失效路由] 根 AGENTS.md 不存在。"
    BROKEN_ROUTES=$((BROKEN_ROUTES + 1))
  fi

  while IFS= read -r fqn; do
    [[ -n "$fqn" ]] || continue
    case "$fqn" in
      com.kacean.power.*) ;;
      common.app.*|common.infra.*|common.web.*|common.util.*)
        fqn="com.kacean.power.${fqn}" ;;
      infra.*|web.*|util.*)
        fqn="com.kacean.power.common.${fqn}" ;;
      *) continue ;;
    esac
    rel="${fqn//./\/}.java"
    if ! find . -type d -name target -prune -o -type f -path "*/src/main/java/$rel" -print -quit | grep -q .; then
      echo "[缺失类] $fqn"
      MISSING_CLASSES=$((MISSING_CLASSES + 1))
    fi
  done < <(grep -rhoE '(com\.kacean\.power\.[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)*|`(common\.(app|infra|web|util)|infra|web|util)(\.[A-Za-z0-9_]+)+`)' .agents/skills --include='*.md' \
    | tr -d '`' | grep -E '\.[A-Z][A-Za-z0-9_]*$' | sort -u)
fi

while IFS= read -r markdown_file; do
  [[ -n "$markdown_file" ]] || continue
  while IFS= read -r link; do
    [[ -n "$link" ]] || continue
    if [[ "$link" == \<*\> ]]; then
      link="${link:1:${#link}-2}"
    else
      link="${link%%[[:space:]]*}"
    fi
    [[ "$link" == \#* ]] && continue
    [[ "$link" =~ ^[A-Za-z][A-Za-z0-9+.-]*: ]] && continue
    target="${link%%#*}"
    [[ -n "$target" ]] || continue
    if [[ "$target" != /* ]]; then
      target="$(dirname "$markdown_file")/$target"
    fi
    if [[ ! -e "$target" ]]; then
      echo "[失效链接] $markdown_file -> $link"
      BROKEN_LINKS=$((BROKEN_LINKS + 1))
    fi
  done < <(grep -oE '\[[^]]+\]\([^)]+\)' "$markdown_file" | sed -E 's/^.*\]\(//; s/\)$//' || true)
done < <({ find .agents/skills -type f -name '*.md' -print 2>/dev/null | sort; if [[ -f AGENTS.md ]]; then echo AGENTS.md; fi; })

ISSUE_TOTAL=$((MISSING_CLASSES + UNREGISTERED_SKILLS + BROKEN_ROUTES + BROKEN_LINKS))
echo "[CHECK] 缺失类=$MISSING_CLASSES；未注册 Skill=$UNREGISTERED_SKILLS；失效路由=$BROKEN_ROUTES；失效链接=$BROKEN_LINKS。"
if [[ "$ISSUE_TOTAL" == "0" ]]; then
  echo "[CHECK] 引用、路由与链接全部通过。"
else
  echo "[CHECK] 请按 power-system-skill-sync 处理文档漂移；代码缺陷只报告，不擅自修改。"
fi

section "6. 下一步"
DEPLOY_NAMES=""
DEPLOY_TARGETS=""
if [[ -d business/deploy ]]; then
  for deploy_dir in business/deploy/*-deploy; do
    [[ -d "$deploy_dir" ]] || continue
    deploy_name="$(basename "$deploy_dir")"
    DEPLOY_NAMES="${DEPLOY_NAMES}${DEPLOY_NAMES:+、}${deploy_name}"
    DEPLOY_TARGETS="${DEPLOY_TARGETS}${DEPLOY_TARGETS:+,}${deploy_dir}"
  done
fi
if [[ -n "$DEPLOY_TARGETS" ]]; then
  echo "[TARGET] 部署模块：$DEPLOY_NAMES。"
  echo "[VERIFY] mvn test -pl $DEPLOY_TARGETS -am test"
else
  echo "[TARGET] 未发现 business/deploy/*-deploy，请人工核对模块结构。"
fi
echo "[LOAD] 业务任务加载 power-system-business；按任务补充 app / share / naming 等协作 Skill。"
echo "[SYNC] 路由、链接或公共事实漂移时加载 power-system-skill-sync。"
echo "[DETAIL] 使用 --verbose 查看未提交路径与全部主源码清单。"
