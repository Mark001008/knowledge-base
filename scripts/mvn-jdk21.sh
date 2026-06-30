#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="${PROJECT_JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

# 代理配置（从环境变量读取）
if [ -n "${HTTPS_PROXY:-}" ]; then
    # 解析 proxy URL，格式：http://host:port
    PROXY_HOST=$(echo "$HTTPS_PROXY" | sed -E 's|^https?://||' | cut -d: -f1)
    PROXY_PORT=$(echo "$HTTPS_PROXY" | sed -E 's|^https?://||' | cut -d: -f2)

    # 可通过 NON_PROXY_HOSTS 额外指定不走代理的地址
    NON_PROXY_HOSTS="${NON_PROXY_HOSTS:-localhost|127.0.0.1}"

    export MAVEN_OPTS="${MAVEN_OPTS:-} -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT -Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttp.nonProxyHosts=$NON_PROXY_HOSTS -Dhttps.nonProxyHosts=$NON_PROXY_HOSTS"
fi

exec mvn "$@"
