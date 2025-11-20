package org.yeauty.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.yeauty.annotation.NettyWebSocketSelector;

/**
 * Netty WebSocket 自动配置入口（Spring Boot 3 风格）
 *
 * 初始版本仅通过 @Import 复用现有 NettyWebSocketSelector 逻辑，
 * 后续可以逐步将核心 Bean 定义收敛到该类中。
 */
@AutoConfiguration
@Import(NettyWebSocketSelector.class)
public class NettyWebSocketAutoConfiguration {
}

