package com.example.bandlink.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Deletes the data owned by a user who chose to withdraw.  A voluntary
 * withdrawal is different from an operator suspension: it removes the
 * account, its conversations and its posts so the email address can be used
 * for a new registration.
 */
@Service
public class AccountDeletionService {
    private final JdbcTemplate jdbc;
    private final ImageStorageService images;

    public AccountDeletionService(JdbcTemplate jdbc, ImageStorageService images) {
        this.jdbc = jdbc;
        this.images = images;
    }

    @Transactional
    public void deleteUserData(Long userId) {
        List<String> postImages = jdbc.queryForList(
                "select pi.image_url from post_images pi join posts p on p.id = pi.post_id where p.user_id = ?",
                String.class, userId);
        List<String> messageImages = jdbc.queryForList(
                "select m.image_url from messages m where m.sender_id = ? or m.conversation_id in "
                        + "(select c.id from conversations c where c.user_a_id = ? or c.user_b_id = ?)",
                String.class, userId, userId, userId);
        List<String> profileImages = jdbc.queryForList(
                "select profile_image_url from users where id = ? and profile_image_url is not null",
                String.class, userId);

        // Reports that point at this account's content no longer have a valid target.
        jdbc.update("delete from reports where reporter_id = ? "
                + "or (target_type = 'POST' and target_id in (select id from posts where user_id = ?)) "
                + "or (target_type = 'MESSAGE' and target_id in (select m.id from messages m where "
                + "m.sender_id = ? or m.conversation_id in (select c.id from conversations c "
                + "where c.user_a_id = ? or c.user_b_id = ?)))",
                userId, userId, userId, userId, userId);

        jdbc.update("delete from messages where sender_id = ? or conversation_id in "
                + "(select c.id from conversations c where c.user_a_id = ? or c.user_b_id = ?)",
                userId, userId, userId);
        jdbc.update("delete from conversations where user_a_id = ? or user_b_id = ?", userId, userId);
        jdbc.update("delete from notifications where user_id = ?", userId);
        jdbc.update("delete from blocks where blocker_id = ? or blocked_id = ?", userId, userId);
        jdbc.update("delete from search_histories where user_id = ?", userId);
        jdbc.update("delete from email_verification_tokens where user_id = ?", userId);
        jdbc.update("delete from password_reset_tokens where user_id = ?", userId);
        jdbc.update("delete from line_accounts where user_id = ?", userId);

        jdbc.update("delete from post_images where post_id in (select id from posts where user_id = ?)", userId);
        jdbc.update("delete from post_age_ranges where post_id in (select id from posts where user_id = ?)", userId);
        jdbc.update("delete from post_parts where post_id in (select id from posts where user_id = ?)", userId);
        jdbc.update("delete from post_genres where post_id in (select id from posts where user_id = ?)", userId);
        jdbc.update("delete from post_stances where post_id in (select id from posts where user_id = ?)", userId);
        jdbc.update("delete from post_prefectures where post_id in (select id from posts where user_id = ?)", userId);
        jdbc.update("delete from posts where user_id = ?", userId);

        jdbc.update("delete from user_parts where user_id = ?", userId);
        jdbc.update("delete from user_genres where user_id = ?", userId);
        jdbc.update("delete from user_stances where user_id = ?", userId);
        jdbc.update("delete from user_prefectures where user_id = ?", userId);
        jdbc.update("delete from users where id = ?", userId);

        postImages.forEach(images::delete);
        messageImages.forEach(images::delete);
        profileImages.forEach(images::delete);
    }
}
