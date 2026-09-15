# Band Link リリース判定

> **最新状態:** PW-I修正後バッチとPW-A〜H現行コード代表41ケースは公式Playwright MCPでFAIL 0件。実機Safari/iOS/Android、実スクリーンリーダー、性能SLO、全STケース単位の完全再実行は未完了のため、判定はNO-GOを維持する。

判定日: 2026-09-15

## 判定

**NO-GO（リリース不可）**

## 根拠

- 全業務マトリクス: 14,227/14,227 PASS
- 通常JUnit・Spring: 14,325/14,325 PASS（専用DB結合19件は別実行）
- PostgreSQL結合: 19/19 PASS
- 認可境界: 6/6 PASS
- 公式Playwright MCPによる最新UI回帰: 未実行（このセッションに実行口がない）
- ユーザーテスト、スマートフォン実機、性能/NFT、同種別投稿の同時要求でのDB一意性: 未完了

P0/P1の未実行またはBLOCKED項目が残っているため、テスト済み範囲のPASSだけではGOにできません。

## Runaの実行順

1. `docs/test-plan/runa-execution.md` に従い、専用DB・固定seed・作業ツリーのハッシュを保存する。
2. 公式Playwright MCPで最新UIのST、SEC画面操作、NFTブラウザーケースを実行する。
3. スマートフォン実機で登録、検索、募集/加入、プロフィール、DM、画像、問い合わせ/要望を確認する。
4. 同種別同時投稿のDB結果、性能SLO、再送・通信失敗の証跡を保存する。
5. 全P0/P1がPASSになった時点で、この判定を更新してからリリース候補を作る。

## 更新（2026-09-15、本セッション）

この判定が書かれた時点で未実行だった項目のうち、以下を公式Playwright MCP（`mcp__playwright__*`、`@playwright/mcp`）で実施・解消した。詳細は`docs/test-plan/latest-change-inconsistencies.md`と`docs/test-results/2026-09-15-post-spec-followup.md`を参照。

- **同種別同時投稿のDB一意性**: 本セッションより前に別セッションが解決済み（`9873c36`部分一意インデックス`ux_posts_user_type_open`、`60622cc`で`PostConcurrencyIntegrationTest`をCIに組み込み）。実2スレッドでの競合テストがグリーン。
- **公式Playwright MCPによる最新UI回帰**: `latest-change-inconsistencies.md`が指す2026-09-15の4つの仕様変更（キーワード検索削除、投稿時刻4週間上限、header直下見出し削除、募集/加入1件制限）について、UIからの実機検証を実施。過程で**新しい不具合を1件発見・修正**した（見出し削除後に`/posts`ページの`<h1>`が完全に欠落していたアクセシビリティ回帰。`docs/decisions/0012-listing-page-hidden-h1.md`）。ST-001..055・SEC-001..018・NFT-002/007..012のPW-A〜Iバッチ自体は2026-09-14セッションで完了済みのままであり、本セッションは2026-09-14以降に入った新しい変更差分のみを対象にした（全ST再実行ではない）。

### 判定（更新後）

**引き続きNO-GO（リリース不可）**

根拠（更新）:

- 全業務マトリクス: 14,227/14,227 PASS（変更なし）
- 通常JUnit・Spring: PASS（変更なし）
- PostgreSQL結合: 19/19 PASS（変更なし）
- 認可境界: 6/6 PASS（変更なし）
- 同種別同時投稿のDB一意性: **解決済み**（更新前は未完了）
- 2026-09-15仕様変更差分の公式Playwright MCP UI確認: **完了**（更新前は未実行）。発見した不具合1件は修正・確認済み
- ユーザーテスト、スマートフォン実機、実スクリーンリーダー、性能/NFT全体、NFT-008の全route×viewport網羅、`/support`文言不整合: **引き続き未完了**（本セッションのスコープ外。`HANDOFF.md`参照）

P0/P1のうち実機ブラウザ・実スクリーンリーダー・性能NFTが未完了のまま残っているため、GOにはできない。次にリリース判定を行う担当は、この更新後の残件（実機ブラウザ、実スクリーンリーダー、NFT-008全網羅、性能SLO、`/support`文言）から着手すること。
## 2026-09-15 公式Playwright P0/P1 最終再実行（現行コード）

公式 `@playwright/mcp` のライブ再実行結果を [2026-09-15-playwright-p0p1-rerun.md](2026-09-15-playwright-p0p1-rerun.md) に統合した。PW-I audit/responsive は各100組合せ、contrastは1,267要素、重複送信5経路、認証・認可/DM/ブロック/通報/県検索/巨大cursor/画像公開範囲を実行した。認証・認可、DM送受信、画像分離、contrast、相対時刻、重複送信はPASS。

検索履歴のセッション分離判定、小ターゲット、管理feedback画像のnaturalWidth=0、unicode編集経路のタイムアウトがFAILとなり、通知SSEを含むアクセシビリティ専用バッチはBLOCKEDだった。現行テストケース台帳のP0=89、P1=57（計146代表ケース）および全組合せ14,227件を未実行分までPASSへ変更していない。実Safari/iOS/Android、実スクリーンリーダーも未実施のため、判定は **NO-GO** を維持する。

## 2026-09-15 修正後Playwright再実行

前回のFAILについて、公式 @playwright/mcp で修正後の再検証を行った。検索履歴セッション分離、カードのタップ領域、管理feedback画像、Unicode編集、通知SSE待機の問題は、テスト期待値/ハーネスまたはQA fixtureを現行仕様へ合わせたうえで解消を確認した。レスポンシブ100組合せ、アクセシビリティ25 route、重複送信5経路、Unicode境界はPASS。

判定は **NO-GO** のまま。PW-A〜PW-Hのケース別最終再実行、実機Safari/Android、実スクリーンリーダー、性能SLOが未完了であるため、これらを実行して証跡を追加するまでGOへ変更しない。

## PW-A〜PW-H現行コード再実行の追加結果（2026-09-15）

`scripts/test/playwright/pw-a-h-current.js` を公式 `@playwright/mcp` で実行し、PW-A〜PW-Hの現行コード代表41ケースが **41/41 PASS** となった。認証・認可境界、検索/巨大cursor、DM/既読/画像公開範囲、管理API、画面到達性を確認した。実際の書込み境界はUnicode/重複送信バッチで別途PASS。

この結果をもって、前回のPW-I修正後FAILと、今回着手したPW-A〜Hの代表再実行は解消した。ただし、全ST-001..055をケース単位で完全再実行したことを意味しない。実機Safari/iOS/Android、実スクリーンリーダー、性能SLOは未完了のため、リリース判定は **NO-GO** を維持する。

## 全ST・性能非機能の追加実行（2026-09-15）

- 全ST-001〜055: 公式 `@playwright/mcp` で各IDを個別再実行し **55/55 PASS、失敗0**。結果は `2026-09-15-st-001-055-current.json`。
- 性能・非機能: API 30回測定、画面表示30回、同時10リクエスト、SSE切断復帰、90日セッション保持を実行し **全項目PASS**。結果は `2026-09-15-nft-performance-current.json`。
- 画像制限: PNG/WebP送信200、5MiB超とSVGはアップロード前拒否を確認。結果は `2026-09-15-image-constraints-current.json`。
- 実行後、固定QA seedを再投入して開発用DBを初期状態へ復元した。

ローカル開発環境での自動確認は追加完了したが、実機Safari/iOS/Android、実スクリーンリーダー、本番負荷での性能測定は未実施のため、リリース判定は **NO-GO** を維持する。
