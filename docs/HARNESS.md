# Band Link — ハーネス（動作確認済みコマンド集）

requirements.md 13.4「ハーネスエンジニアリング」に対応。ここに書くのは実際に動作確認したコマンドのみ。
未検証のコマンドは書かない、または「未検証」と明記する。

## 前提

- PostgreSQL 18がWindowsサービスとして稼働中（`postgresql-x64-18`）
- DB名 `band_link` 作成済み（2026-09-05確認）
- Java 26 / Mavenラッパー(`mvnw`/`mvnw.cmd`)同梱、グローバルmvn不要

## ローカル起動・テスト

**動作確認済み（2026-09-05、cmd.exe、`BUILD SUCCESS`確認）:**

```
set DB_PASSWORD=<postgresのパスワード>
mvnw.cmd test
```

PowerShellの場合は`$env:DB_PASSWORD = "<postgresのパスワード>"`。

アプリ起動（`mvnw.cmd spring-boot:run`）は本書作成時点では**未検証**。エンティティ・コントローラ実装後、実際に起動確認してから追記する。

## テストデータ準備

未検証・未実装。エンティティ実装後、`data.sql`または`CommandLineRunner`によるマスタデータ（パート・ジャンル・活動スタンス・都道府県）投入方法をここに追記する。

## Playwright MCPによるST

`.mcp.json`に`@playwright/mcp`を追加し、Claude Code再起動後に接続確認済み（2026-09-05、`mcp__playwright__*`のツール群が利用可能になった）。
まだ画面が無いため実際のST実行は**未検証**。募集一覧・登録・ログイン等の画面ができ次第、実際にブラウザ操作させて手順・期待結果・実結果をここに追記する。

## ログ確認（Elasticsearch/Kibana）

Docker未導入のため未着手。導入後、Kibanaでのログ確認手順をここに追記する。

## CI

未着手。GitHub Actions等でのJUnit自動実行は今後の設計事項。
