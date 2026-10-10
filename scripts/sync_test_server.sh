#!/usr/bin/env bash
# 同步 ICBM-Classic 构建产物到 G: 盘专用测试服务端与客户端
set -euo pipefail
cd "$(dirname "$0")/.."

SERVER_MODS="/mnt/g/ICBM-Test-Server/mods"
CLIENT_MODS="/mnt/g/Minecraft/.minecraft/mods"

JAR=$(ls build/libs/ICBM-classic-26.3-*.jar 2>/dev/null | grep -v '\-sources\.jar' | head -1 || true)
if [ -z "$JAR" ]; then
    echo "未找到构建产物，正在构建..."
    ./gradlew build
    JAR=$(ls build/libs/ICBM-classic-26.3-*.jar 2>/dev/null | grep -v '\-sources\.jar' | head -1)
fi

echo "同步模组包: $JAR"
if [ -d "$SERVER_MODS" ]; then
    cp "$JAR" "$SERVER_MODS/"
    echo "已同步至服务端: $SERVER_MODS/"
else
    echo "服务端目录不存在: $SERVER_MODS"
fi

if [ -d "$CLIENT_MODS" ]; then
    cp "$JAR" "$CLIENT_MODS/"
    echo "已同步至客户端: $CLIENT_MODS/"
else
    echo "客户端目录不存在: $CLIENT_MODS"
fi
