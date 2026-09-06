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

構成・起動手順・Kibanaでの検索方法は [docs/logging.md](logging.md) にまとめた。

アプリはECS形式のJSONをファイルへ書き、Filebeatがそれを追跡してElasticsearchへ送る。
リクエストごとに処理名・リクエストID・結果・所要時間が1行残る（requirements 13.3）。

**Docker未導入のため、スタックの起動と送信は未検証。** アプリ側のログ出力のみ確認済み。

## CI

GitHub Actions（`.github/workflows/ci.yml`）で `main` と `feature/**`・`fix/**` へのpush、および `main` 宛のPRごとに `./mvnw -B clean test` を実行する。
PostgreSQLのサービスコンテナを同時に起動しており、DBに接続するテストもRunner上で通る。2026-09-06時点で成功。

## 増分ビルドで合成クラスが欠ける（2026-09-06、実際に発生）

`mvnw.cmd spring-boot:run` の増分コンパイルは、enumの`switch`式が生む合成クラス
（`PeerResponse$1` のような`$SwitchMap`保持クラス）を書き出さないことがある。
`PeerResponse.class` は更新されるのに `PeerResponse$1.class` が `target/classes` に無い状態になり、
実行時に `NoClassDefFoundError` → HTTP 500 になる。ソースは正しいのでCIは成功し、ローカルだけで再現する。

症状の確認:

```
ls target/classes/com/example/bandlink/dto/PeerResponse*
```

`PeerResponse$1.class` が無ければこれ。復旧は`clean`を付けた再ビルドのみ。

```
set DB_PASSWORD=<postgresのパスワード>
mvnw.cmd -B clean test
```

**enumの`switch`を含むクラスを編集したら`clean`を付ける。** 増分ビルドのまま起動しない。

## マスタ（パート・ジャンル）を入れ替えるとき

`MasterDataInitializer` は不足分を足すだけで、消しも直しもしない。利用者が追加した選択肢を
勝手に消さないための設計で、`MasterDataInitializerTest` がその挙動を固定している。
そのため一覧から何かを外す・名前を変える場合、既存DBには次の手順が要る（新規DBには不要）。

1. `MasterDataInitializer` の `add(...)` の文字列を新しい一覧にする
2. `MasterDataInitializerTest` の件数（`p.size()` / `g.size()` と `times(n)`）を合わせる
3. `scripts/dev/seed-demo-posts.sql` のデモ投稿を、残る選択肢へ振り直す
   （選択肢の名前が本文に出ている投稿は文面ごと直す）
4. アプリを起動して新しい選択肢を作らせる
5. デモ投入 → 廃止スクリプトの順に流す（2026-09-06実行確認済み）

```
$env:PGPASSWORD = "<postgresのパスワード>"
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d band_link -f scripts/dev/seed-demo-posts.sql
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d band_link -f scripts/dev/retire-master-options.sql
```

`retire-master-options.sql` は統合（ロック→邦ロックなど。参照を付け替えてから古い行を削除）と
削除（統合先が無いもの）を行い、最後に並び順を `MasterDataInitializer` と同じにする。
**廃止する選択肢への参照が1件でも残っていれば、何も消さずに中止する。**
募集はパート・ジャンルを1つ以上持つ必要があるため（requirements 5章）、
消すと項目が空になる募集の件数も併せて報告する。

psqlのNOTICEはPowerShellでは標準エラーに出るため赤字で表示されるが、`COMMIT` が出ていれば成功。

## enum（活動頻度など）の値を入れ替えるとき

マスタ表と違い、活動頻度は enum（`ActivityFrequency`）で `posts.activity_frequency` に文字列で入る。
enum から値を消すと、その文字列を持つ既存の募集は**読み込んだ時点で変換に失敗する**ので、
アプリを新しい enum で起動する前にDBを移行しておくこと。

さらに、Hibernate は `@Enumerated(STRING)` の列に enum の値を並べた CHECK 制約を作るが、
`ddl-auto: update` は**既存の制約を作り直さない**。値を移しただけでは新しい値の書き込みが
`posts_activity_frequency_check` で弾かれる。制約の張り直しまで含めて移行する。

```
$env:PGPASSWORD = "<postgresのパスワード>"
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d band_link -f scripts/dev/migrate-activity-frequency.sql
```

移行（値の付け替え）→ 制約の張り直し、の順でないと、既存行が新しい制約に違反して張り直せない。

なお `seed-demo-posts.sql` の募集INSERTは `WHERE NOT EXISTS` で、既存のデモ投稿には効かない。
一覧から選ぶ列（activity_frequency）は後段のUPDATEでも上書きするようにしてある。

## 認証・復旧メール

requirements 7章「認証・復旧用メールは提供する」に対応。登録時はメール内の本登録リンクを、
パスワード再設定時は再設定コードをSMTPで送る。確認リンクは24時間だけ有効で、
`/verify-email?token=...` を開くと既存の確認APIへ自動送信される。

### LINE Login（任意）

LINE Developers ConsoleでLINE Loginチャネルを作成し、コールバックURLを登録する。ローカルでは次をPowerShellで設定してからアプリを起動する。

```powershell
$env:LINE_CHANNEL_ID = "チャネルID"
$env:LINE_CHANNEL_SECRET = "チャネルシークレット"
$env:LINE_REDIRECT_URI = "http://localhost:8080/api/auth/line/callback"
```

`LINE_REDIRECT_URI` を省略すると `APP_BASE_URL` に `/api/auth/line/callback` を付けたURLになる。3つの環境変数がそろったときだけログイン画面に「LINEでログイン」が表示される。LINE側の設定がない環境では既存のメールログインだけを表示する。

**設定しないあいだは送信しない。** `MAIL_HOST` か `MAIL_FROM` が空なら、
`MailService` が警告を1行残すだけで登録・再設定そのものは通す。
相手のメールサーバの都合でアカウントが作れなくなるほうが困るため、送信失敗も同じ扱い。
requirements 13.3 に従い、**トークンはログや警告文へ書かない**。確認メール本文には、
利用者が本登録を完了するためのリンクとしてのみ含める。

確認リンクの動作確認でトークンをDBから直接取り出す必要はない。上のメール本文のリンクを開く。DBクエリでトークンを取り出す手順が残っている場合は、APIの障害調査など手入力が必要な開発時だけに使い、利用者向け導線には戻さない。

### 送信を有効にする

資格情報は環境変数から渡す。リポジトリには置かない。

```
$env:MAIL_HOST = "<SMTPホスト>"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "<ユーザー>"
$env:MAIL_PASSWORD = "<パスワード>"
$env:MAIL_FROM = "<差出人アドレス>"
$env:APP_BASE_URL = "https://<公開URL>"
```

`APP_BASE_URL` は本文に載せる画面のURLで、未設定なら `http://localhost:8080`。
実際の配信サービスは requirements 11章のとおり未決。

### 送信の確認方法（2026-09-07実施）

SMTPサーバを用意しなくても、受信するだけの簡易サーバで経路を確認できる。
`scripts/dev/smtp-stub.js` を起動し、アプリを次の環境変数で起動して登録・再設定を行う。

```
node scripts/dev/smtp-stub.js received.eml
$env:MAIL_HOST = "127.0.0.1"; $env:MAIL_PORT = "2525"; $env:MAIL_FROM = "no-reply@bandlink.local"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = "false"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = "false"
mvnw.cmd spring-boot:run
```

本文はbase64なので、デコードして確認メールのリンクに含まれるトークンとDBのトークンを
突き合わせる。確認済み：確認メールは本登録リンク、再設定メールは本文のコードがDBの
トークンと一致した。確認画面はメール内リンクの案内だけを表示し、トークンの手入力は求めない。

## 旧仕様で退会したアカウントの整理

2026-09-07より前の旧仕様の退会は `status` を書き換えるだけで、メールアドレス・表示名・
パスワードのハッシュ・プロフィールが残り、そのアドレスでは再登録できなかった。
既存DBには次を一度流す（新しく作るDBには不要）。何度実行しても同じ結果になる。

```
$env:PGPASSWORD = "<postgresのパスワード>"
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d band_link -f scripts/dev/scrub-withdrawn-users.sql
```

このSQLは旧仕様で残った行を互換整理するためだけに使う。新しい本人退会処理は行・会話・
メッセージを削除するため、このSQLを実行しても新しい退会データは作られない。

## 認証・会話UIの追加確認（2026-09-07）

次のJUnitをローカルPostgreSQLへ接続した状態で実行し、**33件成功、失敗・エラー・スキップ0件**を確認した。

```
$env:DB_PASSWORD = "<postgresのパスワード>"
.\mvnw.cmd -B test
Remove-Item Env:DB_PASSWORD
```

LINE APIは`MockRestServiceServer`でトークン交換・プロフィール取得・初回アカウント作成・アクセストークン欠落を検証している。LINEの環境変数を空にした場合は`/api/auth/line/enabled`が無効になり、メールの登録・ログイン画面だけが残る。未確認ユーザーの確認メール再送がゲートで塞がれないことも`EmailVerificationGateFilterTest`で確認した。

`node --check`は変更したES Modules（`account.js`、`app.js`、`community.js`、`post-editor.js`、`ui.js`）で成功し、`git diff --check`も空白エラーなしだった。8080は既存のSpring Bootプロセスが使用中で、`GET /posts`、`GET /login`、`GET /api/auth/line/enabled`（`enabled:false`）と新しいSVG・JSを応答することを確認した。8081の待受は確認できなかった。

Playwright MCPのサーバーはこの実行環境に公開されていないため、指定されたPlaywright MCPによるSTは未実施。代わりにCodexのブラウザで`/login`、`/register`、`/verify-email`を表示し、中央認証フォーム、パスワード表示ボタン、確認メール再送導線（未ログイン時は表示しない）、新しいfaviconを目視・アクセシビリティツリーで確認した。実LINEチャネルを使ったOAuth往復も資格情報を使っていないため未実施。
