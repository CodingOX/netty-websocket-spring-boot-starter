#!/usr/bin/env bash

# 简单发布脚本：构建并通过 Central Portal 发布到 Maven Central
# 前置条件：
# 1. pom.xml 中版本号已修改为要发布的版本
# 2. ~/.m2/settings.xml 中已配置：
#    - <server id="central">（Central Portal User Token）
#    - gpg profile（gpg.executable / gpg.passphrase）
# 3. 当前工作区 git 处于干净状态（可选）

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${PROJECT_ROOT}"

PROFILE="release"

echo "🎯 项目根目录：${PROJECT_ROOT}"
echo "🎯 使用 Maven Profile：${PROFILE}"

# 获取当前 POM 版本
VERSION="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)"
echo "📦 当前 POM 版本：${VERSION}"

read -r -p "确认发布版本 ${VERSION} 到 Maven Central？(y/N) " ANSWER
case "${ANSWER}" in
  y|Y|yes|YES)
    echo "✅ 开始发布流程..."
    ;;
  *)
    echo "❌ 已取消发布。"
    exit 1
    ;;
esac

echo "🚧 步骤 1/2：clean + package（包含源码 / Javadoc / GPG 签名）"
mvn clean package -P "${PROFILE}"

echo "🚧 步骤 2/2：通过 central-publishing-maven-plugin 上传并发布"
mvn central-publishing:publish -P "${PROFILE}"

echo "✅ 发布命令执行完成。"
echo "👉 请在浏览器中打开 https://central.sonatype.com/publishing/deployments 查看本次 Deployment 状态。"

