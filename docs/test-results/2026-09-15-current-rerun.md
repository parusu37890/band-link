# 2026-09-15 現行作業ツリー再実行結果

基準: 現行作業ツリー（未コミット変更を含む）

## 15:43 Mavenフル実行の追記

Windows JDK 26向けのテストソース集約設定とQAフィクスチャのパスワード統一後、通常のMaven経路で全スイートを再実行した。**14,361件実行、成功14,361、失敗0、エラー0、スキップ0**。詳細は [2026-09-15-maven-full-test.md](2026-09-15-maven-full-test.md) を参照。

## 結果

| スイート | 検出 | 実行 | 成功 | 失敗 | スキップ |
|---|---:|---:|---:|---:|---:|
| 全業務マトリクス（JUnit DynamicTest） | 14,227 | 14,227 | 14,227 | 0 | 0 |
| 通常JUnit・Springパッケージ実行 | 14,344 | 14,325 | 14,325 | 0 | 19 |
| PostgreSQL結合 `ReleaseApiIntegrationTest` | 19 | 19 | 19 | 0 | 0 |
| 認可境界 `SecurityBoundaryFilterTest` | 6 | 6 | 6 | 0 | 0 |

通常JUnitの19スキップは、専用DBを保護する `QA_RELEASE_IT` ガードによる結合テスト分です。結合テストは専用DBで別実行し、19/19 PASSを確認しました。

## 今回の修正と回帰

- 全業務マトリクスの仕様変更追随漏れを修正（プロフィール自己紹介500文字、メッセージ本文500文字、選択式検索、募集・加入の同種別1件制限、作成時の12時間制限除外）。
- 廃止したキーワード検索はUIだけでなく検索条件・検索履歴・サービス処理でも結果へ作用しないよう統一しました。後方互換のquery parameter枠は残しています。
- `/uploads/messages/**` と `/uploads/feedback/**` を公開静的経路から拒否し、SEC-011回帰を結合テストで確認しました。

## Windows実行上の注意

JDK 26のWindows環境で発生していた `target/classes` 解決問題は、`pom.xml` のテスト用統合ソースルートで回避した。これにより通常のMaven `testCompile` と `test` が成功する。Mockitoの動的エージェント警告は表示されるが、テスト結果には影響しない。

## 未完了のリリースゲート

- 公式Playwright MCPがこのセッションに提供されていないため、最新UI変更（背景統一、検索UI、時刻表示、スマートフォン表示）の新規システムテストは未実行です。既存のPlaywright記録は過去時点の証跡としてのみ扱います。
- ユーザーテスト、実機スマートフォン、性能/NFT、同種別投稿の同時要求でDBに1件だけ残ることの実証は未完了です。
- 直APIで廃止済み `keyword` を400にするか無視するかは、現行実装を「無視」に統一したため、API仕様書と最終受入ケースの確定が必要です。


## 公式Playwright最終再実行（2026-09-15）
公式 @playwright/mcp でPW-I audit/responsive/contrast、重複送信、相対時刻、認証・認可/DM/ブロック/通報/画像分離を再実行した。PASS: 認証認可、DM送受信・既読、private画像参加者/第三者分離、contrast、相対時刻、二重送信。FAIL: 検索履歴分離判定、小ターゲット、管理feedback画像naturalWidth、unicode編集。BLOCKED: 通知SSEを含む専用a11yスクリプト。詳細と証跡: docs/test-results/2026-09-15-playwright-p0p1-rerun.md。

## 追加訂正（2026-09-15 17:50以降）

公式 `@playwright/mcp` は現在利用可能で、`st-all-001-055-current.js` によるST-001〜055個別再実行は **55/55 PASS**。性能・非機能（API/初期表示/同時10件/SSE切断復帰/90日セッション）もPASS、画像制限（PNG/WebP成功、5MiB超・SVG拒否）もPASS。最新の個別証跡は `docs/test-results/2026-09-15-st-001-055-current.*`、性能は `docs/test-results/2026-09-15-nft-performance-current.*` を参照。
