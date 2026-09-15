# 2026-09-15 仕様差分フォローアップ（キーワード検索廃止・4週間上限・見出し削除・1件制限）の実機検証

作業対象: `docs/test-plan/latest-change-inconsistencies.md`がCodex自身の分析として残していた4項目のうち、DB一意性（NFT-003）を除く3項目＋UI確認1項目。NFT-003（同時要求時のDB一意性）はこのセッションより前に`9873c36`・`60622cc`で別途解決済みのため対象外。

開始時HEAD: `60622cc`（作業ツリーはclean、`.claude/`のみ未追跡）。

## 実施環境

- `band_link_release_test`（disposable QA DB）。`docs/test-results/2026-09-14-st-playwright-pwh.md`の引き継ぎ手順どおり`scripts/test/seed-release-users.sql`を再投入してから開始。
- アプリは`DB_PASSWORD`・`SPRING_DATASOURCE_URL`を明示指定した`spring-boot:run`（ポート8080）。
- 公式Playwright MCP（`mcp__playwright__*`、提供元`@playwright/mcp`）のみを使用。ブラウザ: Chromium、1440×900を基本、390×844で追加確認。
- 破壊的操作（後述のcreated_at書き換え、募集の新規作成2件）の後はいずれも`seed-release-users.sql`を再投入して原状回復済み。

## 1. キーワード検索削除の実機確認

- `GET http://localhost:8080/posts`のaccessibility snapshotを取得し、検索欄（`aside.search-rail`）にキーワード入力欄・キーワードトグルボタンが存在しないことを確認。都道府県・パート・ジャンル・活動スタンス・希望年齢層・活動頻度の選択式条件のみが表示される。
- ブラウザの`fetch`から実際に`GET /api/posts/page?keyword=存在しないZZZ999キーワード&limit=12`と`GET /api/posts/page?limit=12`を実行し比較。結果: 件数（12件・12件）、先頭要素のid一致（true）。キーワードが実際に無視されることをUI経由・同一セッションのAPI呼び出しで確認（サーバー側の回帰は既存の`ReleaseApiIntegrationTest.it017_removedKeywordDoesNotFilterSelectionOnlySearch`でカバー済み）。
- 判断記録: `docs/decisions/0011-keyword-search-removed.md`（0002を明示的にsupersede）。API仕様書の追記: `docs/db-api-design.md`（4.3節、検索履歴節、1.8 search_historyのconditions列説明）。

**結果: PASS**

## 2. 4週間表示上限（ST-019）

### 境界の自動チェック

QA環境に依存せず実際の`src/main/resources/static/js/ui.js`の`relativeTime`/`loginRelativeTime`をブラウザ内で`import()`し、`Date.now()`からの相対オフセットで境界値を検証するスクリプトを追加した: `scripts/test/playwright/st-019-relative-time.js`。既存の`scripts/test/playwright/pw-i-*.js`と同じ「公式Playwright MCPの`browser_run_code_unsafe`へ渡すページ関数」形式に揃えている。このプロジェクトにJSの単体テストランナー（package.json、node実行のCIジョブ）が存在しないため、既存の前例に合わせてPlaywright MCP経由の実行・記録という形式を採用した（CIには組み込んでいない。Java側の`mvnw clean test`のような自動ゲートではなく、フロントエンドの他の検証と同じくリリースごとの手動実行記録という扱い）。

実行結果（2026-09-15、`http://localhost:8080/posts`にnavigate後に実行）: 全12ケースPASS。

| 入力 | 期待 | 実際 |
|---|---|---|
| 30秒前 | 1分前 | 1分前 |
| 59分前 | 59分前 | 59分前 |
| 60分前（1時間境界） | 1時間前 | 1時間前 |
| 23時間前 | 23時間前 | 23時間前 |
| 24時間前（1日境界） | 1日前 | 1日前 |
| 6日前 | 6日前 | 6日前 |
| 7日前（1週境界） | 1週間前 | 1週間前 |
| 21日前（3週） | 3週間前 | 3週間前 |
| 27日前（3週6日、4週未満） | 3週間前 | 3週間前 |
| 28日前（4週境界） | 4週間前 | 4週間前 |
| 35日前（4週超） | 4週間前 | 4週間前 |
| 400日前（4週超・上限維持） | 4週間前 | 4週間前 |
| `loginRelativeTime`（35日前、上限なし） | "4週間前"ではない | 1か月前 |

### 実データでの確認（一覧・詳細）

seedの投稿はすべて`created_at = anchor - 7日`固定で、4週間を超える投稿がfixtureに存在しなかった（`test-users.md`はU10の投稿を「投稿時刻の4週間表示上限」用と記載しているが、`seed-release-users.sql`の実装はU10の投稿にも一律7日前しか与えておらず、ドキュメントとseedの間に齟齬があった。seedスクリプトの修正はこのタスクの範囲外のため、本セッションでは直接DBのcreated_atを書き換えて検証し、検証後にseedを再投入して原状回復した）。

- U10（910010）所有の投稿920011の`created_at`を`now() - interval '40 days'`へUPDATE。
- `/posts/920011`（詳細）: `.detail-topline`のテキストが「募集投稿 4週間前」。
- `/posts`（一覧）: 920011のカードのテキストに「投稿 4週間前」。
- 確認後、`seed-release-users.sql`を再投入して920011を元の状態へ戻した。

**結果: PASS**（自動境界チェック12/12、実データでの一覧・詳細確認2/2）

## 3. header直下の重複見出し削除とアクセシビリティ

- コード確認: `d0f2c9d`で`recruitment-search.js`から`<header class="board-heading"><h1>バンドメンバー募集</h1></header>`が削除されていることを確認。
- 実機確認（変更前の状態）: `/posts`のDOMを`document.querySelectorAll('h1,h2,h3')`で走査したところ、**`<h1>`が一つも存在しない**状態だった。他の全画面（詳細・マイページ・設定・ヘルプ・認証画面）は`page-heading`パターンや`auth-page`パターンで`<h1>`を持っており、一覧ページだけが欠落していた。これは`latest-change-inconsistencies.md`の未確定事項4（非表示h1を置くか、header内サイト名でランドマークを構成するか）に対応する実際のギャップ。
- **不具合として修正**: `recruitment-search.js`の`listing()`の先頭に`<h1 class="sr-only">バンドメンバー募集</h1>`を追加（可視の見出しは復活させない。判断根拠は`docs/decisions/0012-listing-page-hidden-h1.md`）。
- 修正後の実機確認: `/posts`のDOM走査で`h1.sr-only: バンドメンバー募集`が先頭に1件のみ存在し、以降`h2`（「条件検索」、各投稿カードの見出し）が続くことを確認。1440×900・390×844の両方でフルスクリーンショットを撮影し、可視領域に見出しテキストが現れないこと（`.sr-only`が実際にクリップされていること）を目視確認。
- `DESIGN.md`の「募集一覧の見出しと操作配置（2026-09-08）」の「画面タイトルは『バンドメンバー募集』だけに絞った」という記載が現状と矛盾していたため、末尾に2026-09-15の追記節を追加し、削除済みであることと0012の判断を明記した。

**結果: 不具合1件発見・修正・回帰確認済み（PASS）**

## 4. 募集・加入希望の1件制限（UI確認）

DBの同時実行一意性は`9873c36`・`60622cc`（`ux_posts_user_type_open`、`PostConcurrencyIntegrationTest`）で別途解決済みのため対象外。本セッションはUI側のみ確認。

- U05（910005、qa-release-wanted@example.test）でログイン。U05は`MEMBER_WANTED`のOPEN投稿（920001）を既に持ち、`WANTS_TO_JOIN`は未投稿。
- `/posts/new`で種別「募集」のまま条件・本文を入力し公開を試行 → `POST /api/posts`が**409**、画面に`role="alert"`で「公開中の募集投稿は1件までです」を表示。DBに新規投稿は作られない（後続のseed再投入前確認は省略したが、409かつ画面遷移しないことで未作成を確認）。
- 同じ入力内容のまま種別だけ「加入希望」に切り替えて再度公開 → 成功し、`/posts/{新規id}`（実行時は`/posts/1000001`）へ遷移。異種別なら1件目がOPENのままでも新規公開できることを確認。
- 確認後、`seed-release-users.sql`を再投入して新規作成分を除去。

**結果: PASS**

## 発見した不具合（まとめ）

1. **一覧ページの見出し欠落**（`recruitment-search.js`）: 2026-09-15の重複見出し削除で、`/posts`ページから`<h1>`が完全に失われていた。`h1.sr-only`を追加して修正。回帰確認は上記3節のDOM走査・スクリーンショット。専用のJUnit/Playwright自動テストは追加していない（DOM構造の確認であり、既存の`scripts/test/playwright/pw-i-accessibility.js`系のPlaywright MCP実行記録で今後のリリースごとに再確認する運用とする）。

## 後処理

- `band_link_release_test`を`seed-release-users.sql`で最終再投入し、クリーンな状態でセッションを終えた。
- Playwright MCPのbrowser contextはクローズ済み。
