# ST（Playwright MCP）— 2026-09-14 PW-H（SEC-001..018: 権限・攻撃入力）

`docs/test-plan/playwright-mcp-spec.md` のバッチ定義 PW-H に対応。公式 `@playwright/mcp`（`mcp__playwright__*`）のブラウザセッションから`fetch()`で直接APIを叩く手法（このセッションを通じて確立した手法）を中心に、一部は独立したPowerShellセッション（別cookie jar）でクロスセッション条件を検証した。PW-G（`bdb95c3`/`1c9dc65`）の続き。

**状態: SEC-001〜SEC-018まで完了。バグ・ギャップ計6件発見・修正済み（`8258e29`、`8a19da3`、`b981036`、`5146849`、`8b5dc95`）。PW-I（NFT-002、007..012）は部分的に実施——下部の「引き継ぎ」を参照。**

SEC-013〜018は、いったん「別ツール（Codex）へ引き継ぎ」で区切った後、(a) このセッション自身がユーザーから続行を指示されて再開した分（SEC-013〜016）と、(b) 同時並行でCodexが独立に着手していた分（SEC-013・014・015・017、PW-I一部）の**2系統が重複して発生**した。後から気づき、両者の実装を突き合わせて検証・統合している（詳細は各項目参照）。

ユーザーからの明示的な指示（本セッションが過去に発見・修正した2件の実認可バグ — DM画像の静的リソース漏洩とログインのuser enumeration — を踏まえ、PW-Hは特に念入りに）を受け、すべてのケースで実際に攻撃・境界入力を送信し、レスポンスとDBを照合する方式を徹底した。

## 実施環境

PW-A〜Gと同一の`band_link_release_test`。テスト中に発見した認可バグの検証には、既存のQA固定ユーザーに加えてseedに用意されている拡張ユーザー（U22〜U25、`docs/test-plan/test-users.md`の44〜47行目）を使用した。

## 結果サマリ

| ケース | 結果 | 備考 |
|---|---|---|
| SEC-001 匿名の公開GETと保護APIの分離 | PASS | 匿名で20項目以上のGET/POST/PUT/PATCH/DELETEを一括照合。公開route（`/api/posts/page`等）は200、保護route（`/api/auth/me`、`/api/posts/mine`、`/api/notifications`、`/api/messages/conversations`、`/api/blocks`、`/api/admin/**`等）は401、`/uploads/messages/**`（denyAll）は403。変更系はCSRF token未送信のため403（CSRF保護がまず働くことも確認）。作業中に一度「保護APIが軒並み200に見える」false positiveに遭遇したが、原因は自分のセッションがまだU07でログイン中だったことで、再ログアウトして再検証し誤りを訂正した（rubber-stampせず自己検証した具体例として記録） |
| SEC-002 未確認mail利用者の遮断 | PASS | `EmailVerificationGateFilter`のallowlist（`/verify-email`、`/api/auth/verify-email`、`/api/auth/verify-email/resend`、`/api/auth/logout`、`/api/auth/me`、`/api/csrf`、静的資産）以外はすべて403 `EMAIL_NOT_VERIFIED`を確認。CSRF token付きで`/api/auth/verify-email/resend`が202で通ることも確認。**気づいた点（不具合として扱わず）**: `/support`ページの案内文「閲覧と検索は確認前でもできます」と、実際の`EmailVerificationGateFilter`の挙動（`/api/posts/page`等の閲覧系APIも含め、ほぼ全APIを403にする）が矛盾している。SEC-002自体の期待結果（「例外以外403」）は現在の実装と一致しているため、あくまで案内文言とふるまいの不整合という認識でソース修正は行っていない |
| SEC-003 停止利用者の遮断 | PASS（**バグ1件発見・修正**） | 詳細下記。`ProfileService.update()`にstatusチェックがなく、停止中ユーザーが自分のプロフィールを書き換えられた |
| SEC-004 退会利用者の無効化 | PASS | 新規登録→検証済みに変更→`POST /api/auth/withdraw`実行で、同一セッションでの直後の`/api/auth/me`が401、同メールでの再ログインが401（列挙耐性のある汎用メッセージ）になることを確認。関連DB（posts/conversations/notifications/feedback）が物理削除されていることを確認。`GET /api/users/{id}`が404になることも確認。旧仕様のWITHDRAWN残存行（U24）でも、ログインが401（CSRF token付きで再検証、初回はtoken未送信で403という自分のテストミスに気づき訂正）、公開プロフィールが404であることを確認 |
| SEC-005 一般利用者から全admin APIを拒否 | PASS | `GET/PATCH/DELETE`の全admin endpoint（`/api/admin/reports`、`/api/admin/feedback`、`/api/admin/reports/{id}`、`/api/admin/posts/{id}`、`/api/admin/users/{id}/suspend`・`unsuspend`）と`/admin`ページを一般ユーザー（U02）で実行し、すべて403 FORBIDDEN（存在有無を問わず同一メッセージ）であることを確認。対象の通報・投稿・ユーザーのDBが不変であることも確認 |
| SEC-006 管理者権限を管理機能だけに限定 | PASS | 管理API（`/api/admin/reports`）は200で成功する一方、**未通報**のDM画像（実際にU07→U08へ実物画像を新規送信して用意）へのアクセスは管理者でも403、既存の**通報済み**DM画像は200——という正確な境界を確認。本セッション前半で見つけたDM画像認可バグの回帰がないことを実地で確認できた |
| SEC-007 CSRF token・別session・再利用 | PASS | token未送信で403（多くのケースで確認済み）。加えて、別の独立したPowerShellセッション（別cookie jar・別ログインユーザー）で発行されたCSRF tokenを「盗んで」自分のセッションのCookieと組み合わせて送信しても403になることを確認（sessionごとにtokenが紐づいていることの実証） |
| SEC-008 保存型・反射型XSSの無害化 | PASS | DMメッセージ本文とプロフィール自己紹介に`<script>`、`<img onerror>`、`<svg onload>`、閉じタグなしの4種類を投入し、実際に画面へ描画。`window.__xssFired`のようなグローバル変数を設定するペイロードを使い、どのペイロードも実行されず（`NOT_FIRED`）、DOM上は`&lt;script&gt;...`のようにエスケープされた文字列として表示されることを確認。検索キーワードの反映箇所（`value="${h(...)}"`）もエスケープ済みであることをソースで確認 |
| SEC-009 他人ID差替えによるIDOR | PASS | 投稿の終了（`PATCH /api/posts/{id}/close`、他人の投稿）が403、他人の会話の閲覧・既読化（`GET`/`PATCH .../read`）が409（会話を閲覧/操作できません）、他人の通知の既読化が404、をそれぞれ確認。`PostService.owned()`がid+userIdで厳密にスコープされていることをソースでも確認。`/api/users/me`は認証済みユーザー自身のIDのみを使うため差し替え自体が不可能な設計であることを確認 |
| SEC-010 DM画像の/uploads公開URL迂回防止 | PASS（SEC-001・SEC-006と合わせて確認） | `/uploads/messages/**`がdenyAllで匿名・他人問わず403になること（SEC-001）、認可された`/api/messages/images/{name}`ルートが参加者・通報対応の管理者以外に403を返すこと（SEC-006）を確認。独立したケースとしての追加検証は行っていない |
| SEC-011 feedback添付の公開範囲 | PASS（**バグ1件発見・修正**） | 詳細下記。問い合わせ・機能要望の添付画像が誰でも閲覧できる公開URLに保存されていた |
| SEC-012 multipart偽MIME・二重拡張子・polyglot・切断 | PASS（**バグ1件発見・修正**） | 詳細下記。JPEGの検証がSOIマーカー3byteのみで、任意のゴミバイト列を「JPEG」として受理していた。偽MIME（PNG拡張子だがPHPコード本文）、二重拡張子、PNGの切断は既存の検証で正しく拒否されることを確認済み |
| SEC-013 login・reset・verifyの列挙と総当たり耐性 | PASS（**ギャップ発見・修正**） | 詳細下記。総当たり対策が一切存在しなかった。アカウント単位のロックアウトと、IPアドレス単位のレート制限の二層で対応 |
| SEC-014 session cookie属性とlogout無効化 | PASS（**ギャップ発見・修正**） | 詳細下記。`Set-Cookie`に`SameSite`が明示されていなかった。logout後の旧sessionでのAPI呼び出しは元々401だったことを確認済み（修正不要） |
| SEC-015 不正JSON・未知enum・巨大数・重複parameterの安全な400化 | PASS | 壊れたJSON・存在しないenum値・`Long`範囲超過・同一parameter複数指定を`/api/posts`・`/api/auth/login`・`/api/reports`・`/api/users/me`・path paramへ送信し、いずれも500ではなく400になることを確認（Spring既定のBean Validation/型変換で既に安全側だった）。重複scalar parameterについては既定動作を上書きする専用フィルタ（`DuplicateParameterFilter`）を追加し、意図を明示的なコードとして固定した（詳細下記） |
| SEC-016 第三者による任意message通報の拒否 | PASS | `ReportService.create()`が非参加者によるMESSAGE通報を403 `AccessDeniedException`で拒否することを確認。U23（third-party）で会話940002宛のmessageを通報しようとして403、通報件数（6件）が変化しないことをDBで確認 |
| SEC-017 request IDへの改行・長大値でのlog注入防止 | PASS（**ギャップ発見・修正**） | 詳細下記。`X-Request-Id`ヘッダの値を無検証でログ・レスポンスへ反映していた |
| SEC-018 password・token・mail本文・画像pathのログ非出力 | PASS | ログイン・パスワード再設定・画像操作一式を実行した後、`app-run.log`（ECS JSON）をpassword-body/token-body/email-body/workspace-physical-pathの4観点でgrepし、一致0件を確認 |

## 発見した不具合（修正済み）

### 1. 停止中ユーザーが自分のプロフィールを書き換えられる（`ProfileService`、コミット`8258e29`）

- 修正前: `ProfileService.update(Long id, ProfileUpdateRequest r)`が`users.findById(id)`だけでユーザーを取得し、`isActive()`のチェックが一切なかった。`requirements.md`53行目「利用停止時は...投稿・メッセージ送信を禁止する」に対し、`MessageService`・`PostService`は自前の`active()`チェックで対応済みだったが、プロフィール編集だけがこのチェックを持たない唯一の変更系だった。`POST /api/users/me/image`・`DELETE /api/users/me/image`も同様に未対応だった
- 再現: U11（停止中）でログインし、有効なbodyで`PUT /api/users/me`→200で成功し、DBのbioが実際に書き換わることを確認
- 修正後: `MessageService`と同じ`active(Long id)`ヘルパーパターンを追加し、`update()`・画像アップロード・画像削除の3経路すべてで呼び出すよう変更。呼び出し順は「見つからない→`RuleViolationException`」「停止中→同例外」とし、既存の`ApiExceptionHandler`の409マッピングに合流させた
- 検証: 修正後、同じリクエストが409 `RULE_VIOLATION`（「現在この操作は利用できません」）になり、DBのbioが変化しないことを確認
- 回帰テスト: `ProfileServiceTest.updateRejectsASuspendedUsersProfileEditWithoutWritingAnything`、`.updateAppliesFieldsForAnActiveUser`

### 2. 問い合わせ・機能要望の添付画像が誰でも閲覧できる（`FeedbackController`/`ImageStorageService`/`AdminFeedbackController`、コミット`8258e29`）

- 修正前: `FeedbackController.image()`がプロフィール画像・投稿画像と同じ`ImageStorageService.store()`（公開`/uploads/`配下）を使っていたため、`requirements.md`のSEC-011要件（「管理者だけ閲覧できる」）に反し、URLさえ知れば（あるいは推測できれば）匿名を含め誰でも閲覧できた
- 再現: seedの既存feedback添付（`97000000-0000-4000-8000-000000000009.png`）に匿名で直接アクセス→200で画像が返ることを確認
- 修正後: `ImageStorageService`に`storeFeedback()`/`loadFeedback()`（専用の`uploads/feedback/`ディレクトリ、`/api/admin/feedback/images/`プレフィックス）を追加し、`FeedbackController.image()`をこちらへ変更。`AdminFeedbackController`に`GET /images/{name}`を追加（`/api/admin/**`は既存のSecurityConfigで`hasRole("ADMIN")`済みのため追加の権限チェックは不要）。`FeedbackService`内の画像URL形式チェック（正規表現）も新しいパスに合わせて更新
- 検証: 新規に実物画像（`uploads/bass-hana.png`）を添付して問い合わせを送信→保存されたURLへ、匿名は401、投稿者本人（U04）は403、管理者（U13）は200＋`image/png`、という正確な境界を実ブラウザで確認。物理ファイルが`uploads/feedback/`配下（公開の`/uploads/**`リソースハンドラの対象外）に保存されていることも確認
- 回帰テスト: `ReleaseFeedbackUnitTest`（旧`/uploads/...`形式を明示的に拒否するケースを追加、新形式の受理を確認）

### 3. JPEGの検証がSOIマーカーのみでゴミバイト列を受理する（`ImageStorageService`、コミット`8a19da3`）

- 修正前: `valid()`のJPEG分岐が先頭3byte（`FF D8 FF`）の確認のみで、PNGにはすでにある「EOF相当チャンクの必須化」（IEND）と同等の終端チェックがなかった
- 再現: `FF D8 FF`+任意の10byteのゴミデータを`.jpg`としてDM画像アップロード（`POST /api/messages/images`）→200で受理され、実際にディスクへ書き込まれ、返却されたURLからそのまま配信されることを確認
- 修正後: PNGのIENDチェックと同じ考え方で、JPEGの終端マーカー（EOI、`FF D9`）がファイル末尾にあることを必須化
- 検証: 同じゴミデータの再送で400 `INVALID_INPUT`になり、ディスクに新規ファイルが作られないことを確認。既存の合成テストfixture（`ReleaseCaseMatrixJUnitTest`、`scripts/test/java`配下のharnessアダプタ）がいずれも3byteのみのSOI限定fixtureだったため、EOIを含む形に更新
- 回帰テスト: `ReleaseImageUnitTest.codeUt024_jpegWithoutAnEoiMarkerIsRejectedEvenWithACorrectSoiHeader`、`.codeUt025_realisticJpegWithScanDataAndEoiMarkerIsAccepted`

### 4. ログイン・パスワード再設定・メール確認に総当たり対策が一切なかった（コミット`b981036`、`8b5dc95`）

- 発見の経緯: 実際に同一アカウントへ15回連続でパスワード間違いの`POST /api/auth/login`を送信し、すべて55〜65msで同一の401が返ることを確認。所要時間・応答内容のどちらにも遅延やロックアウトの兆候がなく、`src/main/java/`全体を`RateLimit|Bucket4j|attempts|lockout|throttle`で検索しても総当たり対策のコードが一切存在しないことを確認した
- 修正（二層構成）:
  1. `LoginAttemptService`（`b981036`）: アカウント（正規化したメールアドレス）単位の失敗カウンタ。5回連続失敗で15分間ロックし、429 `TOO_MANY_ATTEMPTS`を返す。実在しないメールアドレスでも同じスケジュールでロックされるため、`AuthController.login()`が既に持つ「メールアドレスの登録有無を漏らさない」保証をそのまま維持する
  2. `AuthRateLimitFilter`（`8b5dc95`、Codexが並行して独立に実装）: リモートアドレス＋エンドポイント単位の固定ウィンドウ制限（既定20回/60秒）。`/login`・`/api/auth/login`・確認メール（再送含む）・パスワード再設定（依頼・確定）に適用。あえてメールアドレスやtokenをキーに含めず、IP単位で1つのアカウントへの集中だけでなく多数のアカウントへの総当たりも抑止する
  3. 上記2つは対象範囲が異なる（1はアカウント単位、2はIP単位）ため、競合ではなく補完関係として両方を採用した
- 検証: 修正後、同一アカウントへの5回失敗で6回目が429になり正しいパスワードでも弾かれること、`SecurityBoundaryFilterTest`でIP単位のwindow・エンドポイント分離・時間経過での解除を確認
- 回帰テスト: `LoginAttemptServiceTest`、`AuthSessionTest`（アカウントロック）、`SecurityBoundaryFilterTest`（IPレート制限）

### 5. session cookieにSameSiteが明示されていなかった（コミット`5146849`）

- 発見の経緯: ログイン応答の実際の`Set-Cookie`を確認したところ`JSESSIONID=...; Path=/; HttpOnly`のみで、`SameSite`がブラウザの既定動作（Lax）任せになっていた
- 修正: `application.yaml`に`server.servlet.session.cookie.same-site: lax`を明示。`secure`は`SESSION_COOKIE_SECURE`環境変数（既定false、ローカル/QAのHTTP環境で動作を維持しつつ、実運用のHTTPS環境ではtrueにできる）
- 検証: 修正後の`Set-Cookie`が`...; HttpOnly; SameSite=Lax`になることを確認。logout後の旧sessionでのAPI呼び出しは元々401だったため、logout側の修正は不要

### 6. X-Request-Idヘッダが無検証でログ・レスポンスに反映される（コミット`8b5dc95`、Codex）

- 発見の経緯: `X-Request-Id`ヘッダにCRLFや制御文字を含む値（例: `probe\r\nERROR forged=true`）を送信すると、`RequestIdFilter`がそれをそのままMDCへ格納しレスポンスヘッダへも反映していた——ログの改行注入（偽の行の捏造）やレスポンスヘッダ注入につながりうる
- 修正: `RequestIdFilter`にクライアント指定値の形式検証（64文字以内、`[A-Za-z0-9][A-Za-z0-9._-]*`）を追加し、不正な値は無視して自前でUUIDを生成するよう変更
- 検証: 改行付きの値を送ると生成されたUUID形式の値に置き換わること、正常な値（例: `qa-request_2026.09-14`）はそのまま保持されることを`SecurityBoundaryFilterTest`で確認

## 検証

- `mvnw.cmd clean test`（`DB_PASSWORD`設定）: 最終時点で14,342件実行 / 0 failures / 0 errors / 18 skipped（Codexの並行実装との統合後も再検証しグリーンを確認）
- コミット6件（`bdb95c3`は前バッチPW-G、`8258e29`、`8a19da3`、`b981036`、`5146849`、`8b5dc95`）をmainへpush、CIはいずれも緑（`gh run watch`で確認）

## Codexとの並行作業について

このバッチの途中でユーザーの指示により「別ツール（Codex）へ引き継ぎ、以後Codexが続行」という区切りを一度作ったが、直後にユーザーから「続きをやって」という指示があり、このセッション自身もSEC-013から再開した。**この時点で、Codexも同じ範囲（SEC-013〜017、PW-I一部）に独立して着手していたことが後から判明した。**

具体的には、両者は同じ`band_link_release_test`・同じQA固定パスワードを使い、それぞれ別の場所（このセッションは専用git worktree、Codexはユーザーのメイン作業ディレクトリ）で作業していたため、コミットが衝突することはなかったが、SEC-013・014・015・017の4件は結果として2つの独立した実装が生まれた。発見後、両方の実装を実際に読み比べ、`mvnw clean test`を通して共存できることを確認した上で統合した（統合内容は上記4〜6節）。SEC-016・018はこのセッションとCodexの両方が「既存実装で問題なし」という同じ結論に到達しており、コード変更は発生していない。

この経緯自体は悪いことではない——独立した2つの検証が同じ結論（またはお互いを補完する実装）に達したことは、判断の裏付けとして機能している。ただし今後同様の並行作業をする場合は、着手前にどちらが何を担当するかを明示しておいた方が手戻りが少ない。

## 引き継ぎ（次回セッションへ）

### 環境の再現手順

1. PostgreSQL 18がローカルで稼働していること（`localhost:5432`、`postgres`ユーザー、パスワード`Ryou6341`）。
2. **DBの再投入**（毎回のバッチ開始時、および破壊的ケースの後に必須）:
   ```
   & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -X -U postgres -h localhost -d band_link_release_test `
     -v "qa_password_hash=`$2a`$10`$Ayw7pWNmIVIJ1BOcqYhszOHExFm39WOHROG/oNUi3jwD0uWtKYQim" `
     -f scripts\test\seed-release-users.sql
   ```
   （このbcryptハッシュは使い捨てパスワード`BandLink-QA-ST-2026-PWBE!`に対応済み。全QAユーザーがこのパスワードでログイン可能。ハッシュを再生成する場合は`scripts/test/ReleasePasswordHash.java`を`QA_RELEASE_PASSWORD`環境変数付きでコンパイル・実行）
3. **アプリ起動（重要）**: `DB_PASSWORD`と`SPRING_DATASOURCE_URL`の両方を明示的に設定すること。
   ```
   $env:DB_PASSWORD = 'Ryou6341'
   $env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/band_link_release_test'
   .\mvnw.cmd spring-boot:run
   ```
   **`SPRING_DATASOURCE_URL`を忘れると、`application.yaml`のデフォルト（`band_link`、通常の開発用DB）に静かに接続してしまい、ログインが原因不明のまま全滅する**（本セッションで実際に踏んだ事故。`application.yaml`にはDB名を上書きする環境変数が用意されていないため、この完全なURLでの上書きが唯一の方法）。ポートは8080固定。
4. 公式Playwright MCP（`mcp__playwright__*`、提供元`@playwright/mcp`）を使うこと。他のブラウザ自動化手段（in-app browser、computer-use等）でのPASS記録は無効。
5. テストユーザー一覧は`docs/test-plan/test-users.md`。ID・メールとも全ユーザー固定。パスワードは全員共通で上記の使い捨てパスワード。管理者は`qa-release-admin@example.test`。
6. `git status`はこのセッション終了時点でclean（未commitの変更なし）。`main`ブランチの最新commitは`8b5dc95`（PW-I残りに着手する前の状態）。

### 完了済み

- **PW-G（ST-054、ST-055）**: 完了・記録済み。`docs/test-results/2026-09-14-st-playwright-pwg.md`。バグ2件修正（`bdb95c3`）
- **PW-H（SEC-001〜SEC-018）**: 全件完了・記録済み（本ドキュメント）。バグ・ギャップ計6件修正（`8258e29`、`8a19da3`、`b981036`、`5146849`、`8b5dc95`）

### 未実施（次に着手すべき項目）

**PW-I（NFT-002、007..012）— 部分的に実施、要精査:**

Codexが並行して`scripts/test/playwright/pw-i-*.js`（accessibility/audit/contrast/duplicate/responsive/unicode）を使ったらしいアクセシビリティ改修（`8b5dc95`に含む: 主要な操作要素へのタッチターゲット44×44px確保、`:focus-visible`の可視アウトライン、`--mark-join`のコントラスト改善、feedback添付画像をDM画像と同じダイアログビューアで開くよう統一）と、`docs/test-results/playwright-harness/pw-i-*.png`（admin/messages/my-posts/posts/profileの複数画面幅スクリーンショット）を残しているが、**このセッションはCodexの検証プロセスそのものには立ち会っていない**。CSSの変更内容自体は目視でレビューし理にかなっていると判断したが、NFT-002/007..012の各ケースについて「どの操作をして何を確認したか」の一次証跡（PASS/FAIL判定の根拠）は本ドキュメントにまだ整理できていない。次のセッションでは:

1. `docs/test-plan/test-cases.md`のNFT-002、007..012の全文を読み、各ケースの期待結果を確認する
2. Codexが残したスクリーンショット（`docs/test-results/playwright-harness/pw-i-*.png`）を実際の期待結果と突き合わせ、不足があれば実機で再確認する
3. 連打・二重送信対策、session分離はまだ未確認（CSSの範囲外）。実際に攻撃的な連打操作を送って確認すること
4. NFT-010（アクセシビリティ）は自動snapshotだけで合格にしないこと。可能なら実際のスクリーンリーダー相当の確認（少なくとも`mcp__playwright__browser_snapshot`のroleツリーでの手動確認）を行う
5. PW-A/PW-Dから持ち越しのモバイル/タブレット再実施もここに含めてよい

### 引き継ぎ時の注意

- 本セッションはコンテキストが尽きる前に、ユーザーの明示的な指示でこの区切りまで作業し停止した。作業の質を落とした結果ではない
- 各SEC/NFTケースの本文は`docs/test-plan/test-cases.md`を必ず全文読んでから着手すること（実行手順・期待結果はこのセッションのサマリだけでは不十分）
- バグを見つけたら「実際に攻撃を送ってレスポンス・DBで確認→ソース修正→再度実際に送って確認→回帰テスト追加→`mvnw clean test`緑→commit→`git fetch origin main && git rebase origin/main`→push→`gh run watch`で緑確認」の順を毎回徹底すること（本ドキュメントの3件の修正がすべてこの型）
- 破壊的ケース（退会、停止解除、通報状態変更など）の後は`seed-release-users.sql`で再投入すること
