package com.example.bandlink.controller;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.example.bandlink.repository.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PW-G / ST-055 (all-route background sweep): a signed-in person following a stale bookmark or a
 * typo'd URL used to fall straight through PageController's explicit route allowlist into Spring
 * Boot's raw, unstyled "Whitelabel Error Page" - no Band Link header/footer, no --bg canvas at
 * all. The same thing happened to a signed-in non-admin visiting /admin: SecurityConfig's
 * accessDeniedHandler always wrote the API's raw {"code":"FORBIDDEN",...} JSON, even for a full
 * page navigation, so the browser printed that JSON as plain text on a blank white page. Both are
 * fixed (templates/error.html for the first, a non-/api branch in accessDeniedHandler for the
 * second); these pin the fix and guard the still-JSON API behaviour it must not change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PageRoutingErrorHandlingTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private MockHttpSession verifiedUserSession() throws Exception {
        String email = "st-routing-" + UUID.randomUUID() + "@bandlink.local";
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/register").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ルーティング検証\",\"email\":\"" + email + "\",\"password\":\"stpass1234\","
                                + "\"age\":28,\"experienceYears\":5,\"gender\":\"男性\",\"partIds\":[1],\"genreIds\":[1],"
                                + "\"stanceIds\":[1],\"prefectureIds\":[1]}"))
                .andExpect(status().isCreated());
        var user = users.findByEmail(email).orElseThrow();
        user.setEmailVerifiedAt(LocalDateTime.now());
        users.save(user);
        return session;
    }

    @Test
    void anUnmappedPageUrlStaysA404WithoutThrowing() throws Exception {
        // MockMvc's dispatch does not follow through to a real container's error-page rendering
        // (DefaultErrorViewResolver never runs here, so it cannot confirm templates/error.html is
        // what actually renders): the response body is empty either way. What it can confirm is
        // that the request still resolves to a plain 404 with no server-side exception - the fix
        // itself (real body is the app shell, not the raw whitelabel page, status stays 404) was
        // verified against the running app: `curl` with an authenticated session and a full
        // Playwright MCP browser round-trip both showed the app shell loading and the client
        // router's own not-found fallback (redirect to /posts) taking over from there.
        mvc.perform(get("/this-page-does-not-exist-anywhere").session(verifiedUserSession())
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());
    }

    @Test
    void aNonAdminVisitingTheAdminPageIsSentSomewhereRealNotShownRawJson() throws Exception {
        mvc.perform(get("/admin").session(verifiedUserSession()).accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
    }

    @Test
    void adminApiStillReturnsJsonForbiddenForANonAdmin() throws Exception {
        mvc.perform(get("/api/admin/reports").session(verifiedUserSession()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
