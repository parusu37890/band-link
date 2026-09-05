# ローカルログ基盤

`docker compose -f docker-compose.observability.yml up -d`でElasticsearchとKibanaを起動する。Elasticsearchは`http://localhost:9200`、Kibanaは`http://localhost:5601`で確認する。

アプリケーションログはJSON形式、request IDを含め、認証情報・パスワード・メッセージ本文・画像URLを出力しない。Kibanaではrequest ID、HTTPステータス、処理時間、エラーコードを検索キーにする。

初期構成はローカル検証用であり、公開環境ではTLS、認証、保持期間、ローテーションを別途設定する。
