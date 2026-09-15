package com.example.bandlink.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds constraints Hibernate's ddl-auto=update cannot express through entity annotations, because
 * there is no Flyway/Liquibase migration path in this project (see application.yaml's
 * jpa.hibernate.ddl-auto). Runs as an ApplicationRunner, same as MasterDataInitializer, so the
 * target table already exists by the time this executes.
 *
 * NFT-003: PostService.create/reopen only check-then-insert (existsByUserIdAndStatusAndType), which
 * is not atomic - two concurrent requests from the same user can both pass the check before either
 * commits, producing two OPEN posts of the same type. A partial unique index (WHERE status='OPEN')
 * is the actual guard; PostService's check stays as a fast-path that avoids a round trip to the DB
 * for the common case, and its saveAndFlush catch converts the loser's constraint violation into the
 * same RuleViolationException message a synchronous duplicate gets.
 */
@Component
public class DatabaseConstraintInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Autowired
    public DatabaseConstraintInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_posts_user_type_open ON posts (user_id, type) WHERE status = 'OPEN'");
    }
}
