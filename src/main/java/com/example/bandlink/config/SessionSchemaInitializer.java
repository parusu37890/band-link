package com.example.bandlink.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Spring Session JDBC's own schema-postgresql.sql (bundled in spring-session-jdbc) uses plain
 * CREATE TABLE with no IF NOT EXISTS, so Spring Boot's spring.session.jdbc.initialize-schema=always
 * would fail on every restart after the first. There is also no Flyway/Liquibase migration path in
 * this project (see DatabaseConstraintInitializer), so this follows the same ApplicationRunner
 * pattern: idempotent DDL, safe to run on every startup. Columns and indexes match Spring Session's
 * official schema exactly so its own JdbcSessionRepository queries work unmodified.
 */
@Component
public class SessionSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Autowired
    public SessionSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS SPRING_SESSION (
                    PRIMARY_ID CHAR(36) NOT NULL,
                    SESSION_ID CHAR(36) NOT NULL,
                    CREATION_TIME BIGINT NOT NULL,
                    LAST_ACCESS_TIME BIGINT NOT NULL,
                    MAX_INACTIVE_INTERVAL INT NOT NULL,
                    EXPIRY_TIME BIGINT NOT NULL,
                    PRINCIPAL_NAME VARCHAR(100),
                    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
                )
                """);
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME)");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
                    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
                    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
                    ATTRIBUTE_BYTES BYTEA NOT NULL,
                    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
                    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
                        REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
                )
                """);
    }
}
