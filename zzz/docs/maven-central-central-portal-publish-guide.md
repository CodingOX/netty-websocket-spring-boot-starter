Maven Central 发布指南（Central Portal 新方案）
==========================================

🎯 本文目标

面向已经在 **Sonatype Central Portal** 上创建好命名空间（例如：`io.github.codingox`）的开发者，完整说明如何：

- 使用 **GPG** 给构件签名
- 在 `~/.m2/settings.xml` 配置 Central Portal 的 Token
- 在 `pom.xml` 中使用 **central-publishing-maven-plugin**
- 通过 Maven 一键发布到 Maven Central（新方案，**不再依赖 `s01.oss.sonatype.org` / `nexus-staging-maven-plugin`**）

---

✨ 一、整体发布流程概览
-------------------

以当前项目 `io.github.codingox:netty-websocket-spring-boot-starter` 为例，完整发布链路是：

1. 在 Central Portal 上创建/验证命名空间，例如：`io.github.codingox`
2. 在 Central Portal 账户页面生成 **User Token**（Portal Token）
3. 在本机安装 GPG，并生成一把 GPG 密钥用于签名
4. 在 `~/.m2/settings.xml` 中：
   - 配置 `<server id="central">`，使用 Portal Token
   - 配置一个 `gpg` profile，提供 GPG 密码（简单方案）
5. 在项目 `pom.xml` 中：
   - 保留 `maven-source-plugin`、`maven-javadoc-plugin`、`maven-gpg-plugin`
   - 移除老的 `nexus-staging-maven-plugin`、`distributionManagement`
   - 增加 `central-publishing-maven-plugin`
6. 执行：
   - `mvn clean package -P release`
   - `mvn central-publishing:publish -P release`
7. 在 Central Portal 的 Deployments 页面查看发布状态，等待同步到 Maven Central。

---

🔑 二、准备工作：Central Portal 命名空间与 Token
-----------------------------------------

1. 打开 Central Portal 并登录
   - 访问：`https://central.sonatype.com`
   - 使用 GitHub / Google / 邮箱注册或登录

2. 创建或验证命名空间
   - 访问：`https://central.sonatype.com/publishing/namespaces`
   - 根据向导创建或迁移你的命名空间，例如：`io.github.codingox`
   - 通过域名、GitHub 组织等方式完成所有权验证

3. 生成用于 Maven 发布的 Portal User Token
   - 访问：`https://central.sonatype.com/account` 或 `https://central.sonatype.com/usertoken`
   - 在页面中生成一个 User Token，得到一对值：
     - `username`（类似随机短字符串）
     - `password` / `token`（一长串随机字符）

这对 `username/password` 将配置在 `~/.m2/settings.xml` 中，供 Maven 插件调用 Central Portal API 使用。

---

🛠 三、本地安装与配置 GPG
----------------------

Central Publishing 插件不会自动帮你做 GPG 签名，而 Maven Central 要求正式版必须有 PGP/GPG 签名，因此仍需在本地准备好 GPG 环境。

1. 安装 GPG（macOS 示例）

```bash
brew install gnupg
gpg --version
```

2. 生成一把 GPG 密钥（只需一次）

```bash
gpg --full-generate-key
```

推荐选择：

- 密钥类型：回车（`RSA and RSA`）
- 密钥长度：`4096`
- 有效期：例如 `3y`（3 年）
- Name / Email：建议与 Central Portal 使用的邮箱一致
- 最后设置一个 GPG 密码（passphrase）

3. 查看并确认生成的密钥

```bash
gpg --list-secret-keys --keyid-format LONG
```

你会看到类似：

```text
sec   rsa4096/E1BFC1BD0D8729C4 2025-11-20 [SC]
      FA2DD1CA482D17DC234BC133E1BFC1BD0D8729C4
uid                   [ 绝对 ] Your Name <you@example.com>
ssb   rsa4096/A543FCA3FDDE670A 2025-11-20 [E]
```

这说明 GPG 本地配置正常，可以用于后续的 Maven 签名。

> 可选但推荐：将公钥上传到公共 keyserver（如 keys.openpgp.org），方便他人验证签名。

---

📦 四、配置 Maven 的 settings.xml（Central + GPG）
-------------------------------------------

编辑（若不存在则创建）`~/.m2/settings.xml`，核心有三块：

1. Central Portal 的 `server` 配置

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <!-- 其他私有仓库配置略 -->

    <!-- Central Portal 发布凭证（使用 central.sonatype.com 页面生成的 User Token） -->
    <server>
      <id>central</id>
      <username>你的-central-portal-username</username>
      <password>你的-central-portal-token</password>
    </server>
  </servers>
```

其中：

- `<id>central</id>` 必须与 `pom.xml` 里 `publishingServerId` 对应
- `<username>` / `<password>` 来自 Central Portal 的 User Token 页面

2. GPG profile（简单方案：明文 passphrase）

```xml
  <profiles>
    <!-- GPG 签名配置 -->
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>你的-gpg-密码</gpg.passphrase>
      </properties>
    </profile>

    <!-- 可选：自定义仓库镜像配置（如阿里云） -->
    <profile>
      <id>nexus</id>
      ...
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>gpg</activeProfile>
    <activeProfile>nexus</activeProfile>
  </activeProfiles>

</settings>
```

注意：

- 不需要再配置 `<activeProfile>central</activeProfile>`，否则会有 “profile central 不存在” 的 warning。
- 这一方案会把 GPG 密码写在本地 `settings.xml` 中，仅用于开发机，后续可以再根据安全要求做封装（例如只用 gpg-agent）。

---

🧩 五、项目 POM 配置（central-publishing-maven-plugin）
---------------------------------------------

下面以当前项目的 `pom.xml` 为例，核心变更点是：

- groupId 改为你的命名空间：`io.github.codingox`
- 移除老的 OSSRH 发布配置：`nexus-staging-maven-plugin`、`distributionManagement`
- 加上 Central Publishing 插件：`central-publishing-maven-plugin`

1. 基本坐标与属性

```xml
<groupId>io.github.codingox</groupId>
<artifactId>netty-websocket-spring-boot-starter</artifactId>
<version>0.13.0-codingox-1</version>

<properties>
    <netty.version>4.1.118.Final</netty.version>
    <spring-boot.version>3.3.5</spring-boot.version>

    <!-- 版本参数，方便升级 -->
    <central-publishing-maven-plugin.version>0.9.0</central-publishing-maven-plugin.version>
    <maven-gpg-plugin.version>1.6</maven-gpg-plugin.version>
</properties>
```

2. 在 `release` profile 中配置源码 / javadoc / GPG / Central Publishing

```xml
<profiles>
  <profile>
    <id>release</id>
    <build>
      <plugins>
        <!-- Sources Plugin：生成源码 jar -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-source-plugin</artifactId>
          <version>3.0.1</version>
          <executions>
            <execution>
              <goals>
                <goal>jar-no-fork</goal>
              </goals>
            </execution>
          </executions>
        </plugin>

        <!-- Javadoc Plugin：生成 javadoc jar -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-javadoc-plugin</artifactId>
          <version>3.0.1</version>
          <executions>
            <execution>
              <goals>
                <goal>jar</goal>
              </goals>
              <configuration>
                <additionalOptions>
                  <additionalOption>-Xdoclint:none</additionalOption>
                </additionalOptions>
              </configuration>
            </execution>
          </executions>
        </plugin>

        <!-- GPG 签名插件：给所有构件生成 .asc 签名 -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-gpg-plugin</artifactId>
          <version>${maven-gpg-plugin.version}</version>
          <executions>
            <execution>
              <phase>verify</phase>
              <goals>
                <goal>sign</goal>
              </goals>
            </execution>
          </executions>
          <configuration>
            <!-- 无 TTY 环境下使用 loopback 模式，从 settings.xml 读取 passphrase -->
            <gpgArguments>
              <arg>--pinentry-mode</arg>
              <arg>loopback</arg>
            </gpgArguments>
          </configuration>
        </plugin>

        <!-- Central Publishing Maven Plugin：通过 Central Portal 发布 -->
        <plugin>
          <groupId>org.sonatype.central</groupId>
          <artifactId>central-publishing-maven-plugin</artifactId>
          <version>${central-publishing-maven-plugin.version}</version>
          <extensions>true</extensions>
          <configuration>
            <!-- 对应 settings.xml 中 <server><id>central</id> -->
            <publishingServerId>central</publishingServerId>
            <!-- 自动完成发布流程 -->
            <autoPublish>true</autoPublish>
            <!-- 直接发布为 published 状态 -->
            <publishingType>published</publishingType>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

3. 移除老的 OSSRH / s01 配置

- 删除 `nexus-staging-maven-plugin`
- 删除 `distributionManagement` 中指向 `https://s01.oss.sonatype.org/...` 的配置

新方案完全通过 Central Portal API 发布，不再直接访问 `s01.oss.sonatype.org`。

---

🚀 六、发布命令与流程
------------------

准备工作完成后，发布正式版本的流程非常简单：

1. 在项目根目录执行构建 + 签名：

```bash
mvn clean package -P release
```

2. 执行 Central Publishing 发布：

```bash
mvn central-publishing:publish -P release
```

执行输出中你会看到类似日志：

```text
Uploaded bundle successfully, deploymentId: xxx
Deployment xxx has been validated. To finish publishing visit:
https://central.sonatype.com/publishing/deployments
```

此时：

- 构件已经上传到 Central Portal
- 根据 `autoPublish` / `publishingType` 配置，将自动变为 Published 状态（可能需要一点时间）

---

🔍 七、在 Central Portal 中确认发布结果
-----------------------------------

1. 打开 Deployments 页面

- 访问：`https://central.sonatype.com/publishing/deployments`
- 找到刚刚那次 `deploymentId` 对应的记录
- 查看状态是否从 `Validating` / `Pending` 变为 `Published`

2. 在 Artifact 页面搜索验证

- 等待一段时间（通常十几分钟内）
- 在 Central 搜索你的构件，例如：
  - Namespace：`io.github.codingox`
  - Artifact：`netty-websocket-spring-boot-starter`

3. 在 Maven / Gradle 项目中尝试引用

```xml
<dependency>
    <groupId>io.github.codingox</groupId>
    <artifactId>netty-websocket-spring-boot-starter</artifactId>
    <version>0.13.0-codingox-1</version>
</dependency>
```

如果能正常解析依赖，说明发布流程已完全打通。

---

✅ 八、与老方案（OSSRH + nexus-staging）的对比
-----------------------------------------

旧方案（你之前踩坑的那条路）：

- 需要在 `s01.oss.sonatype.org` 拿 OSSRH User Token
- POM 中配置：
  - `distributionManagement` → `https://s01.oss.sonatype.org/...`
  - `nexus-staging-maven-plugin`（deploy / close / release）
- 使用 `mvn deploy -P release` 发布

新方案（本文方案）：

- 不再直接访问 `s01.oss.sonatype.org`
- 使用：
  - Central Portal 账户 + Portal User Token
  - `central-publishing-maven-plugin` 调用 Portal API
  - GPG 签名仍保留，但部署完全由 Central 处理
- 使用：

```bash
mvn clean package -P release
mvn central-publishing:publish -P release
```

两种方案最终都会把构件发布到 Maven Central，  
但新方案更加统一，且是 Sonatype 当前重点推荐的路径。

---

🧾 九、小结
----------

1. **GPG 仍然需要**：Central Publishing 不会替你签名，正式发布必须保留 `maven-gpg-plugin`。
2. **settings.xml 中配置的是 `server id="central"`，不再是 `ossrh`**。
3. **POM 中不再需要 `nexus-staging-maven-plugin` 和 `distributionManagement`**，改用 `central-publishing-maven-plugin` 即可。
4. 超过一次发布后，这套配置基本可以在你所有库中复用，只需调整 `groupId` / `artifactId` / `version` 即可。

如果你后续还想为多模块项目、SNAPSHOT 仓库、CI/CD 流水线做进一步自动化，可以在此文档基础上继续扩展发布策略。 

