#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk

echo "== forbidden 1.12 names in src/"
fail=0
while IFS= read -r pattern; do
  [ -z "$pattern" ] && continue
  if grep -rEn --include='*.java' "$pattern" src/ ; then
    echo "!! forbidden pattern: $pattern"; fail=1
  fi
done < scripts/forbidden_names.txt

echo "== client classes in main source set"
if grep -rEn --include='*.java' '^import (net\.minecraft\.client|com\.mojang\.blaze3d|net\.fabricmc\.fabric\.api\.client)' src/main/java ; then
  echo "!! client import in src/main"; fail=1
fi

if [ -f scripts/check_lang.py ]; then
  echo "== lang files"
  python3 -I scripts/check_lang.py || fail=1
fi

echo "== gradle build (compile + JUnit + GameTest)"
./gradlew build --console=plain || fail=1

exit $fail
