netty-websocket-spring-boot-starter：在 Spring Boot 3 中重新审视自动配置与 @EnableXXX
==============================================================================

🎯 本文目标

从一个真实的 starter（`netty-websocket-spring-boot-starter`）出发，梳理：

- 为什么同一个 starter 里会同时存在 `spring.factories` 和 `AutoConfiguration.imports`
- 为什么指向的是两个不同的自动配置类
- 在 Spring Boot 3 下，推荐怎样写自动配置
- `@EnableXXX`（如 `@EnableWebSocket`）在新体系中的“显式开关”角色

---

✨ 一、两套 META-INF 配置长什么样？
-----------------------------------

在当前项目中，`src/main/resources/META-INF` 有两种自动导入方式。

1. 旧方案：`spring.factories`

```properties
# src/main/resources/META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.yeauty.autoconfigure.NettyWebSocketAutoConfigure
```

2. 新方案：`AutoConfiguration.imports`

```text
# src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
org.yeauty.autoconfigure.NettyWebSocketAutoConfiguration
```

它们分别导入两个不同的类：

- `NettyWebSocketAutoConfigure`（旧的、给 Spring Boot 2.x 使用）
- `NettyWebSocketAutoConfiguration`（新的、符合 Spring Boot 3 推荐写法）

看起来像“重复配置”，但实际上是为了兼容不同代的 Spring Boot，并对职责做了更清晰的拆分。

---

🔧 二、Spring Boot 2 时代：spring.factories + @EnableWebSocket 链路
-----------------------------------------------------------------

旧时代的自动装配入口是 `spring.factories`，配置指向：

```java
// org.yeauty.autoconfigure.NettyWebSocketAutoConfigure
@EnableWebSocket
public class NettyWebSocketAutoConfigure {
}
```

关键在于这个注解：

```java
// org.yeauty.annotation.EnableWebSocket
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(NettyWebSocketSelector.class)
public @interface EnableWebSocket {

    String[] scanBasePackages() default {};
}
```

真正提供 Bean 的，是被导入的配置类：

```java
// org.yeauty.annotation.NettyWebSocketSelector
@Configuration
@ConditionalOnMissingBean(ServerEndpointExporter.class)
public class NettyWebSocketSelector {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
```

这条链路可以概括为：

1. Spring Boot 2 通过 `spring.factories` 加载 `NettyWebSocketAutoConfigure`
2. `NettyWebSocketAutoConfigure` 上的 `@EnableWebSocket` 生效
3. `@EnableWebSocket` 使用 `@Import(NettyWebSocketSelector.class)` 导入核心配置
4. `NettyWebSocketSelector` 注册 `ServerEndpointExporter` 等核心 Bean

这里的特点是：**自动配置入口和 `@EnableWebSocket` 混在一起**。

---

🚀 三、Spring Boot 3 推荐模式：@AutoConfiguration + AutoConfiguration.imports
---------------------------------------------------------------------------

从 Spring Boot 2.7 / 3.x 开始，官方推荐新的自动配置写法：

1. 自动配置类使用 `@AutoConfiguration`
2. 在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中声明

对应代码如下：

```java
// org.yeauty.autoconfigure.NettyWebSocketAutoConfiguration
@AutoConfiguration
@Import(NettyWebSocketSelector.class)
public class NettyWebSocketAutoConfiguration {
}
```

配套的资源文件：

```text
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
org.yeauty.autoconfigure.NettyWebSocketAutoConfiguration
```

可以看到，新老两条链路最后都指向：

- `@Import(NettyWebSocketSelector.class)`
- `NettyWebSocketSelector` 中定义核心 Bean

区别在于：

- 旧方案：通过 `spring.factories` 间接触发 `@EnableWebSocket`，再导入配置
- 新方案：由 `@AutoConfiguration` 直接配合 `AutoConfiguration.imports` 管理自动配置

对于一个面向 Spring Boot 3 的 starter 来说，**新方案是推荐做法**。

---

💡 四、如何正确理解 @EnableXXX 的角色？
--------------------------------------

很多人的疑惑是：

> “Spring Boot 3 下是不是就不推荐再用 `@EnableXXX` 了？”

更准确的说法是：

- 对于 **starter 的自动装配入口**，更推荐用 `@AutoConfiguration` + `AutoConfiguration.imports`
- 对于 **用户显式控制功能开关**，`@EnableXXX` 依然非常有价值

在这个项目中：

- `@EnableWebSocket` 的职责，是提供一个“显式开启 Netty WebSocket”的注解
- 它通过 `@Import(NettyWebSocketSelector.class)` 启用一整套配置

可以用一句话来概括它的定位：

> `@EnableXXX` 不再是唯一的自动配置入口，  
> 更像是**给用户用的“显式开启”开关**。

典型适用场景包括：

1. 在非 Spring Boot 的纯 Spring 应用中，想快速启用一整套配置
2. 在某些复杂场景下，不希望自动装配，而是让用户自己选择是否标注 `@EnableWebSocket`

而对于“正常使用 starter 的 Spring Boot 3 项目”，**不再建议要求用户去写 `@EnableWebSocket`**。

---

✅ 五、在 Spring Boot 3 下的推荐实践
-----------------------------------

结合上面的分析，可以给出一套相对清晰的实践建议。

1. starter 作者（维护者）侧

推荐保留三种角色清晰的类：

- 自动配置类：`NettyWebSocketAutoConfiguration`
- 核心配置类：`NettyWebSocketSelector`
- 显式开关注解：`@EnableWebSocket`

推荐的自动配置写法：

```java
// 自动配置入口（面向 Spring Boot 3）
@AutoConfiguration
@Import(NettyWebSocketSelector.class)
public class NettyWebSocketAutoConfiguration {
}
```

```text
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
org.yeauty.autoconfigure.NettyWebSocketAutoConfiguration
```

保留 `@EnableWebSocket`，但将其定位在：

- 非 Boot 环境 / 手动配置场景
- 需要“显式一眼看到功能开启”的高级用法

如果仍然要兼容 Spring Boot 2，可以继续保留：

- `spring.factories` 中的 `NettyWebSocketAutoConfigure`
- `NettyWebSocketAutoConfigure` 通过 `@EnableWebSocket` 导入 `NettyWebSocketSelector`

2. starter 使用者（业务开发者）侧

在一个 Spring Boot 3 项目中，推荐使用方式是：

- `pom.xml` 中只需引入 starter 依赖
- 不必额外声明 `ServerEndpointExporter` Bean
- 一般情况下也**不必在启动类上加 `@EnableWebSocket`**

示例依赖：

```xml
<dependency>
    <groupId>org.yeauty</groupId>
    <artifactId>netty-websocket-spring-boot-starter</artifactId>
    <version>0.13.0</version>
</dependency>
```

之后只需要在业务端点类上使用框架提供的注解即可，例如：

```java
@ServerEndpoint(path = "/ws/{arg}")
public class MyWebSocket {
    // 省略业务方法
}
```

自动配置将由 `@AutoConfiguration` + `AutoConfiguration.imports` 自动完成。

---

🧾 六、小结：一句话记住这两件事
-----------------------------

1. 对于 Spring Boot 3 的 starter 自动配置，**推荐使用 `@AutoConfiguration` + `AutoConfiguration.imports`**，而不是继续依赖 `spring.factories` + `@EnableXXX` 组合。
2. `@EnableXXX`（如 `@EnableWebSocket`）更适合被视为**“显式开启某功能的用户级开关”**，在需要手动控制或非 Boot 场景下依然非常有价值。

这样设计之后：

- 自动配置路径符合 Spring Boot 3 的官方推荐
- 用户依赖体验更简单：加依赖即生效
- 同时保留了对高级场景、非 Boot 场景的灵活控制能力

