package com.pm.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;

@SpringBootApplication
public class ErpApplication {

    public static void main(String[] args) {
        applyRenderDatabaseUrl();
        SpringApplication.run(ErpApplication.class, args);
    }

    /**
     * Render (and most PaaS Postgres add-ons) inject a single DATABASE_URL in
     * "postgres://user:pass@host:port/db" form, but Spring's DataSource needs a
     * JDBC URL plus separate username/password. Converts it, when present, into
     * the system properties Spring reads for the datasource — skipped if
     * SPRING_DATASOURCE_URL is already set explicitly.
     */
    private static void applyRenderDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        if (System.getenv("SPRING_DATASOURCE_URL") != null) {
            return;
        }
        URI uri = URI.create(databaseUrl);
        String[] userInfo = uri.getUserInfo() != null ? uri.getUserInfo().split(":", 2) : new String[0];
        System.setProperty("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath());
        if (userInfo.length > 0) {
            System.setProperty("spring.datasource.username", userInfo[0]);
        }
        if (userInfo.length > 1) {
            System.setProperty("spring.datasource.password", userInfo[1]);
        }
    }
}
