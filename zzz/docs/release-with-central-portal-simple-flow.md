Maven Central 发布操作手册（配套 release.sh）
======================================

🎯 目标

这篇文档只解决一个问题：

> “我每次发版，要做什么？要敲哪些命令？”

并配套一个可直接执行的脚本：`scripts/release.sh`。

---

✨ 一、发版前需要人工确认的事项
-------------------------

每次发布新版本前，建议先手工完成：

1. **确认版本号**
   - 修改 `pom.xml` 中的：
     - `<version>0.13.0-codingox-1</version>` → 换成你要发布的新版本，例如：`0.13.1` 或 `0.13.1-codingox-1`

2. **更新文档 / 变更记录（可选但推荐）**
   - 在 `README_zh.md` 或单独的 CHANGELOG 中写明本次变更内容

3. **确保本地代码处于干净状态**
   - 建议检查：
     ```bash
     git status
     ```
   - 避免在有未提交改动的情况下执行发布脚本

4. **确保公共配置已就绪（只需配置一次）**
   - `~/.m2/settings.xml` 中已经配置：
     - `<server id="central">` 使用 Central Portal 中生成的 User Token
     - `gpg` profile，且能正常执行 GPG 签名
   - 详情可参考文档：`zzz/docs/maven-central-central-portal-publish-guide.md`

---

🛠 二、一次完整发版需要的 Maven 命令
------------------------------

如果你不想用脚本，可以手工执行两条命令完成一次发布（前提是按上面要求配置好了环境）：

1. 构建 + 签名（源码 / Javadoc / GPG）

```bash
mvn clean package -P release
```

2. 通过 Central Portal 插件发布到 Maven Central

```bash
mvn central-publishing:publish -P release
```

执行完成后，在浏览器中访问：

- `https://central.sonatype.com/publishing/deployments`

找到本次 Deployment，确认状态为 `Published`，稍等一段时间后，即可在 Maven Central 搜索到对应版本。

---

🚀 三、使用 scripts/release.sh 一键发布
---------------------------------

为了减少每次输错命令的可能性，项目中提供了一个简单的发布脚本：

- 路径：`scripts/release.sh`

脚本做的事情等价于：

1. 调用 Maven 读取当前 `pom.xml` 中的版本号
2. 询问你是否确认发布这个版本
3. 执行：
   - `mvn clean package -P release`
   - `mvn central-publishing:publish -P release`

### 1. 第一次使用前给脚本加执行权限

```bash
chmod +x scripts/release.sh
```

### 2. 每次发版时的使用步骤

1. 修改 `pom.xml` 版本号、更新文档，提交代码（可选但推荐）：

   ```bash
   # 示例
   git add pom.xml README_zh.md
   git commit -m "release: bump version to 0.13.1"
   ```

2. 执行发布脚本：

   ```bash
   ./scripts/release.sh
   ```

3. 脚本会显示：
   - 项目根目录
   - 当前 POM 版本号
   - 询问是否确认发布：

   ```text
   📦 当前 POM 版本：0.13.0-codingox-1
   确认发布版本 0.13.0-codingox-1 到 Maven Central？(y/N)
   ```

   输入 `y` / `yes` 确认后，会依次执行：

   - `mvn clean package -P release`
   - `mvn central-publishing:publish -P release`

4. 完成后，脚本会输出类似提示：

   ```text
   ✅ 发布命令执行完成。
   👉 请在浏览器中打开 https://central.sonatype.com/publishing/deployments 查看本次 Deployment 状态。
   ```

5. 最后，建议打 Tag 并推送到 GitHub（可选但非常推荐）：

   ```bash
   VERSION=0.13.0-codingox-1  # 换成本次实际版本
   git tag -a "v${VERSION}" -m "Release ${VERSION}"
   git push origin HEAD --tags
   ```

---

✅ 四、快速记忆版 checklist
-------------------------

每次发版可以按这个顺序自查一遍：

1. 改 `pom.xml` 版本号（例如 `0.13.1`）
2. 更新 README / 变更记录
3. `git status` 确认工作区干净，必要时做一次 commit
4. 确保 `~/.m2/settings.xml` 的 `central` / `gpg` 配置已经存在且可用
5. 执行：`./scripts/release.sh`
6. 去 Central Portal 的 Deployments 页面确认状态为 Published
7. 给本次版本打 Tag 并 push 到远程仓库

这样，你之后每次发版基本只需要记住一句话：

> “改版本 → 更新文档 → `./scripts/release.sh` → 去 Portal 看结果 → 打 tag”

