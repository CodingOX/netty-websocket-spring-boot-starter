package org.yeauty.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yeauty.standard.ServerEndpointExporter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot 3 自动配置行为测试
 *
 * 通过 ApplicationContextRunner 验证：
 * 1. 引入 NettyWebSocketAutoConfiguration 后，可以获得 ServerEndpointExporter Bean
 * 2. 提供一个带 @SpringBootApplication 注解的普通 Bean，避免 ServerEndpointExporter 在扫描包时抛出异常
 */
class NettyWebSocketAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestBootApplicationConfig.class)
                    .withConfiguration(AutoConfigurations.of(NettyWebSocketAutoConfiguration.class));

    @Test
    void shouldProvideServerEndpointExporterBean() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(ServerEndpointExporter.class));
    }

    @Configuration
    static class TestBootApplicationConfig {

        /**
         * 提供一个带 @SpringBootApplication 注解的普通 Bean，
         * 仅用于满足 ServerEndpointExporter#scanPackage 中对
         * @SpringBootApplication 的依赖，不参与实际自动配置。
         */
        @Bean
        TestBootApplication testBootApplication() {
            return new TestBootApplication();
        }
    }

    @SpringBootApplication
    static class TestBootApplication {
    }
}

