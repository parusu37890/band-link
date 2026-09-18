package com.example.bandlink.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * NFT-013, against the real disposable QA database. Part A attempts each listed DB-level violation
 * directly over JDBC, in its own transaction, expecting Postgres to reject it and leave existing rows
 * untouched; each connection is rolled back, not committed, so these tests are non-destructive by
 * construction and need no fixture restore. Part B attempts the same class of violation through the
 * real API - including two genuine concurrency races - and checks the caller only ever sees a 4xx
 * with a safe, structured message, never a raw 5xx (the ApiExceptionHandler.dataIntegrity backstop
 * this case exists to prove out).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${QA_RELEASE_JDBC_URL:jdbc:postgresql://localhost:5432/band_link_release_test}",
        "spring.datasource.username=${QA_RELEASE_DB_USERNAME:postgres}",
        "spring.datasource.password=${QA_RELEASE_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "QA_RELEASE_IT", matches = "true")
class DatabaseConstraintDefenseIntegrationTest {
    private static final long U07_ID = 910007L;
    private static final long U11_ID = 910011L;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @BeforeEach
    void guardDisposableReleaseQaDatabase() {
        assertEquals("band_link_release_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from qa_release_fixture where singleton=true and suite='Band Link disposable release QA'",
                Integer.class));
    }

    // ---------------------------------------------------------------------------------------------
    // Part A: direct DB-level violations, each in its own transaction, rolled back, not committed.
    // ---------------------------------------------------------------------------------------------

    @Test
    void nft013a_duplicateUserEmailIsRejectedByUniqueConstraint() throws SQLException {
        expectRejection("users",
                "insert into users(username,email,password_hash,status,role,created_at) values (?,?,?,?,?,now())",
                new Object[]{"QA_RELEASE_nft013_dup", "qa-release-sender@example.test", "hash", "ACTIVE", "USER"},
                "23505");
    }

    @Test
    void nft013a_duplicateConversationPairIsRejectedByUniqueConstraint() throws SQLException {
        // Mirrors the already-seeded C001 (930001, U07/U08) exactly - same ordered pair.
        expectRejection("conversations",
                "insert into conversations(user_a_id,user_b_id,created_at,last_message_at) values (?,?,now(),now())",
                new Object[]{U07_ID, 910008L},
                "23505");
    }

    @Test
    void nft013a_duplicateBlockPairIsRejectedByUniqueConstraint() throws SQLException {
        // Mirrors the already-seeded block row (960001, blocker 910009 -> blocked 910010).
        expectRejection("blocks",
                "insert into blocks(blocker_id,blocked_id,created_at) values (?,?,now())",
                new Object[]{910009L, 910010L},
                "23505");
    }

    @Test
    void nft013a_orphanForeignKeyIsRejected() throws SQLException {
        expectRejection("posts",
                "insert into posts(user_id,type,title,content,area_sub,activity_frequency,status,"
                        + "created_at,updated_at,expires_at,rank_updated_at) "
                        + "values (?, 'MEMBER_WANTED','QA_RELEASE orphan','QA_RELEASE orphan body','都内',"
                        + "'WEEKLY_1','OPEN', now(), now(), now()+interval '30 days', now())",
                new Object[]{999999999L},
                "23503");
    }

    @Test
    void nft013a_unknownEnumValueIsRejectedByCheckConstraint() throws SQLException {
        expectRejection("posts",
                "insert into posts(user_id,type,title,content,area_sub,activity_frequency,status,"
                        + "created_at,updated_at,expires_at,rank_updated_at) "
                        + "values (?, 'NOT_A_REAL_TYPE','QA_RELEASE bogus enum','QA_RELEASE bogus body','都内',"
                        + "'WEEKLY_1','OPEN', now(), now(), now()+interval '30 days', now())",
                new Object[]{U07_ID},
                "23514");
    }

    @Test
    void nft013a_requiredNullIsRejectedByNotNullConstraint() throws SQLException {
        expectRejection("users",
                "insert into users(username,email,password_hash,status,role,created_at) values (?,?,?,?,?,now())",
                new Object[]{"QA_RELEASE_nft013_null", null, "hash", "ACTIVE", "USER"},
                "23502");
    }

    /** Attempts sql/params on its own connection/transaction, expects SQLState, then rolls back. */
    private void expectRejection(String table, String sql, Object[] params, String expectedSqlState) throws SQLException {
        long before = countRows(table);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
                SQLException thrown = assertThrows(SQLException.class, statement::executeUpdate);
                assertEquals(expectedSqlState, thrown.getSQLState(),
                        "unexpected SQLSTATE for rejected " + table + " insert: " + thrown.getMessage());
            } finally {
                connection.rollback();
            }
        }
        assertEquals(before, countRows(table), table + " row count must be unchanged after the rejected+rolled-back insert");
    }

    private long countRows(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Part B: the same classes of violation, attempted through the real API.
    // ---------------------------------------------------------------------------------------------

    @Test
    void nft013b_duplicateEmailRegistrationReturns4xxWithAStructuredBodyNotA500() throws Exception {
        long before = countRows("users");
        String body = registerBody("QA_RELEASE_nft013_dup_api", "qa-release-sender@example.test");
        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        int status = result.getResponse().getStatus();
        assertTrue(status >= 400 && status < 500, "duplicate email registration must be 4xx, got " + status);
        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"code\""), "error body must be the structured Error(code,message) shape: " + responseBody);
        assertFalse(responseBody.toLowerCase(java.util.Locale.ROOT).contains("exception"),
                "error body must not leak an exception class name: " + responseBody);
        assertEquals(before, countRows("users"), "a rejected duplicate registration must not create a row");
    }

    @Test
    void nft013b_concurrentDuplicateRegistrationNeverSurfacesA500() throws Exception {
        String email = "qa-release-nft013-concurrent@example.test";
        long before = countRows("users");
        try {
            List<MvcResult> results = fireConcurrently(
                    () -> registerAttempt(email, "QA_RELEASE_nft013_race_a"),
                    () -> registerAttempt(email, "QA_RELEASE_nft013_race_b"));
            long created = results.stream().filter(r -> r.getResponse().getStatus() == 201).count();
            long rejected = results.stream().filter(r -> r.getResponse().getStatus() >= 400 && r.getResponse().getStatus() < 500).count();
            long serverErrors = results.stream().filter(r -> r.getResponse().getStatus() >= 500).count();
            assertEquals(0, serverErrors, "neither concurrent registration with the same email may surface a raw 500");
            assertEquals(1, created, "exactly one of the two concurrent same-email registrations should succeed");
            assertEquals(1, rejected);
            assertEquals(before + 1, countRows("users"), "exactly one new user row, not zero and not two");
        } finally {
            Long userId = jdbc.query("select id from users where email=?", (rs, i) -> rs.getLong("id"), email)
                    .stream().findFirst().orElse(null);
            if (userId != null) {
                jdbc.update("delete from email_verification_tokens where user_id=?", userId);
                jdbc.update("delete from user_parts where user_id=?", userId);
                jdbc.update("delete from user_genres where user_id=?", userId);
                jdbc.update("delete from user_stances where user_id=?", userId);
                jdbc.update("delete from user_prefectures where user_id=?", userId);
                jdbc.update("delete from users where id=?", userId);
            }
        }
    }

    @Test
    void nft013b_concurrentDuplicateBlockNeverSurfacesA500AndStaysIdempotent() throws Exception {
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from blocks where blocker_id=? and blocked_id=?", Integer.class, U07_ID, U11_ID),
                "precondition: U07 must not already block U11");
        try {
            List<MvcResult> results = fireConcurrently(() -> blockAttempt(), () -> blockAttempt());
            long serverErrors = results.stream().filter(r -> r.getResponse().getStatus() >= 500).count();
            assertEquals(0, serverErrors, "neither concurrent block request for the same pair may surface a raw 500");
            for (MvcResult r : results) {
                int status = r.getResponse().getStatus();
                assertTrue(status < 300 || (status >= 400 && status < 500),
                        "each request must be a success or a clean 4xx, got " + status);
            }
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from blocks where blocker_id=? and blocked_id=?", Integer.class, U07_ID, U11_ID),
                    "the pair must be blocked exactly once, not duplicated and not left unblocked");
        } finally {
            jdbc.update("delete from blocks where blocker_id=? and blocked_id=?", U07_ID, U11_ID);
        }
    }

    private MvcResult registerAttempt(String email, String username) throws Exception {
        return mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody(username, email)))
                .andReturn();
    }

    private String registerBody(String username, String email) {
        return """
                {"username":"%s","email":"%s","password":"qa-pass-1234",
                 "age":20,"experienceYears":1,"gender":"男性","partIds":[%d],"genreIds":[%d],
                 "stanceIds":[%d],"prefectureIds":[%d]}
                """.formatted(username, email, id("parts", "リードギター"), id("genres", "ポップス"),
                id("stances", "趣味で楽しみたい"), id("prefectures", "東京都"));
    }

    private long id(String table, String name) {
        return jdbc.queryForObject("select id from " + table + " where name=?", Long.class, name);
    }

    private MvcResult blockAttempt() throws Exception {
        return mvc.perform(post("/api/blocks").param("userId", String.valueOf(U11_ID))
                        .with(user("qa-release-sender@example.test").roles("USER")).with(csrf()))
                .andReturn();
    }

    @FunctionalInterface
    private interface Attempt { MvcResult run() throws Exception; }

    private List<MvcResult> fireConcurrently(Attempt a, Attempt b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Callable<MvcResult> ca = () -> { barrier.await(); return a.run(); };
            Callable<MvcResult> cb = () -> { barrier.await(); return b.run(); };
            List<Future<MvcResult>> futures = pool.invokeAll(List.of(ca, cb));
            return List.of(futures.get(0).get(), futures.get(1).get());
        } finally {
            pool.shutdown();
        }
    }
}
