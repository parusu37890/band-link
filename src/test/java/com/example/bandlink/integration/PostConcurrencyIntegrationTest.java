package com.example.bandlink.integration;

import org.junit.jupiter.api.AfterEach;
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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * NFT-003, against the real disposable QA database. Deliberately NOT class-level @Transactional
 * (unlike ReleaseApiIntegrationTest): the two requests below run on their own worker threads, which
 * do not inherit the main test thread's transaction-synchronized connection, so each gets a real,
 * independent DB transaction - the same shape as two browser tabs racing each other. Cleanup is
 * therefore done for real via JdbcTemplate in @BeforeEach/@AfterEach rather than relying on
 * test-transaction rollback.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${QA_RELEASE_JDBC_URL:jdbc:postgresql://localhost:5432/band_link_release_test}",
        "spring.datasource.username=${QA_RELEASE_DB_USERNAME:postgres}",
        "spring.datasource.password=${QA_RELEASE_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "QA_RELEASE_IT", matches = "true")
class PostConcurrencyIntegrationTest {
    private static final String U18 = "qa-release-edit-ready@example.test";
    private static final long U18_ID = 910018L;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private List<Long> seededOpenPostIds;

    @BeforeEach
    void refuseEveryDatabaseExceptDisposableReleaseQaAndStartFromNoOpenPosts() {
        assertEquals("band_link_release_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from qa_release_fixture where singleton=true and suite='Band Link disposable release QA'",
                Integer.class));
        // U18 (910018) owns a seeded OPEN post (920003, MEMBER_WANTED) that it031 elsewhere in this
        // suite also depends on staying OPEN with its seeded ageRanges=ANY. Closing rather than
        // deleting sidesteps the post_age_ranges/post_parts/etc. FK chain entirely, and recording
        // which ids this moved lets @AfterEach put the fixture back exactly as it found it instead
        // of leaking a permanent side effect into every other test in the class.
        seededOpenPostIds = jdbc.queryForList("select id from posts where user_id=? and status='OPEN'", Long.class, U18_ID);
        closeAnyOpenPosts();
    }

    @AfterEach
    void cleanUpWhateverTheRaceCommittedAndRestoreTheSeededFixture() {
        closeAnyOpenPosts();
        for (Long id : seededOpenPostIds) {
            jdbc.update("update posts set status='OPEN', closed_reason=NULL, closed_at=NULL where id=?", id);
        }
    }

    private void closeAnyOpenPosts() {
        jdbc.update("update posts set status='CLOSED', closed_reason='MANUAL', closed_at=now() " +
                "where user_id=? and status='OPEN'", U18_ID);
    }

    @Test
    void nft003_concurrentSameTypeCreatesLeaveExactlyOneOpenPost() throws Exception {
        List<MvcResult> results = fireConcurrently(postBody("MEMBER_WANTED"), postBody("MEMBER_WANTED"));
        long created = results.stream().filter(r -> r.getResponse().getStatus() == 201).count();
        long rejected = results.stream().filter(r -> r.getResponse().getStatus() >= 400 && r.getResponse().getStatus() < 500).count();
        assertEquals(1, created, "exactly one of the two same-type concurrent requests should succeed");
        assertEquals(1, rejected, "the loser must get a 4xx RuleViolation, not a 500");
        assertEquals(1, openCount("MEMBER_WANTED"));
    }

    @Test
    void nft003_concurrentDifferentTypeCreatesBothSucceed() throws Exception {
        List<MvcResult> results = fireConcurrently(postBody("MEMBER_WANTED"), postBody("WANTS_TO_JOIN"));
        long created = results.stream().filter(r -> r.getResponse().getStatus() == 201).count();
        assertEquals(2, created, "different post types must not contend with each other");
        assertEquals(1, openCount("MEMBER_WANTED"));
        assertEquals(1, openCount("WANTS_TO_JOIN"));
    }

    private List<MvcResult> fireConcurrently(String bodyA, String bodyB) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Callable<MvcResult> a = () -> submit(barrier, bodyA);
            Callable<MvcResult> b = () -> submit(barrier, bodyB);
            List<Future<MvcResult>> futures = pool.invokeAll(List.of(a, b));
            return List.of(futures.get(0).get(), futures.get(1).get());
        } finally {
            pool.shutdown();
        }
    }

    private MvcResult submit(CyclicBarrier barrier, String body) throws Exception {
        barrier.await();
        return mvc.perform(post("/api/posts").with(user(U18).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
    }

    private int openCount(String type) {
        return jdbc.queryForObject(
                "select count(*) from posts where user_id=? and type=? and status='OPEN'",
                Integer.class, U18_ID, type);
    }

    private String postBody(String type) {
        return """
                {"type":"%s","title":"QA_RELEASE nft-003","content":"QA_RELEASE concurrent body",
                 "areaSub":"都内","partIds":[%d],"genreIds":[%d],"stanceIds":[%d],
                 "prefectureIds":[%d],"ageRanges":["ANY"],"activityFrequency":"WEEKLY_1"}
                """.formatted(type, id("parts", "リードギター"), id("genres", "ポップス"),
                id("stances", "趣味で楽しみたい"), id("prefectures", "東京都"));
    }

    private long id(String table, String name) {
        return jdbc.queryForObject("select id from " + table + " where name=?", Long.class, name);
    }
}
