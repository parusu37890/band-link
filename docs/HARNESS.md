# Band Link — ハーネス（動作確認済みコマンド集）

requirements.md 13.4「ハーネスエンジニアリング」に対応。ここに書くのは実際に動作確認したコマンドのみ。
未検証のコマンドは書かない、または「未検証」と明記する。

## 前提

- PostgreSQL 18がWindowsサービスとして稼働中（`postgresql-x64-18`）
- DB名 `band_link` 作成済み（2026-09-05確認）
- Java 26 / Mavenラッパー(`mvnw`/`mvnw.cmd`)同梱、グローバルmvn不要

## ローカル起動・テスト

**実行方法確認済み（2026-09-05、cmd.exe）。テスト成功にはDBパスワードが必要:**

```
set DB_PASSWORD=<postgresのパスワード>
mvnw.cmd test
```

PowerShellの場合は`$env:DB_PASSWORD = "<postgresのパスワード>"`。Maven Wrapper自体は`cmd.exe /d /c "mvnw.cmd -version"`で起動確認済み。DBパスワード未設定の環境ではPostgreSQL認証エラーになり、テストは失敗する。

アプリ起動（`mvnw.cmd spring-boot:run`）は本書作成時点では**未検証**。エンティティ・コントローラ実装後、実際に起動確認してから追記する。

## テストデータ準備

デモ表示用のユーザー4名・募集40件を追加するSQLを `scripts/dev/seed-demo-posts.sql` に置いている。これはローカルDB専用で、`demo01@bandlink.local`〜`demo04@bandlink.local` を識別子にしている。パスワードは4ユーザー共通で `password`。実行手順はDB接続情報を設定したPowerShellから次の通り（2026-09-06実行確認済み）。

```
$env:PGPASSWORD = "<postgresのパスワード>"
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d band_link -f scripts/dev/seed-demo-posts.sql
```

## Playwright MCPによるST

`.mcp.json`に`@playwright/mcp`を追加し、Claude Code再起動後に接続確認済み（2026-09-05、`mcp__playwright__*`のツール群が利用可能になった）。
画面は実装済みだが、Playwright MCPによるSTは**未実行**。デモデータ投入後に募集一覧・検索・詳細・登録・ログイン等をブラウザ操作し、手順・期待結果・実結果をここに追記する。

## ログ確認（Elasticsearch/Kibana）

Docker未導入のため未着手。導入後、Kibanaでのログ確認手順をここに追記する。

## CI

未着手。GitHub Actions等でのJUnit自動実行は今後の設計事項。
