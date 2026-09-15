# 2026-09-15 最新仕様との差分

状態: 2026-09-15時点で単体98/98、PostgreSQL結合19/19、業務状態マトリクス14,227/14,227を実行済み。公式Playwright MCPによる最新UI回帰・NFT-003の同時実行DB検証・本ドキュメントの4項目のUI/文書フォローアップも完了しました（下表参照）。実機ブラウザ（Safari、実iOS/Android）、実スクリーンリーダー、NFT-008の全route×viewport網羅、`/support`文言修正は依然未着手です（このドキュメントのスコープ外、`HANDOFF.md`参照）。

この文書は、キーワード検索削除、投稿時刻の4週間表示上限、募集一覧のheader直下見出し削除、募集・加入希望の各1件制限を基準に、現行実装・既存テスト・設計記録へ残る不整合を整理します。過去の実行証跡は書き換えず、再実行結果で更新します。

|変更|現状|対応状況|
|---|---|---|
|キーワード検索削除|UI・検索サービス・履歴正規化を選択式条件だけへ統一し、keywordは後方互換の受け口で無視する実装へ更新済み。IT-017、IT-032、業務マトリクスは現行契約でPASS。公式Playwright MCPで`/posts`のUIにキーワード欄が存在しないこと、`keyword`パラメータ付きAPI呼び出しが実際に無視されることをブラウザから確認済み。|**解決済み**（2026-09-15）。`docs/db-api-design.md`4.3節に「keywordは無視」を明記。証跡: `docs/test-results/2026-09-15-post-spec-followup.md`1節|
|キーワード検索削除|`docs/decisions/0002-freetext-area-search.md` はタイトル・本文・補足エリアのキーワード検索を採用した記録のまま。|**解決済み**（2026-09-15）。`docs/decisions/0011-keyword-search-removed.md`が0002を明示的にsupersede。0002自体は履歴として書き換えなし|
|投稿時刻4週間上限|画面実装は `ui.js` の `relativeTime`（`discovery.js`はこれを呼び出すのみ）で上限を実装済み。境界を直接検証する自動テストが無かった。|**解決済み**（2026-09-15）。`scripts/test/playwright/st-019-relative-time.js`で1分/59分/1時間/23時間/1日/6日/1週/3週/3週6日/4週/4週超/400日の12境界を実行しPlaywright MCPで実データ（一覧・詳細）も確認。証跡: `docs/test-results/2026-09-15-post-spec-followup.md`2節|
|header直下見出し削除|画面実装は `recruitment-search.js` から重複見出しを削除済みだったが、削除の結果`/posts`ページに`<h1>`が一つも存在しない状態になっていた（未発見の不具合）。`DESIGN.md` は画面タイトル「バンドメンバー募集」を残す方針の記載のまま。|**解決済み**（2026-09-15）。`recruitment-search.js`に非表示`<h1 class="sr-only">`を追加して修正、`docs/decisions/0012-listing-page-hidden-h1.md`に判断記録、`DESIGN.md`に追記節。Playwright MCPで1440×900・390×844の両方で見出し階層とビジュアル非表示を確認。証跡: `docs/test-results/2026-09-15-post-spec-followup.md`3節|
|募集・加入希望を各1件|`PostService` と `PostRepository` は種別単位の存在確認へ更新済み。通常のPostgreSQL結合と業務状態マトリクスはPASS。|**DB一意性は解決済み**（`9873c36`・`60622cc`、`ux_posts_user_type_open`部分一意インデックス＋`PostConcurrencyIntegrationTest`の実2スレッド競合テスト、CI組み込み済み）。**UI確認も完了**（2026-09-15）: 同種別2件目は409＋「公開中の募集投稿は1件までです」を画面表示、異種別は成功することをPlaywright MCPで確認。証跡: `docs/test-results/2026-09-15-post-spec-followup.md`4節|
|現行リリース判定|単体・結合・業務マトリクスは2026-09-15に現行コードで再実行しPASS。公式Playwright MCPによる本ドキュメント記載4項目のUI回帰も2026-09-15に完了。|判定は`docs/test-results/release-result-2026-09-15.md`を参照。実機ブラウザ・スクリーンリーダー・NFT-008全網羅・パフォーマンスSLOなど、本ドキュメントのスコープ外の項目が残るためGOにはしていない|

## 未確定事項（更新後）

1. ~~廃止済みの `keyword` query parameterを直APIで受けた場合に、400で拒否するか無視するか。~~ → 決定済み: 無視する（`docs/decisions/0011-keyword-search-removed.md`）。400への変更は別リリース要件として扱う。
2. 投稿時刻の4週間上限は一覧カードと投稿詳細上部（`.detail-topline`）に適用されることを確認済み。詳細下部に正確な日時表示（`time()`関数、`new Intl.DateTimeFormat`）を残す既存仕様に変更はない。
3. ~~種別ごとの公開1件制限を同時要求でも保証するDB制約・ロック方式は未確定~~ → 解決済み（`ux_posts_user_type_open`、`9873c36`・`60622cc`）。
4. ~~募集一覧から可視のページ見出しを除いた後、アクセシビリティ用の非表示`h1`を置くか、header内のサイト名でランドマークを構成するかは未確定~~ → 決定済み: 非表示`h1`を一覧ページに追加する（`docs/decisions/0012-listing-page-hidden-h1.md`）。header要素はページごとのh1の代わりにはしない。

## 本ドキュメントのスコープ外として残る既知のギャップ

`HANDOFF.md`・`docs/test-results/release-result-2026-09-15.md`より（今回のセッションでは着手していない）:

- 実機ブラウザ（Safari、実iOS/Android）、実スクリーンリーダー（NVDA/VoiceOver等）による確認
- NFT-008（画面幅）の25 route×4 viewport全網羅（代表画面の抜き取りのみ実施済み）
- `/support`ページの案内文とメール未確認ゲートの実際の挙動の不整合（文言修正は未着手）
- ログ基盤（Elasticsearch/Kibana）・実LINE OAuth・公開環境そのものの確認
