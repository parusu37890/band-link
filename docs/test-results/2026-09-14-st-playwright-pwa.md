# ST（Playwright MCP）— 2026-09-14 PW-A（ST-001..010: 登録・ログイン・メール・LINE）

`docs/test-plan/playwright-mcp-spec.md` のバッチ定義 PW-A に対応。公式 `@playwright/mcp`（`mcp__playwright__*`）で実施。

## 実施環境

- 専用ビルド起動: `mvnw.cmd spring-boot:run`、`SERVER_PORT=8082`、`SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/band_link_release_test`、`SPRING_JPA_HIBERNATE_DDL_AUTO=none`（既存スキーマ・マスタを前提。作成は別途 `band_link` 相当の起動で実施）
- 専用DB `band_link_release_test` を都度 `scripts/test/seed-release-users.sql` で再投入（`ReleasePasswordHash.java` で生成した使い捨てハッシュ、平文は `BandLink-QA-ST-2026!`）
- viewport 1440×900（デスクトップのみ。モバイル/タブレット幅での再実施は未実施）
- メール送信・LINE連携は環境未構成のため、確認トークン・再設定トークンはDBから直接取得（`docs/test-plan/test-users.md` に明記された既定の代替手段）

## 結果サマリ

| ケース | 結果 | 備考 |
|---|---|---|
| ST-001 初見登録→確認案内 | PASS | 201→自動ログイン→`/verify-email`。DB行(id=1000001)確認済み |
| ST-002 登録の必須・境界エラー | PASS | ブラウザネイティブ検証(required/type=email/maxlength=80/type=number)で送信ブロック、日本語メッセージ、フォーカス移動を確認。年齢の非数字入力・表示名の上限超過はinput属性で構造的に防止されており、別途アプリ側エラー表示は発生しない |
| ST-003 login成功/誤password/未知mail | PASS（**バグ2件発見・修正**） | 詳細下記 |
| ST-004 logout後browser back | PASS | `/login?next=/settings` へ。personal dataの再表示なし。`/api/auth/me`は401 |
| ST-005 未確認利用者の導線 | PASS | posts/messages/settings/support いずれも`/verify-email`へ誘導。再送・ログアウトのみ操作可 |
| ST-006 確認mail linkの一度性 | PASS | 有効token→verified、同一token再利用→「トークンが無効または期限切れです」の安全な案内（クラッシュ・再確認なし） |
| ST-007 確認再送の連打防止 | PASS | 1回目クリックでボタンが`disabled`化、2回目のクリックは自動化ツールからも実行不可なほど確実。DB上の有効token数は1のまま |
| ST-008 password再設定→新passwordログイン | PASS | 依頼→DB上の新規token→confirm→旧password 401（修正後の正しいメッセージ）→新password 200 |
| ST-009 存在しないmailでも同一案内 | PASS | 未登録・登録済み両方で`202`+「再設定の案内を送信しました。メールをご確認ください。」、差異なし |
| ST-010 LINE無効時/有効時 | PARTIAL | 無効時（現環境）: ボタン非表示・`/api/auth/line/enabled`が`{"enabled":false}`を確認しPASS。**有効時（state付き遷移・OAuth往復）はBLOCKED**: `LINE_CHANNEL_ID`等が本環境に未設定のため実機検証不可。state不一致拒否のロジック自体は`LineLoginServiceTest`（JUnit）で別途カバー |

## 発見した不具合（修正済み・コミット `357fe5e`）

### 1. ログイン失敗時のエラーメッセージがすり替わる（`static/js/ui.js`）

`api()`共通fetchヘルパーが、**すべての401レスポンス**をサーバーの実メッセージを無視して固定文言「ログインが必要です。ログインしてからもう一度お試しください。」に置き換えていた。ログインフォーム自身への誤ったパスワード入力に対してもこの文言が出るため、「たった今ログインしようとした人に『ログインしてください』と表示する」という支離滅裂な状態になっていた。

- 修正前: `curl`で確認した実際のサーバー応答は `{"code":"UNAUTHENTICATED","message":"メールアドレスとパスワードを確認してください。"}` だが、画面には別の文言が出ていた
- 修正後: サーバーの`message`を優先し、401の固定文言はサーバーが何も返さなかった場合のみのフォールバックに変更
- ブラウザで再現・修正を確認（本レポート ST-003 参照）

### 2. ログインでメールアドレスの登録有無が判別できてしまう（`AuthController.login`）

ログイン処理が認証を試みる前に`userRepository.existsByEmail(email)`を呼び、未登録メールには「このメールアドレスは登録されていません。」という専用メッセージを返していた。一方パスワード違いは「メールアドレスとパスワードを確認してください。」。この2つの文言の違いだけで、総当たり的にメールアドレスがBand Linkに登録済みかどうかを判別できてしまう（ユーザー列挙）。

同じプロジェクトが ST-009（パスワード再設定）で明示的に要求している「存在しないmailでもアカウント有無を画面に出さない」という原則が、ログイン自体には適用されていなかった。

- 修正: `existsByEmail`の事前チェックを削除。`BandLinkUserDetailsService`は既に未登録ユーザーに対して`UsernameNotFoundException`を投げており、Spring Securityの既定設定（`hideUserNotFoundExceptions=true`）がこれをパスワード違いと同じ`BadCredentialsException`に変換するため、両者は同一の一般メッセージに揃う
- ブラウザで再現・修正を確認（本レポート ST-003 参照）: 修正前は未登録mailとパスワード違いで異なる文言、修正後は両方とも「メールアドレスとパスワードを確認してください。」

## 検証

- `mvnw.cmd -B clean test`: 14,311件実行 / 0 failures / 0 errors / 15 skipped（修正反映後）
- GitHub Actions CI: green（コミット `357fe5e`）

## 未実施・持ち越し

- モバイル/タブレット幅（390×844、375×667、768×1024）での再実施
- ST-010のLINE有効時（実際のOAuth往復）はテスト用LINEチャネルの用意が必要
- PW-B以降（プロフィール・投稿・検索・DM・block/report/admin等）は未着手
