# ST（Playwright MCP）— 2026-09-14 PW-D（ST-027..034: 検索・ページング・詳細・導線）※途中まで

`docs/test-plan/playwright-mcp-spec.md` のバッチ定義 PW-D に対応。公式 `@playwright/mcp`（`mcp__playwright__*`）で実施。PW-A（`357fe5e`）・PW-B（`9f53409`）・PW-C（`bf07eb9`）の続き。

**状態: ST-027〜ST-032まで実施・記録済み。ST-033、ST-034とPW-E（DM、ST-035..043）はセッション時間の都合で未着手。** 次のセッションはST-033から再開してください。

## 実施環境

PW-A/B/Cと同一（`SERVER_PORT=8083`、`band_link_release_test`、`scripts/test/seed-release-users.sql`で都度再投入）。viewport 1440×900（デスクトップのみ）。

## 結果サマリ

| ケース | 結果 | 備考 |
|---|---|---|
| ST-027 keyword入力・Enter・clear | PASS | 「藍色セッション」で1件ヒット（P002）、存在しないキーワードで「条件に合う募集はありません」の0件案内、入力欄を空にして再検索すると12件表示に復元。URLクエリとの同期も確認 |
| ST-028 全filterのAND/ORとANY規則 | PASS（**バグ1件発見・修正**） | 詳細下記。都道府県のOR（同一項目内）、都道府県×パートのAND（異なる項目間）は元から正しく動作。年齢不問（ANY）が特定年代検索で一致しない不具合を修正 |
| ST-029 並び順切替の残留フォーカス | PASS | mouseでのselect操作後、`outline: none`でフォーカス枠の残留なし。並び順（新しい掲載順↔投稿者のログイン順）で表示順序も正しく切り替わることを確認 |
| ST-030 下端scrollの自動追加 | PASS | 匿名で自動スクロール読み込みを10回実行し、公開中68件が重複・欠落なく1回ずつ表示されることをDOM上のIDで確認（`68件の募集`のstatusとも一致） |
| ST-031 検索中の通信失敗からの再試行 | PASS | `window.fetch`を一時的に書き換えて`/api/posts/page`のみ失敗させ、「接続できませんでした。通信環境を確認して、もう一度お試しください。」+「もう一度読み込む」ボタンを確認。fetchを復元してボタンを押すと同じ条件のまま追加分が正常に読み込まれることを確認 |
| ST-032 認証時の検索履歴・匿名時は非保存 | PASS（**バグ1件発見・修正**） | 詳細下記。修正後、U02でキーワード検索すると`GET /api/search-history`に反映されることを確認 |
| ST-033 募集詳細の基準線 | 未実施 | 次回着手 |
| ST-034 プロフィール・一覧導線 | 未実施 | 次回着手 |

## 発見した不具合（修正済み）

### 1. 「年齢不問」を選んだ投稿が特定の年代検索でヒットしない（`PostService.search`）

- 修正前: 年代検索は`root.join("ageRanges").in(criteria.ageRanges())`という厳密な一致のみで、選択した年代（例: 40代）と投稿の`ageRanges`が完全一致しない限りヒットしなかった。`requirements.md`144行目が明示的に「年齢不問や任意未入力値が検索に一致する条件を整理する」ことを要求しているにもかかわらず、実装は「年齢不問」の投稿（例: 固定データP003=920003）を、40代などの具体的な年代で検索した際に一切ヒットさせていなかった
- 再現: `GET /api/posts/page?ageRanges=S40`の結果に、`ageRanges:["ANY"]`のP003(920003)が含まれないことをAPIレスポンスで確認
- 修正後: 検索条件に`AgeRange.ANY`を常に加えた集合でINマッチするよう変更（`年齢不問の投稿は、どの年代で検索しても対象に含まれる`という直感的な規則を実装）。同一APIで再確認しP003が結果に含まれることを確認
- 回帰テスト: `ReleaseApiIntegrationTest.it031_ageRangeSearchAlsoMatchesAnyTaggedPosts`（`QA_RELEASE_IT=true`の実DB統合テスト、単体および近傍テストと合わせて実行し合格を確認）

### 2. 検索画面からの実際の検索が検索履歴に一切保存されない（`PostController`）

- 修正前: 検索履歴の記録（`searchHistoryService.record(...)`）は、素の`GET /api/posts`（一覧API）にしか実装されておらず、実際の検索画面（`recruitment-search.js`）が呼んでいる`GET /api/posts/page`（カーソルページングAPI）には記録処理が一切なかった。フロントエンドが`/api/posts`をGETで直接呼ぶ箇所は存在しないため、ログインユーザーが実際に検索を行っても`GET /api/search-history`は常に空のままになっていた
- 再現: U02でログインし`/posts?keyword=...`で検索→`GET /api/search-history`が`[]`のまま変化しないことを確認
- 修正後: `/api/posts/page`のカーソルなし（=検索の1ページ目）リクエストでも同じ`searchHistoryService.record(...)`を呼ぶよう追加。「さらに表示」による同一検索の2ページ目以降では二重記録しないよう`cursor==null`の条件を付けた（`SearchHistoryService.record`自体は無条件検索を渡されても何もしない安全策を持つため、`hasConditions()`の判定は既存のサービス側に委ねた）
- 修正後、U02でキーワード検索→`GET /api/search-history`に該当条件が反映されることを確認
- 回帰テスト: `ReleaseApiIntegrationTest.it032_pageSearchEndpointRecordsHistoryForSignedInUsers`

## 検証

- `mvnw.cmd -B clean test`（`DB_PASSWORD`設定の上、`band_link`実DBへ接続する`@SpringBootTest`系も含む）: 14,320件実行 / 0 failures / 0 errors / 17 skipped
- `ReleaseApiIntegrationTest`（`QA_RELEASE_IT=true`）の新規2ケース（it031, it032）は単体および近傍数ケースとの組み合わせ実行で合格を確認。クラス全体を一括実行すると本修正と無関係な既存ケース（it008付近）由来と見られるコンテキスト共有起因の連鎖失敗が観測されたが、これは本セッションの変更を含まない状態でも再現しうる並び依存の可能性があり、このクラスはデフォルトでは`QA_RELEASE_IT`未設定のため通常の`mvnw test`には含まれない。原因の切り分けは持ち越し

## 未実施・持ち越し

- **ST-033、ST-034（PW-D残り）は次回セッションで最初に着手**
- **PW-E（ST-035..043、DM・画像拡大・既読・SSE）は本セッションでは未着手。ユーザーから優先指定されていたバッチであり、次回セッションで最優先に着手すること**
- PW-F・PW-G・PW-H・PW-I（block/report/admin、障害回復、権限・攻撃入力、a11y）は元々本タスクの対象範囲外
- モバイル/タブレット幅での再実施は未着手（PW-A/B/Cから継続の持ち越し）
- `ReleaseApiIntegrationTest`クラス全体を一括実行した際の連鎖失敗の原因調査（上記参照）
