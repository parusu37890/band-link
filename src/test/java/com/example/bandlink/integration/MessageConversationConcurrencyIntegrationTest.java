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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * NFT-005, against the real disposable QA database. Deliberately NOT class-level @Transactional
 * (same reasoning as PostConcurrencyIntegrationTest / PostImageConcurrencyIntegrationTest): the two
 * sends below run on their own worker threads with their own independent DB transactions, the same
 * shape as two people opening a DM with each other for the first time at the same instant.
 *
 * The seed fixture already gives U07/U08 a conversation (930001, with messages 940001..940003 and
 * notifications 950001/950002), so this test removes that row first to reproduce the "first DM"
 * precondition NFT-005 actually calls for, then restores the exact original rows afterward via
 * JdbcTemplate rather than relying on test-transaction rollback.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${QA_RELEASE_JDBC_URL:jdbc:postgresql://localhost:5432/band_link_release_test}",
        "spring.datasource.username=${QA_RELEASE_DB_USERNAME:postgres}",
        "spring.datasource.password=${QA_RELEASE_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "QA_RELEASE_IT", matches = "true")
class MessageConversationConcurrencyIntegrationTest {
    private static final String U07 = "qa-release-sender@example.test";
    private static final String U08 = "qa-release-receiver@example.test";
    private static final long U07_ID = 910007L;
    private static final long U08_ID = 910008L;
    private static final long SEEDED_CONVERSATION_ID = 930001L;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private Map<String, Object> seededConversation;
    private List<Map<String, Object>> seededMessages;
    private List<Map<String, Object>> seededNotifications;

    @BeforeEach
    void removeTheSeededConversationSoThisIsGenuinelyAFirstDm() {
        assertEquals("band_link_release_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from qa_release_fixture where singleton=true and suite='Band Link disposable release QA'",
                Integer.class));
        seededConversation = jdbc.queryForMap("select * from conversations where id=?", SEEDED_CONVERSATION_ID);
        seededMessages = jdbc.queryForList("select * from messages where conversation_id=? order by id", SEEDED_CONVERSATION_ID);
        seededNotifications = jdbc.queryForList("select * from notifications where related_id=?", SEEDED_CONVERSATION_ID);

        jdbc.update("delete from notifications where related_id=?", SEEDED_CONVERSATION_ID);
        jdbc.update("delete from messages where conversation_id=?", SEEDED_CONVERSATION_ID);
        jdbc.update("delete from conversations where id=?", SEEDED_CONVERSATION_ID);
        assertEquals(0, pairConversationCount(), "precondition: U07/U08 must have no conversation before the race");
    }

    @AfterEach
    void deleteWhateverTheRaceCreatedAndRestoreTheSeededConversation() {
        for (Long id : jdbc.queryForList(
                "select id from conversations where (user_a_id=? and user_b_id=?) or (user_a_id=? and user_b_id=?)",
                Long.class, U07_ID, U08_ID, U08_ID, U07_ID)) {
            jdbc.update("delete from notifications where related_id=?", id);
            jdbc.update("delete from messages where conversation_id=?", id);
            jdbc.update("delete from conversations where id=?", id);
        }
        jdbc.update("insert into conversations(id,user_a_id,user_b_id,created_at,last_message_at) values (?,?,?,?,?)",
                seededConversation.get("id"), seededConversation.get("user_a_id"), seededConversation.get("user_b_id"),
                seededConversation.get("created_at"), seededConversation.get("last_message_at"));
        for (Map<String, Object> m : seededMessages) {
            jdbc.update("insert into messages(id,conversation_id,sender_id,content,image_url,created_at,read_at) "
                            + "values (?,?,?,?,?,?,?)",
                    m.get("id"), m.get("conversation_id"), m.get("sender_id"), m.get("content"), m.get("image_url"),
                    m.get("created_at"), m.get("read_at"));
        }
        for (Map<String, Object> n : seededNotifications) {
            jdbc.update("insert into notifications(id,user_id,type,content,related_id,created_at,read_at) "
                            + "values (?,?,?,?,?,?,?)",
                    n.get("id"), n.get("user_id"), n.get("type"), n.get("content"), n.get("related_id"),
                    n.get("created_at"), n.get("read_at"));
        }
    }

    @Test
    void nft005_concurrentFirstDmProducesExactlyOneConversationAndTwoMessagesWithNo5xx() throws Exception {
        List<MvcResult> results = fireConcurrently();
        for (MvcResult r : results) {
            int status = r.getResponse().getStatus();
            assertTrue(status >= 200 && status < 300,
                    "both sides of a first-DM race must succeed, got " + status + ": " + r.getResponse().getContentAsString());
        }

        assertEquals(1, pairConversationCount(), "exactly one conversation must exist for the pair after the race");
        Long conversationId = jdbc.queryForObject(
                "select id from conversations where (user_a_id=? and user_b_id=?) or (user_a_id=? and user_b_id=?)",
                Long.class, U07_ID, U08_ID, U08_ID, U07_ID);
        int messageCount = jdbc.queryForObject("select count(*) from messages where conversation_id=?", Integer.class, conversationId);
        assertEquals(2, messageCount, "both concurrent sends must land in the same conversation");
    }

    private long pairConversationCount() {
        return jdbc.queryForObject(
                "select count(*) from conversations where (user_a_id=? and user_b_id=?) or (user_a_id=? and user_b_id=?)",
                Long.class, U07_ID, U08_ID, U08_ID, U07_ID);
    }

    private List<MvcResult> fireConcurrently() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Callable<MvcResult> a = () -> send(barrier, U07, U08_ID, "QA_RELEASE nft-005 race-A");
            Callable<MvcResult> b = () -> send(barrier, U08, U07_ID, "QA_RELEASE nft-005 race-B");
            List<Future<MvcResult>> futures = pool.invokeAll(List.of(a, b));
            return List.of(futures.get(0).get(), futures.get(1).get());
        } finally {
            pool.shutdown();
        }
    }

    private MvcResult send(CyclicBarrier barrier, String senderEmail, long recipientId, String content) throws Exception {
        String body = "{\"content\":\"%s\"}".formatted(content);
        barrier.await();
        return mvc.perform(post("/api/messages").param("recipientId", String.valueOf(recipientId))
                        .with(user(senderEmail).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
    }
}
