package com.timzaak.cloud.order.third;

import com.timzaak.cloud.order.entity.OrderInfo;
import com.timzaak.cloud.user.convert.JsonbTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfiguration {
    @Bean
    ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return (configuration) -> {
            final var registry = configuration.getTypeHandlerRegistry();
            registry.register(OrderInfo.class, JdbcType.OTHER, JsonbTypeHandler.class);
        };
    }
}
