package com.taskmanagement.secure.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationTest {

    @Test
    @DisplayName("Should successfully parse and execute Flyway migrations V1 and V2")
    void testFlywayMigrations() {
        DataSource dataSource = DataSourceBuilder.create()
                .url("jdbc:h2:mem:flyway_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();

        assertDoesNotThrow(() -> {
            int migrationsExecuted = flyway.migrate().migrationsExecuted;
            assertTrue(migrationsExecuted >= 2, "Expected at least 2 migrations to be applied");
        });
    }
}
