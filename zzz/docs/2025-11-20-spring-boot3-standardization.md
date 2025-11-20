# Spring Boot 3 标准化适配实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:**  
将 `netty-websocket-spring-boot-starter` 的自动装配与依赖管理改造为符合 Spring Boot 3
推荐的第三方 Starter 规范，同时保持对现有使用方式的最大兼容性。

**Architecture:**  
通过新增 `NettyWebSocketAutoConfiguration` 作为单一自动配置入口，使用
`AutoConfiguration.imports` 完成注册；保留 `@EnableWebSocket` 供非 Spring Boot 场景使用，
并将其实现复用到自动配置类。引入基础集成测试验证自动装配行为。

**Tech Stack:**  
JDK 17、Spring Framework 6.x、Spring Boot 3.3.x、Netty 4.1.118.Final、Maven。

---

### Task 1: 准备工作与基线构建

**Files:**
- 修改：无（命令行与 Git 操作）
- 参考：`zzz/todo`

**Step 1: 创建功能分支**

命令：

```bash
git checkout -b feature/sb3-autoconfig-standardization
```

预期：切换到新分支 `feature/sb3-autoconfig-standardization`。

**Step 2: 运行现有构建，确认基线可用**

命令：

```bash
mvn -q -DskipTests=false clean test
```

预期：构建成功；如果有失败测试，记录失败信息，暂不修改代码，只记在 `zzz/todo`。

**Step 3: 简要复盘当前 Spring Boot 3 适配现状**

动作：
- 打开 `pom.xml`，确认 `spring-boot-autoconfigure` 版本为 3.3.x。
- 打开 `src/main/resources/META-INF/spring.factories`，确认自动配置注册方式。
- 打开 `zzz/todo`，回顾当前改进建议。

预期：对当前实现与待改造点有统一认识。

**Step 4: 建立后续任务的提交策略**

决定：
- 每完成一个 Task 的代码与测试，就进行一次 Git 提交。
- 提交信息形如：`feat(sb3): add NettyWebSocketAutoConfiguration`。

预期：后续实现过程中的历史变更清晰可追溯。

**Step 5: 更新 `zzz/todo`，记录本次计划入口**

动作：
- 在 `zzz/todo` 顶部或底部补充一行，说明已创建 Spring Boot 3 标准化实施计划，
  文件路径为 `docs/plans/2025-11-20-spring-boot3-standardization.md`。

---

### Task 2: 引入 NettyWebSocketAutoConfiguration 骨架（不改变现有行为）

**Files:**
- 创建：`src/main/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfiguration.java`
- 参考：`target/classes/org/yeauty/annotation/NettyWebSocketSelector.class`
- 测试：`src/test/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfigurationTests.java`
  （新增）

**Step 1: 编写失败测试，验证自动配置类能提供 ServerEndpointExporter**

动作：
- 新建测试类
  `src/test/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfigurationTests.java`。
- 使用 `ApplicationContextRunner`（Spring Boot 测试工具）编写测试，
  断言在导入 `NettyWebSocketAutoConfiguration` 时，容器中存在
  `ServerEndpointExporter` Bean。

示例片段（示意，后续实现时可细化）：

```java
class NettyWebSocketAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(NettyWebSocketAutoConfiguration.class));

    @Test
    void shouldProvideServerEndpointExporterBean() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(ServerEndpointExporter.class));
    }
}
```

预期：编译通过，但测试执行时会失败（因为自动配置类尚未存在或尚未提供 Bean）。

**Step 2: 运行测试，确认处于红灯状态（TDD）**

命令：

```bash
mvn -q -Dtest=org.yeauty.autoconfigure.NettyWebSocketAutoConfigurationTests test
```

预期：测试失败，错误信息提示找不到类或 Bean。

**Step 3: 创建最小实现的 NettyWebSocketAutoConfiguration 骨架**

动作：
- 新建类 `NettyWebSocketAutoConfiguration`，所在包
  `org.yeauty.autoconfigure`。
- 使用 `@AutoConfiguration` 注解（来自 Spring Boot 3）。
- 初始版本先通过 `@Import(NettyWebSocketSelector.class)` 复用现有配置，
  不直接复制逻辑，避免一次性大重构。

示例片段（示意）：

```java
@AutoConfiguration
@Import(NettyWebSocketSelector.class)
public class NettyWebSocketAutoConfiguration {
}
```

预期：仅保证测试中的类可被引用，尚未接入自动装配注册机制。

**Step 4: 再次运行测试，确保测试通过**

命令：

```bash
mvn -q -Dtest=org.yeauty.autoconfigure.NettyWebSocketAutoConfigurationTests test
```

预期：测试通过，证明通过显式导入配置类可以获得 `ServerEndpointExporter`。

**Step 5: Git 提交**

命令：

```bash
git add src/main/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfiguration.java \
       src/test/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfigurationTests.java
git commit -m "feat(sb3): add NettyWebSocketAutoConfiguration skeleton"
```

---

### Task 3: 从 spring.factories 迁移到 AutoConfiguration.imports（保留兼容）

**Files:**
- 创建：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 修改：`src/main/resources/META-INF/spring.factories`
- 测试：复用 `NettyWebSocketAutoConfigurationTests`，后续可按需补充。

**Step 1: 创建 AutoConfiguration.imports 文件**

动作：
- 在 `src/main/resources/META-INF/spring/` 目录下创建
  `org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 写入单行：

```text
org.yeauty.autoconfigure.NettyWebSocketAutoConfiguration
```

预期：Spring Boot 3 能通过新机制发现该自动配置类。

**Step 2: 保留 spring.factories 项（用于兼容旧工程）**

动作：
- 打开 `src/main/resources/META-INF/spring.factories`。
- 保留原有内容：

```text
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.yeauty.autoconfigure.NettyWebSocketAutoConfigure
```

- 在代码注释或文档中说明：`spring.factories` 为历史兼容路径，长期计划可在
  Spring Boot 4 之后移除。

预期：新老两种方式并存，新项目依赖新机制，旧项目仍可正常运行。

**Step 3: 编写（或扩展）测试，验证通过 Spring Boot 自动装配能拿到 Bean**

动作：
- 在 `NettyWebSocketAutoConfigurationTests` 中增加一个使用
  `ApplicationContextRunner` 的用例，不显式导入配置类，仅依赖
  自动装配机制。
- 测试中通过设置 `spring.autoconfigure.exclude` 等属性，确保真正通过
  `AutoConfiguration.imports` 生效（可在实现时具体推敲）。

预期：测试能证明在典型 Spring Boot 应用环境中会自动出现
`ServerEndpointExporter`。

**Step 4: 运行相关测试**

命令：

```bash
mvn -q -Dtest=org.yeauty.autoconfigure.NettyWebSocketAutoConfigurationTests test
```

预期：所有测试通过。

**Step 5: Git 提交**

命令：

```bash
git add src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
       src/main/resources/META-INF/spring.factories \
       src/test/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfigurationTests.java
git commit -m "feat(sb3): register auto-configuration via AutoConfiguration.imports"
```

---

### Task 4: 收敛配置逻辑，解耦 @EnableWebSocket 与 AutoConfiguration

**Files:**
- 修改：`src/main/java/org/yeauty/annotation/EnableWebSocket.java`
- 修改：`src/main/java/org/yeauty/annotation/NettyWebSocketSelector.java`
- 修改：`src/main/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfiguration.java`
- 测试：在现有测试基础上补充一个非 Spring Boot 场景下使用 `@EnableWebSocket`
  的简单配置测试（如新建一个纯 Spring Context）。

**Step 1: 为当前行为添加保护性测试**

动作：
- 新建测试类，例如：
  `src/test/java/org/yeauty/annotation/EnableWebSocketTests.java`。
- 通过 `AnnotationConfigApplicationContext` 手动注册一个使用
  `@EnableWebSocket` 的配置类，断言可以获得 `ServerEndpointExporter`。

预期：测试描述当前行为，确保后续重构不破坏非 Spring Boot 场景。

**Step 2: 将核心 Bean 定义迁移到 NettyWebSocketAutoConfiguration**

动作：
- 打开 `NettyWebSocketSelector` 反编译对应源码（或直接从现有源码中迁移，
  保持字段与 Bean 定义不变）。
- 在 `NettyWebSocketAutoConfiguration` 中直接定义
  `@Bean ServerEndpointExporter`，并保留 `@ConditionalOnMissingBean`。
- 将 `NettyWebSocketSelector` 精简为仅负责兼容旧用法，内部可 `@Import`
  `NettyWebSocketAutoConfiguration`。

预期：自动配置类成为“单一真相来源”，`@EnableWebSocket` 只做简单转发。

**Step 3: 调整 @EnableWebSocket 实现，指向新的自动配置类**

动作：
- 打开 `EnableWebSocket` 注解定义。
- 将 `@Import(NettyWebSocketSelector.class)` 调整为更直接、清晰的导入链
  （例如 `@Import(NettyWebSocketAutoConfiguration.class)` 或保留 Selector，
  但其内部再导入 AutoConfiguration）。

预期：非 Boot 用户使用 `@EnableWebSocket` 时，最终仍然走
`NettyWebSocketAutoConfiguration` 的逻辑。

**Step 4: 全量运行相关测试**

命令：

```bash
mvn -q test
```

预期：之前为自动配置和 `@EnableWebSocket` 新增的所有测试全部通过。

**Step 5: Git 提交**

命令：

```bash
git add src/main/java/org/yeauty/annotation/EnableWebSocket.java \
       src/main/java/org/yeauty/annotation/NettyWebSocketSelector.java \
       src/main/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfiguration.java \
       src/test/java/org/yeauty/autoconfigure/NettyWebSocketAutoConfigurationTests.java \
       src/test/java/org/yeauty/annotation/EnableWebSocketTests.java
git commit -m "refactor(sb3): centralize websocket configuration in auto-configuration"
```

---

### Task 5: 优化 POM 中的 Spring Boot 依赖声明方式

**Files:**
- 修改：`pom.xml`
- 测试：复用现有所有测试。

**Step 1: 检查当前 Spring Boot 相关依赖声明**

动作：
- 打开 `pom.xml`。
- 找到 `spring-boot-autoconfigure` 依赖，注意其中 `<version>` 属性。

预期：确认当前是否显式指定了 Spring Boot 版本。

**Step 2: 去掉 spring-boot-autoconfigure 的版本号（交由 BOM 管理）**

动作：
- 在 `pom.xml` 中，将 `spring-boot-autoconfigure` 依赖中的 `<version>` 标签删除，
  保留 `<optional>true</optional>`。

预期：Starter 不再强推 Spring Boot 版本，让使用方通过 BOM 统一管理。

**Step 3: 确认项目仍可构建**

命令：

```bash
mvn -q -DskipTests=false clean test
```

预期：构建成功；如因缺少 BOM 导致版本解析问题，可在文档中明确说明使用方式，
而不是在 Starter 中强行指定版本。

**Step 4: 在 README_zh.md 中补充版本管理建议**

动作：
- 在 `README_zh.md` 中新增一小节“与 Spring Boot 版本的兼容性”。
- 说明推荐在业务项目中通过 `spring-boot-dependencies` BOM 管理版本，
  本 Starter 不强制绑定具体 Spring Boot 版本。

**Step 5: Git 提交**

命令：

```bash
git add pom.xml README_zh.md
git commit -m "chore(sb3): align starter with BOM-based version management"
```

---

### Task 6: 文档与示例工程的 Spring Boot 3 用法更新

**Files:**
- 修改：`README.md`
- 修改：`README_zh.md`
- 可选：新增 `zzz` 或单独仓库中的示例工程说明。

**Step 1: 在 README_zh.md 中补充 Spring Boot 3 使用说明**

动作：
- 增加一个“小节”，说明在 Spring Boot 3 工程中只需引入本 Starter 依赖，
  自动装配会通过 `AutoConfiguration.imports` 生效，无需手动添加
  `@EnableWebSocket`。

**Step 2: 在 README.md 中同步英文说明（如需要）**

动作：
- 用简要英文补充与中文一致的内容，保持中英文文档一致性。

**Step 3: 在文档中标注历史兼容路径**

动作：
- 说明仍然保留 `spring.factories` 注册方式，以兼容老工程；
  建议新项目以 Spring Boot 3 推荐方式为主。

**Step 4: 检查文档中的示例配置**

动作：
- 检查 README 中是否有需要手动声明 `ServerEndpointExporter` 的示例，
  如有，与新自动装配行为保持一致。

**Step 5: Git 提交**

命令：

```bash
git add README.md README_zh.md
git commit -m "docs(sb3): document modern auto-configuration usage"
```

---

### Task 7: 最终回归验证与清理

**Files:**
- 修改：`zzz/todo`
- 参考：`docs/plans/2025-11-20-spring-boot3-standardization.md`

**Step 1: 全量构建与测试**

命令：

```bash
mvn -q -DskipTests=false clean test
```

预期：所有测试通过。

**Step 2: 在一个真实或临时 Spring Boot 3 应用中验证 Starter**

动作：
- 在本地准备一个最小化 Spring Boot 3 Web 工程，
  引入该 Starter 的 SNAPSHOT 或本地安装版本。
- 不写任何额外配置，直接通过 `@ServerEndpoint` 定义端点，
  验证自动装配是否能正确启动 Netty WebSocket 服务。

预期：应用启动成功，端点可通过 WebSocket 客户端成功连接。

**Step 3: 更新 zzz/todo，标记标准化任务完成状态**

动作：
- 在 `zzz/todo` 中补充“Spring Boot 3 标准化适配”任务的完成记录，
  包含完成日期、主要变更点、测试方式等。

**Step 4: 代码自查与风格统一**

动作：
- 检查新增与修改类的包名、命名、注释是否统一。
- 运行常用静态检查或 IDE 自带格式化。

**Step 5: 最终 Git 提交（或准备发布版本）**

命令（示例）：

```bash
git status
git commit -am "chore(sb3): finalize standardized Spring Boot 3 support"
```

预期：工作分支干净，可根据项目流程发起 PR 或发布新版本。

