# ログ基盤（Elasticsearch / Kibana）

requirements.md 13.3 に対応する構成。**このセクションのうち Docker を起動しての動作確認は未実施**（所有者のPCに Docker が未導入のため）。ファイルは用意済みで、Docker 導入後に下記の手順で起動できる状態にしてある。

## 構成

```
アプリ ──> logs/band-link.ecs.json ──> Filebeat ──> Elasticsearch ──> Kibana
```

アプリは Elasticsearch へ直接送らない。ファイルへ書き、Filebeat が追跡して送る。

**この形にした理由**：13.3 が「障害時の挙動」を決めることを求めている。直接送信にすると Elasticsearch が落ちている間のログが失われるか、アプリ側で送信の失敗を扱う必要が出る。ファイル経由なら、スタックが停止していてもアプリは通常どおり書き続け、Filebeat は再開時に続きから送る。ログ基盤の障害がアプリの障害にならない。

## ログに何が入るか

`RequestIdFilter` がリクエストごとに1行出力する。13.3 が求める項目に対応：

| 要件 | フィールド |
|---|---|
| 日時 | `@timestamp` |
| レベル | `log.level` |
| 処理名 | `http_method` + `http_path` |
| リクエストID | `request_id` |
| 結果 | `http_status` |
| 所要時間 | `duration_ms` |

実際の出力例（ローカルで確認済み）：

```json
{"@timestamp":"2026-09-06T05:53:28.569033100Z","log":{"level":"INFO","logger":"com.example.bandlink.access"},
 "message":"request completed","duration_ms":"12","http_path":"/api/masters","http_status":"200",
 "http_method":"GET","request_id":"manual-check-001"}
```

`X-Request-Id` ヘッダを付けて送るとその値が引き継がれ、レスポンスにも同じヘッダが返る。付けなければ UUID を採番する。

**記録しないもの**（13.3 の確定事項）：パスワード、認証トークン、Cookie、メッセージ本文、リクエスト本文全体。クエリ文字列も記録しない（検索キーワードは利用者の情報のため）。フィルタ側で除去するのではなく、そもそも書かない方式にしている。

## 起動と停止

Docker Desktop 導入後：

```
docker compose -f docker/logging/compose.yml up -d
```

- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601

停止：

```
docker compose -f docker/logging/compose.yml down
```

データごと消す場合は `down -v`。

アプリ側は通常どおり `mvnw.cmd spring-boot:run` で起動すればよい。`logs/band-link.ecs.json` に書き出され、Filebeat がそれを拾う。

## Kibana での確認手順

1. http://localhost:5601 を開く
2. 左メニュー → Discover
3. データビューの作成を求められたら、インデックスパターンに `filebeat-*`、時刻フィールドに `@timestamp` を指定
4. 検索例：
   - 特定のリクエストを追う：`request_id : "manual-check-001"`
   - エラーだけ見る：`log.level : "ERROR"`
   - 遅いリクエスト：`duration_ms > 500`
   - 特定の画面：`http_path : "/api/posts"`

## 資源の目安

Elasticsearch のヒープは 512MB に固定している（`ES_JAVA_OPTS`）。開発機1台で他の作業と同居する前提のため、既定値のままだと過大になる。重いと感じる場合はここを下げる。

## 保持期間（未決）

Filebeat の既定に従い日次インデックス（`filebeat-8.15.3-YYYY.MM.DD`）に書かれる。**保持期間は設定していない。** ローカル開発では放置すると増え続けるため、当面は手動で削除する：

```
curl -X DELETE "http://localhost:9200/filebeat-*-2026.09.01"
```

自動削除（ILM）を入れるかどうかは、実際にどれくらい溜まるかを見てから決める。

## セキュリティ上の注意

この構成は**ローカル開発専用**。`xpack.security.enabled=false` で認証を切っており、公開環境にこのまま持ち込んではいけない。ポートは `127.0.0.1` にのみ公開している。

## 未検証

- `docker compose up` による起動、Filebeat から Elasticsearch への送信、Kibana での表示は**未確認**。Docker 未導入のため実行していない。
- 上記のログ出力（ファイルへの ECS JSON、フィールドの内容、`X-Request-Id` の引き継ぎ）はローカルのアプリ起動で確認済み。
- イメージのバージョンは 8.15.3 で揃えている（Elasticsearch / Kibana / Filebeat は一致させる必要がある）。起動時にバージョン不整合や取得失敗が出た場合は3つとも同じタグに揃えて再実行する。
