# Renderへの本番デプロイ手順

状態: 設定ファイル(`Dockerfile`・`render.yaml`)は用意済み。実際のデプロイ・SMTP実送信・ロールバックは未リハーサル([HANDOFF.md](../HANDOFF.md)の残課題)。本書の手順を実施したら、実施日・結果を`docs/test-results/`へ記録すること。

## 前提

- GitHubへ`main`がpush済みであること。
- Renderのアカウント作成、GitHub連携の許可、支払い情報の登録は**本人が行う**（Claudeは代行しない）。

## 1. Renderアカウントを作る

1. https://render.com でサインアップし、GitHubアカウントで連携する。
2. `band-link`（または現在のリポジトリ名）へのアクセスを許可する。

## 2. Blueprintでデプロイする

このリポジトリ直下の[render.yaml](../render.yaml)がBlueprintの定義。

1. Renderダッシュボードで **New > Blueprint** を選び、このリポジトリを選択する。
2. `render.yaml`が自動検出され、以下が作成される：
   - Webサービス `band-link`（`Dockerfile`からビルド）
   - PostgreSQL `band-link-db`（無料プラン）
   - 永続ディスク `band-link-uploads`（`/app/uploads`にマウント。プロフィール・募集・メッセージ画像の保存先）
3. `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD`はDBサービスから自動注入される（`render.yaml`の`fromDatabase`）。

## 3. 手動で設定する環境変数

Renderダッシュボードの対象サービス → Environment で設定する。**値をこのリポジトリやチャットに貼らないこと。**

| 変数 | 用途 | 備考 |
|---|---|---|
| `APP_BASE_URL` | メール本文中のリンク等に使う自ホストURL | 初回デプロイ後にRenderが払い出すURL（`https://xxxx.onrender.com`）を控えて設定し、再デプロイする |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | 実SMTP送信 | SendGrid/AWS SES等。未設定のままだと[HANDOFF.md](../HANDOFF.md)の既知の未検証事項のまま |
| `MAIL_FROM` | 送信元メールアドレス | SMTPプロバイダで認証済みのドメイン/アドレスを使う |
| `LINE_CHANNEL_ID` / `LINE_CHANNEL_SECRET` / `LINE_REDIRECT_URI` | LINE Login | 使う場合のみ。`LINE_REDIRECT_URI`は`${APP_BASE_URL}/api/auth/line/callback`に一致させる |

`SESSION_COOKIE_SECURE=true`は`render.yaml`に既定で入れてある（本番はHTTPS配信のため）。

## 4. デプロイ後の確認

- `/robots.txt`がヘルスチェックに使われる（DBを触らない軽量エンドポイント）。
- 実際に開いて確認する項目：
  - トップ/`/posts`の表示
  - 新規登録 → 確認メールが実際のメールアドレスに届くか、リンクが機能するか（HANDOFF.mdの未検証事項1の解消）
  - ログイン、募集投稿、画像アップロード（永続ディスクに保存され再起動後も残るか）
  - `SESSION_COOKIE_SECURE=true`環境でCookieが発行されるか（HTTPでアクセスすると弾かれるのが正しい挙動）

## 5. ロールバック

Renderは各デプロイをスナップショットとして保持している。ダッシュボードの当該サービス → Events / Deploys から過去のデプロイを選び「Rollback to this deploy」で戻せる。DBスキーマは`ddl-auto: update`で追加のみ行う設計のため、アプリのロールバックだけでは列の削除等は戻らない点に注意する。

## 未決・持ち帰り事項

- ログ基盤（Elasticsearch/Kibana）は[docs/infra-design.md](infra-design.md)のとおりローカル設計のみで、公開環境側の構成は未定。
- 画像アップロードは永続ディスク1GBを仮置きしている。実データ量に応じてサイズ調整が必要。
- 無料/Starterプランはスリープ・リソース制限があるため、実運用トラフィックが増えたらプラン見直しが必要。
