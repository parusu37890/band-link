package com.example.bandlink.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * requirements 7章「認証・復旧用メールは提供する」。トークンを作るだけで送っていなかったので、
 * 利用者は確認も再設定も自力では終えられなかった。
 *
 * <p>送信先が設定されていないとき（`spring.mail.host` が空）は送らずに警告だけ残す。ローカルでは
 * DBからトークンを読む前提で、requirements 13.3 に従い本文にもログにもトークンを書かない。
 * 送信の失敗で登録や再設定そのものを落とさない — 相手のサーバの都合でアカウントが作れなくなる
 * ほうが困るため、失敗は記録して呼び出し元には返さない。
 */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final String from;
    private final String baseUrl;
    private final boolean configured;

    public MailService(JavaMailSender sender,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${app.mail-from:}") String from,
                       @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.sender = sender;
        this.from = from;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.configured = !host.isBlank() && !from.isBlank();
    }

    public void sendVerification(String to, String token) {
        send(to, "【Band Link】メールアドレスの確認",
                "Band Linkへの登録ありがとうございます。\n\n"
                        + "次の画面で、下の確認コードを入力してください。\n"
                        + baseUrl + "/verify-email\n\n"
                        + "確認コード: " + token + "\n\n"
                        + "このコードは24時間で使えなくなります。\n"
                        + "心当たりがない場合は、このメールを破棄してください。\n",
                "verification");
    }

    public void sendPasswordReset(String to, String token) {
        send(to, "【Band Link】パスワードの再設定",
                "パスワード再設定の手続きを受け付けました。\n\n"
                        + "次の画面で、下の再設定コードと新しいパスワードを入力してください。\n"
                        + baseUrl + "/password-reset/confirm\n\n"
                        + "再設定コード: " + token + "\n\n"
                        + "心当たりがない場合は、このメールを破棄してください。\n"
                        + "その場合、いまのパスワードはそのまま使えます。\n",
                "password reset");
    }

    private void send(String to, String subject, String body, String kind) {
        if (!configured) {
            // The token is deliberately absent here (requirements 13.3).
            log.warn("{} mail not sent: set spring.mail.host and app.mail-from to enable delivery", kind);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("{} mail sent", kind);
        } catch (RuntimeException e) {
            log.error("{} mail failed to send", kind, e);
        }
    }
}
