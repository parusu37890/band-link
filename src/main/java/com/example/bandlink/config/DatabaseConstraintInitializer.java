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
 *
 * NFT-004: PostImageService.add/addAll have the exact same class of bug - countByPostId(postId) >= 3
 * is checked, then a row is inserted, with no atomicity between the two. Two concurrent uploads to
 * the same post can both pass the count check before either commits, producing 4+ images. A "count
 * must stay <= 3" rule cannot be expressed as a plain UNIQUE/CHECK constraint (CHECK cannot see other
 * rows), so this uses a BEFORE INSERT trigger instead: it takes FOR UPDATE lock on the parent posts
 * row first, which serializes concurrent inserts for the same post_id against each other, then counts
 * committed sibling rows and rejects the insert once 3 already exist. PostImageService's own count
 * check stays as the fast path; its saveAndFlush catch both converts the loser's rejection into the
 * same RuleViolationException message the synchronous over-3 case gets, and deletes the file it had
 * already written to disk so a rejected insert never leaves an orphan upload behind.
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
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION enforce_post_images_limit() RETURNS trigger AS $$
                DECLARE
                    current_count integer;
                BEGIN
                    -- Lock the parent post row so a concurrent insert for the same post_id
                    -- serializes behind this one instead of racing the COUNT(*) below (NFT-004).
                    PERFORM 1 FROM posts WHERE id = NEW.post_id FOR UPDATE;
                    SELECT count(*) INTO current_count FROM post_images WHERE post_id = NEW.post_id;
                    IF current_count >= 3 THEN
                        RAISE EXCEPTION 'post_images_limit_exceeded (post_id=%)', NEW.post_id
                            USING ERRCODE = '23514';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbc.execute("""
                CREATE OR REPLACE TRIGGER trg_post_images_limit
                    BEFORE INSERT ON post_images
                    FOR EACH ROW EXECUTE FUNCTION enforce_post_images_limit()
                """);
    }
}
