package com.pm.erp.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Render (and most PaaS Postgres add-ons) inject a single DATABASE_URL in
 * "postgres://user:pass@host:port/db" form, but Spring's DataSource needs a
 * JDBC URL plus separate username/password. This converts DATABASE_URL, when
 * present, into SPRING_DATASOURCE_URL/USERNAME/PASSWORD before the datasource
 * binds — with lower precedence than any of those set explicitly.
 */
public class RenderDatabaseUrlConverter implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        URI uri = URI.create(databaseUrl);
        String[] userInfo = uri.getUserInfo() != null ? uri.getUserInfo().split(":", 2) : new String[0];
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();

        Map<String, Object> derived = new LinkedHashMap<>();
        derived.put("spring.datasource.url", jdbcUrl);
        if (userInfo.length > 0) {
            derived.put("spring.datasource.username", userInfo[0]);
        }
        if (userInfo.length > 1) {
            derived.put("spring.datasource.password", userInfo[1]);
        }
        environment.getPropertySources().addLast(new MapPropertySource("databaseUrl", derived));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
