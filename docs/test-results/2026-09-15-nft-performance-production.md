# 本番環境（band-link.jp）性能測定 — 2026-09-15

`@playwright/mcp` の `browser_run_code_unsafe` で実行。対象は **本番デプロイ環境**（`https://band-link.jp`）。

ローカル版の `nft-performance-current.js` はQA用テストアカウント（`qa-release-sender@example.test`）や
固定テストID（投稿・メッセージ）にログインして測定する内容だが、本番DBにはQA fixtureを投入していない
（実データのみという方針のため）。そのため本番向けは **未認証で叩ける公開エンドポイントのみ** に絞った軽量版で計測した。
ログイン必須の経路・SSE・90日セッション保持はこの計測の対象外（ローカル環境側の結果を参照）。

## 結果: 全項目PASS

| 項目 | 回数 | p95 | 最大 | SLO | 判定 |
|---|---|---|---|---|---|
| GET /api/posts/page | 30 | 315ms | 329ms | 1000ms | PASS |
| GET /api/masters | 30 | 211ms | 277ms | 1000ms | PASS |
| GET /robots.txt | 30 | 154ms | 222ms | 1000ms | PASS |
| 初回ページ表示（/posts） | 30 | 1009ms | 1178ms | 3000ms | PASS |
| 同時10リクエスト（/api/posts/page） | 10並列 | 全体430ms、全件200 | - | - | PASS |

Renderの実プラン（Web: 0.5 vCPU/512MB、DB: 0.1 vCPU/256MB）上での計測。
コンテンツが空（投稿0件）の状態での結果である点に注意。実データが増えた後の再計測が望ましい。

未実施: 実機ブラウザ・実スクリーンリーダー・ログイン必須経路の本番負荷測定。
