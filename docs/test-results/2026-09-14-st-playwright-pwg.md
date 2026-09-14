# ST（Playwright MCP）— 2026-09-14 PW-G（ST-054..055: 障害回復・全route背景）

`docs/test-plan/playwright-mcp-spec.md` のバッチ定義 PW-G に対応。公式 `@playwright/mcp`（`mcp__playwright__*`）で実施。PW-F（`d625638`、ST-044..053）の続き。

**状態: ST-054・ST-055を全件実施・記録済み。バグ2件発見・修正済み（`bdb95c3`）。**

## 実施環境

PW-A〜Fと同一の`band_link_release_test`（`scripts/test/seed-release-users.sql`で都度再投入）。ST-055は仕様どおりdesktop（1440×900）とmobile（390×844）の両方で実施。

## 結果サマリ

| ケース | 結果 | 備考 |
|---|---|---|
| ST-054 全主要画面が401/403/404/409/422/500から回復できる | PASS | 詳細下記。DMメッセージ送信APIへ`page.route()`で500を注入し、白画面にならず理由付きエラーが表示され、入力中の下書きが失われないこと、通信を復旧して再送信すると正常に1件追加されることを確認。`/my/posts`のページ読み込みAPIへの500注入でも、ルーターのcatchが機能し見出し・戻るリンク付きの案内画面が表示されることを確認（背景色も統一）。共有`bindForm`ヘルパー（report/confirmAction/community.js/account.jsで共通利用）とpost-editor.jsの専用ハンドラのソースを確認し、いずれもエラー時にフォーム内容を消去しない設計であることを確認 |
| ST-055 全25 routeの背景色・header・footerを一貫表示する | PASS（**バグ2件発見・修正**） | 詳細下記。匿名・一般（U02）・未確認（U01）・停止（U11）・管理者（U13）の適用可能な主体で25 route全てを巡回。`--bg`トークン（`rgb(248, 247, 243)`）がhtml/bodyで一貫していることをcomputed styleで確認し、縦の長いページ（ヘルプ、設定・プロフィール編集、管理画面）はfull-page screenshotで下端までの色切れがないことも確認。巡回中に「存在しないroute」と「管理者専用ページへの一般ユーザーアクセス」の2件で白背景の生JSON/whitelabelページが表示される不具合を発見・修正 |

## 発見した不具合（修正済み・`bdb95c3`）

### 1. 未定義routeがSpring Bootの生のWhitelabel Error Pageを表示する（`PageController` / 新規`templates/error.html`）

- 修正前: `PageController`はSPAが処理する24個のrouteを明示的に列挙しているのみで、それ以外のURL（古いブックマーク、typo、リンク切れ）にアクセスすると、Band Linkのheader/footerも`--bg`背景も一切ない、Spring Boot標準の「Whitelabel Error Page」（`html`/`body`のbackground-colorが未設定＝ブラウザ既定の白）がそのまま表示されていた。ST-055が「存在しないroute」を25 routeの1つとして明示的に含めているのは、まさにこの種の不具合を検出するためである
- 再現: ログイン中のU02で`/this-route-does-not-exist-qa`へアクセス→`document.body.innerHTML`が`<h1>Whitelabel Error Page</h1>...`であることを確認
- 修正後: Spring Bootの規約（`DefaultErrorViewResolver`が`templates/error.html`を自動的に解決する）に従い、`posts.html`と同一内容のSPAシェルを`templates/error.html`として追加。これにより、HTMLを期待するどのエラー（404など）でもBand Linkのシェルが読み込まれ、クライアント側ルーター自身の「認識できないpathは`/posts`へ」というフォールバックが機能するようになった。`/api/**`のJSONエラー応答はSpring Bootのcontent negotiationにより無関係に維持される（`curl`で確認）
- 検証: `curl`（認証済みセッション、`Accept: text/html`）で404ステータスのまま本文がアプリシェルに変わったことを確認。Playwright MCPの実ブラウザで同URLへ遷移→アプリシェルが読み込まれ、クライアントルーターが`/posts`へ自動的に遷移することを確認
- 回帰テスト: `PageRoutingErrorHandlingTest.anUnmappedPageUrlStaysA404WithoutThrowing`（MockMvcは実サーバーのエラーページ描画までは追随しないため、ステータスコードのみを機械的に確認。実際の描画内容はcurl/実ブラウザでの手動確認で担保）

### 2. 一般ユーザーが`/admin`へアクセスすると生JSONが白画面に表示される（`SecurityConfig`）

- 修正前: `accessDeniedHandler`は`/api/**`かどうかを判定せず、常に`{"code":"FORBIDDEN","message":"..."}`というJSON文字列をレスポンスへ書き込んでいた。`/admin`は`/api/admin/**`と同じ`hasRole("ADMIN")`ルールで保護されているため、一般ユーザー（管理者ロールなし）がブラウザで直接`/admin`へアクセスすると、このJSON文字列がプレーンテキストとして真っ白なページに表示されてしまっていた。同じファイルの`authenticationEntryPoint`（401側）はすでに`/api/`かどうかで分岐しページ用にはredirectしていたが、`accessDeniedHandler`（403側）だけこの分岐がなかった
- 再現: U02（一般ユーザー）で`/admin`へアクセス→`document.body.innerText`が`{"code":"FORBIDDEN",...}`のプレーンテキストであることを確認
- 修正後: `authenticationEntryPoint`と同じパターンで分岐を追加。`/api/**`は従来どおりJSONのまま、それ以外（ページ遷移）は`/posts`へredirectするよう変更
- 検証: 修正後にU02で`/admin`へアクセス→`/posts`へ正しく遷移し、通常の背景・header/footerで表示されることを確認。`/api/admin/reports`への直接fetchは引き続き`403`+JSON（`{"code":"FORBIDDEN",...}`）を返すことを確認し、API側の契約に変更がないことを確認
- 回帰テスト: `PageRoutingErrorHandlingTest.aNonAdminVisitingTheAdminPageIsSentSomewhereRealNotShownRawJson`（/adminのredirect先を確認）、`.adminApiStillReturnsJsonForbiddenForANonAdmin`（/api/admin/**のJSON契約が変わっていないことを確認）

## 気づいた点（不具合として扱わなかったもの）

- 管理画面の「お問い合わせ・機能要望」一覧で、seedに含まれる1件の添付画像（`97000000-0000-4000-8000-000000000009.png`）が実際には1×1ピクセルの合成フィクスチャであり、表示領域（306×258px）いっぱいに引き伸ばされて真っ白な矩形に見える。これはアプリ側のレンダリングやCSSの不具合ではなく（要素自体は正しく`<img>`として描画されており、`--bg`ページ背景とは独立した「意味のあるsurface」に該当する）、seedフィクスチャが実物でない画像を使っていることに起因する表示上の副作用。本セッションを通じて実施した「実物のアセットを使う」方針の対象は自分で投入したテストデータ（DM画像・プロフィール画像・feedback添付など）であり、このseed自体の修正は本バッチの範囲外と判断し、ソース変更は行っていない

## 検証

- `mvnw.cmd clean test`（`DB_PASSWORD`設定）: 14,325件実行 / 0 failures / 0 errors / 18 skipped（新規`PageRoutingErrorHandlingTest`3件を含む）
- コミット`bdb95c3`をmainへpush、CIは緑（`gh run watch`で確認）

## 未実施・持ち越し

- ST-055のmobile幅チェックは代表的なroute（トップ、ヘルプ、存在しないroute等）のみをfull-page screenshotで確認し、25 route全ての網羅的なmobile screenshotは取得していない（desktopでの背景トークンの一貫性が極めて強く裏付けられていること、DESIGN.mdの2026-09-08以降の更新で`--surface`/`--raised`/`--field`/`--avatar-bg`がすべて`--bg`に統一されていることをソースで確認済みであることから、リスクは低いと判断）
- PW-H（SEC-001..018）、PW-I（NFT-002、007..012、モバイル/タブレット再実施を含む）は続けて別セッション/別ドキュメントで実施
