# メール本登録リンクの検証（2026-09-07）

## 対象

登録後の確認メールを、確認トークンの手入力から `/verify-email?token=...` のリンク主導に変更した。
既存の `POST /api/auth/verify-email` と、リンクを開けない場合の手入力欄は残している。

## 確認結果

- `node --check src/main/resources/static/js/account.js`：成功。
- `git diff --check`：空白エラーなし。
- `./mvnw.cmd -B clean test`：**23件成功、失敗・エラー・スキップ0件**。
- `MailServiceTest`：確認メール本文に本登録リンクと案内文が含まれることを確認。
- 8081番ポートでアプリを起動し、ブラウザで `/verify-email?token=demo-token` を表示。無効トークンのエラーと、トークンなし `/verify-email` の手入力退避欄を確認。

## 未確認

実SMTPサービスを使った実メールの受信は、`MAIL_*` と `APP_BASE_URL` が未設定のため未実施。ローカルSMTPスタブでの確認手順は `docs/HARNESS.md` に記載している。
