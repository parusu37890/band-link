# Band Link インフラ・ログ基盤設計

状態: 実装前の設計案。Elasticsearch・Kibanaを今回のローカル開発基盤に含めるという要件を具体化する。実際の起動・接続確認前に「構築済み」と扱わない。

## 目的と範囲

- PostgreSQLは業務データ、Elasticsearchはアプリログの検索・保持に使う。
- KibanaはElasticsearchに保存したログの検索・可視化に使う。
- 募集検索や検索履歴をElasticsearchへ保存しない。
- 初回のローカル構成はDocker Composeで再現可能にする。公開環境の構成は別途決める。

## ローカル構成案

```text
Spring Boot --JSON logs--> Log collector --bulk--> Elasticsearch
                                      \--> Kibana (閲覧)
Spring Boot --------------------------> PostgreSQL (業務DB)
```

- ElasticsearchとKibanaは同一系統のバージョンを使う。具体的なバージョンは導入時に固定し、Composeと文書に記録する。
- Logstashを使うか、軽量なFilebeat/Elastic Agentを使うかは、Windows上の資源使用量とJSON処理の確認後に決める。アプリからElasticsearchへ直接送信する方式は、ログ基盤停止がリクエスト処理へ影響しないことを検証できない限り採用しない。
- アプリ・DB・ログ基盤のデータ領域は分離する。ログ基盤の再作成でPostgreSQLの業務データを消さない。
- ローカルのElasticsearch/Kibanaは開発用認証情報を環境変数で渡し、リポジトリへ平文保存しない。

## アプリログ契約

JSONの共通フィールド案:

`@timestamp`, `level`, `service`, `environment`, `logger`, `message`, `request_id`, `http_method`, `path`, `status`, `duration_ms`, `user_id_hash`

- `request_id`は入口で生成または受信し、レスポンスと全関連ログへ引き継ぐ。
- `user_id_hash`は調査用の一貫した識別子が必要な場合だけ使い、メールアドレスなどの個人情報は記録しない。
- パスワード、Cookie、セッションID、認証トークン、メールアドレス、メッセージ本文、画像の内容、リクエスト本文全体は出力しない。
- 例外ログにはスタックトレースを残してよいが、例外メッセージに機密情報が混ざらないよう入力値をそのまま埋め込まない。
- アプリからElasticsearchへの送信失敗で、ユーザー向け処理を失敗させない。標準出力またはローカルファイルを一時退避先にする案を検証する。

## 保持と確認

- ローカル保持期間は初期値7日を提案する。ディスク使用量を計測して調整する。
- Kibanaで`request_id`による1リクエストの追跡、HTTPエラーの抽出、処理時間の確認ができることを受け入れ条件にする。
- 画像アップロード、ログイン失敗、権限エラー、通報操作で機密情報がログに出ないことを確認する。

## 導入手順

1. Docker Desktopの有無とWindowsの割り当てメモリを確認する。
2. Elasticsearch/Kibanaのバージョンを固定し、Composeファイルと環境変数サンプルを追加する。
3. Spring BootのJSONログ形式とrequest_idフィルターを実装する。
4. 収集エージェントを接続し、Kibanaのデータビューを作る。
5. `docs/HARNESS.md`へ、実際に起動・ログ検索できたコマンドだけを追記する。

## 未決

- Elasticsearch/Kibanaの具体バージョン、収集エージェント、Dockerメモリ割り当て。
- ローカルでの認証方式と開発用パスワードの注入方法。
- 公開環境でのログ基盤、保持期間、アクセス制御。
