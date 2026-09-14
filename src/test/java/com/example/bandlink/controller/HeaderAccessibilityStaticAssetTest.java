package com.example.bandlink.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PW-I / NFT-010: the header's notification bell shows unread state as a colour-only dot
 * (`[data-unread-dot]`, plain CSS, no text). A live accessibility-snapshot check during PW-I found
 * that the bell link's accessible name stayed a static "通知" even with unread notifications
 * present, so a screen-reader user got no cue at all that anything was new - only sighted users
 * saw the dot. Fixed in app.js's header() by also updating the link's aria-label (plain "通知" vs
 * "通知（未読N件）") whenever the unread count is fetched, alongside the dot's hidden state.
 *
 * There is no JS unit-test harness in this project, so this pins the fix at the level that does
 * exist: the actual bytes served at /js/app.js must keep both the marker attribute the fix relies
 * on and the aria-label update call, so a future edit can't silently drop it again.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HeaderAccessibilityStaticAssetTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notificationBellLinkCarriesAMarkerAndUpdatesItsAccessibleNameWithUnreadCount() throws Exception {
        String source = mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(source.contains("data-notif-link"),
                "the notification link must carry a stable marker so header() can update its aria-label");
        assertTrue(source.contains("aria-label=\"通知\""),
                "the link must still have a plain, correct default accessible name before the unread count loads");
        assertTrue(source.contains("link.setAttribute('aria-label'"),
                "header() must update the link's accessible name, not just the visual dot, when unread state changes");
        assertTrue(source.contains("未読${count}件"),
                "the accessible name must actually say how many notifications are unread, matching the visual dot cue");
    }
}
