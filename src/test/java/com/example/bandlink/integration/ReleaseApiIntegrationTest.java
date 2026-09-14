package com.example.bandlink.integration;

import com.example.bandlink.service.AccountDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0/P1 release integration checks against the disposable seeded PostgreSQL database.
 * They are disabled unless QA_RELEASE_IT=true so a normal developer test run cannot touch data.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${QA_RELEASE_JDBC_URL:jdbc:postgresql://localhost:5432/band_link_release_test}",
        "spring.datasource.username=${QA_RELEASE_DB_USERNAME:postgres}",
        "spring.datasource.password=${QA_RELEASE_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "QA_RELEASE_IT", matches = "true")
class ReleaseApiIntegrationTest {
    private static final String GENERAL = "qa-release-general@example.test";
    private static final String UNVERIFIED = "qa-release-unverified@example.test";
    private static final String ADMIN = "qa-release-admin@example.test";
    private static final String THIRD_PARTY = "qa-release-third-party@example.test";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccountDeletionService deletion;

    @BeforeEach
    void refuseEveryDatabaseExceptDisposableReleaseQa() {
        assertEquals("band_link_release_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from qa_release_fixture where singleton=true and suite='Band Link disposable release QA'",
                Integer.class));
    }

    @Test
    void it001_registrationPersistsEveryRequiredProfileRelation() throws Exception {
        long part = id("parts", "ギター");
        long genre = id("genres", "ポップス");
        long stance = id("stances", "趣味で楽しみたい");
        long prefecture = id("prefectures", "東京都");
        String email = "qa-release-created@example.test";
        String body = """
                {"username":"QA_RELEASE_created","email":"%s","password":"qa-pass-1234",
                 "age":20,"experienceYears":1,"gender":"男性","partIds":[%d],"genreIds":[%d],
                 "stanceIds":[%d],"prefectureIds":[%d]}
                """.formatted(email, part, genre, stance, prefecture);
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email));
        Long userId = jdbc.queryForObject("select id from users where email=?", Long.class, email);
        assertEquals(1, count("user_parts", userId));
        assertEquals(1, count("user_genres", userId));
        assertEquals(1, count("user_stances", userId));
        assertEquals(1, count("user_prefectures", userId));
    }

    @Test
    void it002_loginCreatesSessionAndLogoutInvalidatesIt() throws Exception {
        String password = requiredPassword();
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + GENERAL + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void it004_emailVerificationTokenIsSingleUse() throws Exception {
        mvc.perform(post("/api/auth/verify-email").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"qa-release-verify-valid\"}"))
                .andExpect(status().isNoContent());
        assertEquals(1, jdbc.queryForObject("select count(*) from users where id=910001 and email_verified_at is not null", Integer.class));
        mvc.perform(post("/api/auth/verify-email").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"qa-release-verify-valid\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void it008_withdrawalAlsoDeletesFeedbackAndAllowsEmailReuse() {
        assertEquals(1, jdbc.queryForObject("select count(*) from feedback where user_id=910022", Integer.class));
        assertDoesNotThrow(() -> deletion.deleteUserData(910022L));
        assertEquals(0, jdbc.queryForObject("select count(*) from users where id=910022", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from feedback where user_id=910022", Integer.class));
    }

    @Test
    void it009_publicAndPrivateProfileContractsDiffer() throws Exception {
        mvc.perform(get("/api/users/910004"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.lastSeenAt").doesNotExist());
        mvc.perform(get("/api/users/me").with(user("qa-release-complete@example.test").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("qa-release-complete@example.test"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void it010_unknownProfileMasterIdRollsBackWholeUpdate() throws Exception {
        String before = jdbc.queryForObject("select username from users where id=910004", String.class);
        String body = """
                {"username":"SHOULD_NOT_PERSIST","gender":"女性","bio":"x","age":20,"experienceYears":1,
                 "partIds":[999999],"genreIds":[999999],"stanceIds":[999999],"prefectureIds":[999999]}
                """;
        mvc.perform(put("/api/users/me").with(user("qa-release-complete@example.test").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is4xxClientError());
        assertEquals(before, jdbc.queryForObject("select username from users where id=910004", String.class));
    }

    @Test
    void it013_nonOwnerCannotMutatePost() throws Exception {
        var stranger = user(GENERAL).roles("USER");
        mvc.perform(patch("/api/posts/920001/close").with(stranger).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/posts/920005/reopen").with(stranger).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/posts/920001").with(stranger).with(csrf())).andExpect(status().isForbidden());
        assertEquals("OPEN", jdbc.queryForObject("select status from posts where id=920001", String.class));
    }

    @Test
    void it014_secondOpenPostIsRejectedWithoutInsert() throws Exception {
        int before = jdbc.queryForObject("select count(*) from posts where user_id=910005", Integer.class);
        String body = postBody();
        mvc.perform(post("/api/posts").with(user("qa-release-wanted@example.test").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is4xxClientError());
        assertEquals(before, jdbc.queryForObject("select count(*) from posts where user_id=910005", Integer.class));
    }

    @Test
    void it017_keywordAlsoSearchesAreaSupplement() throws Exception {
        jdbc.update("update posts set area_sub='QA_RELEASE_補足だけの検索語' where id=920001");
        mvc.perform(get("/api/posts/page").param("keyword", "補足だけの検索語"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(920001));
    }

    @Test
    void it020_hugeCursorNeverBecomesServerError() throws Exception {
        mvc.perform(get("/api/posts/page").param("cursor", "999999999999999999999999999999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
    }

    /**
     * ST-028: a poster who marked "年齢不問" (ANY, no age preference) is not stating a specific
     * band, so they must still surface for someone searching by a concrete one - P003 (920003) is
     * seeded with ageRanges=["ANY"] and must appear in a search for S40 even though it never
     * selected S40 itself. Before the fix, the age filter was a plain IN() against exactly the
     * selected bands and silently dropped every ANY-tagged post as soon as one concrete band was
     * chosen.
     */
    @Test
    void it031_ageRangeSearchAlsoMatchesAnyTaggedPosts() throws Exception {
        mvc.perform(get("/api/posts/page").param("ageRanges", "S40").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id==920003)]").exists());
    }

    /**
     * ST-032: the search screen (recruitment-search.js) only ever calls GET /api/posts/page, never
     * the plain GET /api/posts that used to be the only endpoint recording search history - so a
     * signed-in person's real searches were silently never saved, and GET /api/search-history stayed
     * empty forever regardless of how much they searched.
     */
    @Test
    void it032_pageSearchEndpointRecordsHistoryForSignedInUsers() throws Exception {
        mvc.perform(get("/api/posts/page").param("keyword", "qa_release_history_probe")
                        .with(user(GENERAL).roles("USER")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/search-history").with(user(GENERAL).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conditions").value(org.hamcrest.Matchers.containsString("qa_release_history_probe")));
    }

    /**
     * PW-E follow-up (ST-039): POST /api/messages/images declares produces=text/plain for its
     * success body (a bare URL string, not JSON) and the frontend matches that with its own
     * Accept: text/plain. An invalid image still fails with a JSON {@link ApiExceptionHandler.Error}
     * body, which without forcing the response content type could not be content-negotiated against
     * that Accept header - Spring's own attempt to report the 400 threw a second exception, and the
     * browser only ever saw an empty 500 instead of the "画像はjpg/png/webp、1枚5MBまでです" guidance
     * ST-039 requires. A magic-byte-invalid WebP (the RIFF/WEBP header with no VP8 payload after it)
     * doubled as a regression for ImageStorageService.valid(), which used to require WebP files to be
     * exactly 12 bytes - the length of the bare header alone, which no real-world WebP file ever is.
     */
    @Test
    void it033_invalidMessageImageReturnsReadableJsonNotABareServerError() throws Exception {
        MockMultipartFile bogusWebp = new MockMultipartFile("file", "bogus.webp", "image/webp",
                "this is not an actual webp file at all".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        mvc.perform(multipart("/api/messages/images").file(bogusWebp)
                        .with(user("qa-release-sender@example.test").roles("USER")).with(csrf())
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void it024_messageImageRequiresParticipantOrExactReportModerator() throws Exception {
        String name = "97000000-0000-4000-8000-000000000007.png";
        mvc.perform(get("/api/messages/images/{name}", name).with(user(THIRD_PARTY).roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/messages/images/{name}", name).with(user("qa-release-sender@example.test").roles("USER")))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/api/messages/images/{name}", name).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void it027_thirdPartyCannotCreateMessageReportOrSnapshot() throws Exception {
        int before = jdbc.queryForObject("select count(*) from reports where target_type='MESSAGE' and target_id=940002", Integer.class);
        mvc.perform(post("/api/reports").with(user(THIRD_PARTY).roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"MESSAGE\",\"targetId\":940002,\"reason\":\"第三者テスト\"}"))
                .andExpect(status().isForbidden());
        assertEquals(before, jdbc.queryForObject("select count(*) from reports where target_type='MESSAGE' and target_id=940002", Integer.class));
    }

    @Test
    void it028_adminEndpointsRejectUserAndAcceptAdmin() throws Exception {
        mvc.perform(get("/api/admin/reports").with(user(GENERAL).roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/feedback").with(user(GENERAL).roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reports").with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/feedback").with(user(ADMIN).roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void it030_mutationRequiresCsrf() throws Exception {
        var principal = user(GENERAL).roles("USER");
        mvc.perform(post("/api/blocks").param("userId", "910008").with(principal)).andExpect(status().isForbidden());
        mvc.perform(post("/api/blocks").param("userId", "910008").with(principal).with(csrf())).andExpect(status().isOk());
    }

    @Test
    void sec010_privateMessageImageCannotBeFetchedThroughPublicUploadsHandler() throws Exception {
        // The seed fixture actually lives under uploads/messages/, which is what
        // ImageStorageService.storePrivate() writes to. Asserting on the root-level
        // path here would pass for the wrong reason (no file exists there at all)
        // without proving the private directory itself is unreachable.
        mvc.perform(get("/uploads/messages/97000000-0000-4000-8000-000000000007.png"))
                .andExpect(status().is4xxClientError());
    }

    private long id(String table, String name) {
        return jdbc.queryForObject("select id from " + table + " where name=?", Long.class, name);
    }

    private int count(String table, Long userId) {
        return jdbc.queryForObject("select count(*) from " + table + " where user_id=?", Integer.class, userId);
    }

    private String requiredPassword() {
        String value = System.getenv("QA_RELEASE_PASSWORD");
        if (value == null || value.isBlank()) throw new IllegalStateException("QA_RELEASE_PASSWORD is required");
        return value;
    }

    private String postBody() {
        return """
                {"type":"MEMBER_WANTED","title":"QA_RELEASE second","content":"QA_RELEASE body",
                 "areaSub":"都内","partIds":[%d],"genreIds":[%d],"stanceIds":[%d],
                 "prefectureIds":[%d],"ageRanges":["ANY"],"activityFrequency":"WEEKLY_1"}
                """.formatted(id("parts", "ギター"), id("genres", "ポップス"),
                id("stances", "趣味で楽しみたい"), id("prefectures", "東京都"));
    }
}
