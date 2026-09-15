# 0011: キーワード検索の廃止（0002を上書き）

- 状態: 確定（2026-09-15）
- 関連: requirements.md 6章（検索・一覧・履歴）、[[0002-freetext-area-search]]

## 決定

募集検索からキーワード（自由文字列）検索を廃止する。検索条件は都道府県・パート・ジャンル・活動スタンス・希望年齢層・活動頻度の選択式条件のみとする。

[[0002-freetext-area-search]]は、キーワード検索の対象にタイトル・本文に加えて活動エリアの自由記入欄を含める、という決定だった。キーワード検索そのものを廃止した本決定は0002を**上書き（supersede）する**。0002は履歴として残し、本文は書き換えない。

## 理由

選択式条件だけで十分に絞り込めること、自由文字列検索がノイズの多い結果や運用上のエスケープ・インデックス設計の複雑さを持ち込んでいたことから、プロダクト判断として選択式検索へ一本化した（詳細な業務判断の経緯は本リポジトリの要件確定プロセスに従う。技術的な移行方針は以下に記す）。

## 影響・実装済みの内容

- **UI**: `src/main/resources/static/js/recruitment-search.js`からキーワード入力欄・キーワードトグルボタンを削除済み。検索フォームはキーワードを送信しない。検索結果一覧のURLクエリにも`keyword`は現れない。
- **API後方互換**: `GET /api/posts`・`GET /api/posts/page`は引き続きクエリパラメータ`keyword`を受け付けるが、**値を無視する**（`PostSearchCriteria.hasConditions()`のコメント参照）。呼び出し元が古いブックマークやAPI連携で`keyword`を送ってきても400にはせず、単に検索条件として扱わない。この契約はAPI仕様書（`docs/db-api-design.md` 4.3節）に明記した。
- **検索履歴**: `conditions`のJSONに`keyword`キーは記録されなくなった（`ReleaseApiIntegrationTest.it032_pageSearchEndpointRecordsSelectionHistoryForSignedInUsers`で回帰確認）。
- **将来の方針転換**: 将来`keyword`パラメータそのものを400で拒否する方針に変える場合は、後方互換を切る別リリースとして扱う（クライアント側の呼び出しがどれだけ残っているか確認してから判断する）。

## 検証

- サーバー側: `ReleaseApiIntegrationTest.it017_removedKeywordDoesNotFilterSelectionOnlySearch`（既存）が、`keyword`を送っても結果が変わらないことをDB照合で確認。
- UI側（本決定と同時に実施、2026-09-15）: 公式Playwright MCPで`/posts`のaccessibility snapshotを取得し、キーワード入力欄・キーワードトグルが存在しないことを確認。`fetch('/api/posts/page?keyword=存在しないZZZ999キーワード')`と`fetch('/api/posts/page')`を同一ブラウザセッションから実行し、件数・先頭要素が一致することを確認（記録: `docs/test-results/2026-09-15-post-spec-followup.md`）。
