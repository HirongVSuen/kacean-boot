#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
MODULE_DIR="$PROJECT_ROOT/common/common-codegen"
CLASSPATH_FILE="$MODULE_DIR/target/codegen-classpath.txt"

if command -v mvn >/dev/null 2>&1; then
  MVN=(mvn)
elif [[ -x "$PROJECT_ROOT/mvnw" ]]; then
  MVN=("$PROJECT_ROOT/mvnw")
else
  echo "未找到 mvn 或 mvnw，无法编译 common-codegen。" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "未找到 java，请使用项目要求的 JDK 21。" >&2
  exit 1
fi

"${MVN[@]}" -q -f "$MODULE_DIR/pom.xml" compile \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -Dmdep.outputFile="$CLASSPATH_FILE" -DincludeScope=runtime

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) PATH_SEPARATOR=';' ;;
  *) PATH_SEPARATOR=':' ;;
esac

DEPENDENCIES="$(tr -d '\r\n' < "$CLASSPATH_FILE")"
cd "$PROJECT_ROOT"
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
  -cp "common/common-codegen/target/classes${PATH_SEPARATOR}${DEPENDENCIES}" \
  com.kacean.boot.codegen.CodeGeneratorApplication "$@"
