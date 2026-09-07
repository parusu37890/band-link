# 2026-09-08 UI・検索順・会話画像フォローアップ

## 変更

- 募集一覧に「新しい掲載順／投稿者のログイン順」を追加。`sort=login`は投稿者の最終ログインを主キーにする。
- モバイルの活動区分表示、検索欄フォーカス、活動エリアのスクロールバー、プロフィール背景を調整。
- 会話画像入力をラベル操作へ変更し、メッセージ本文の有無にかかわらず画像を送信できるよう修正。画像APIの`text/plain`応答をクライアントが正しく扱うようにした。

## 自動検証

- `./mvnw.cmd -B test`：成功（37 tests, failures 0, errors 0, skipped 0）
- `./mvnw.cmd -B -DskipTests compile`：成功
- `node --check src/main/resources/static/js/community.js`：成功
- `node --check src/main/resources/static/js/recruitment-search.js`：成功
- `git diff --check`：成功

## ブラウザ確認（8080）

Codex内蔵ブラウザのPlaywright相当APIで、ログイン後に以下を確認した。

- 募集一覧でログイン順を選ぶとURLが`/posts?sort=login`になり、投稿者の活動区分（オンライン中／3日以内／1週間以内／1か月以内）が表示される。
- キーワード欄のフォーカス時に青い標準アウトラインがなく、活動エリアのスクロールトラックがページ背景色になる。
- プロフィール詳細の各セクションがページ背景色で描画される。
- 会話画面で画像を選択し、本文なしで送信。送信後に添付がクリアされ、会話へ「メッセージ画像」が表示された。

## 未検証

- Playwright MCPサーバーとしてのST（環境にサーバーが無い）。
- 8081、Safari、実機、スクリーンリーダー、実LINE OAuth、Elasticsearch/Kibana。
- 異なる権限の第三者からの画像403、ログイン順とカーソルページングの大量データ組み合わせ。
