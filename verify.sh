#!/usr/bin/env bash
# Runs Gradle with an auto-detected JDK (sandbox environment has no system Java).
set -euo pipefail
cd "$(dirname "$0")"

if [ -z "${JAVA_HOME:-}" ]; then
  for candidate in \
    /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
    /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
    "$HOME/.jdks"/*/Contents/Home \
    /Library/Java/JavaVirtualMachines/*/Contents/Home \
    "/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"; do
    if [ -x "$candidate/bin/java" ]; then export JAVA_HOME="$candidate"; break; fi
  done
fi
if [ -z "${JAVA_HOME:-}" ] && command -v /usr/libexec/java_home >/dev/null; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)"
fi
echo "Using JAVA_HOME=$JAVA_HOME"
"$JAVA_HOME/bin/java" -version 1>&2

TASK="${1:-test}"
shift || true
case "$TASK" in
  test) ./gradlew test "$@" ;;
  build) ./gradlew buildPlugin "$@" ;;
  verify) ./gradlew verifyPlugin "$@" ;;
  all) ./gradlew test verifyPlugin buildPlugin "$@" ;;
  *) ./gradlew "$TASK" "$@" ;;
esac
