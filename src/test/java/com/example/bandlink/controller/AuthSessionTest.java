package com.example.bandlink.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ST on 2026-09-06 registered an account through the browser and landed on the board signed out:
 * /api/auth/register returned the new user but never established a session, while the client set
 * state.user from that response and navigated as if it had. These pin both entry points to the
 * session they are supposed to create.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSessionTest {

    @Autowired
    private MockMvc mvc;

    private String uniqueEmail() {
        return "st-session-" + UUID.randomUUID() + "@bandlink.local";
    }

    private String body(String email) {
        return "{\"username\":\"セッション検証\",\"email\":\"" + email + "\",\"password\":\"stpass1234\"}";
    }

    @Test
    void registeringSignsThePersonIn() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/api/auth/register").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void loggingInEstablishesItsOwnSession() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").session(new MockHttpSession()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(email)))
                .andExpect(status().isCreated());

        MockHttpSession fresh = new MockHttpSession();
        mvc.perform(get("/api/auth/me").session(fresh)).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").session(fresh).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"stpass1234\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").session(fresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }
}
