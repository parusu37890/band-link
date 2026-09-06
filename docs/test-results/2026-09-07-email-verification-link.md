# メール本登録リンクの検証（2026-09-07）

## 対象

登録後の確認メールを、確認トークンの手入力から `/verify-email?token=...` のリンク主導に変更した。
既存の `POST /api/auth/verify-email` はリンク処理の内部で利用し、確認画面のトークン手入力欄は廃止した。

## 確認結果

- `node --check src/main/resources/static/js/account.js`：成功。
- `git diff --check`：空白エラーなし。
- `./mvnw.cmd -B "-Dtest=AuthServiceTest,MailServiceTest" test`：**6件成功、失敗・エラー・スキップ0件**。
- `MailServiceTest`：確認メール本文に本登録リンクと、トークン入力が不要である案内文が含まれることを確認。
- `node --check src/main/resources/static/js/account.js`：確認画面のJavaScript構文を確認。

## 未確認

実SMTPサービスを使った実メールの受信とブラウザでの確認画面表示は未実施。`MAIL_*` と `APP_BASE_URL` を設定すれば、ローカルSMTPスタブまたはGmailで確認できる。手順は `docs/HARNESS.md` に記載している。
