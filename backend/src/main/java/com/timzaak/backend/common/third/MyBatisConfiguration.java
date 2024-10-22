package com.timzaak.backend.common.third;

import com.timzaak.backend.common.convert.JsonbTypeHandler;
import com.timzaak.backend.entity.order.OrderInfo;
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
