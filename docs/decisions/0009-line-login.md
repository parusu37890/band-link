# 0009 LINE Loginを追加する

## 決定

Band Linkのログイン画面にLINE Login v2.1を追加する。認可コードの `state` をセッションに保存して照合し、LINEから取得したユーザーIDだけを `line_accounts` に保存する。アクセストークンは保存しない。

LINEで初回ログインしたユーザーには、LINEの表示名を使ったローカルアカウントを作成する。ローカルメール用の合成アドレスは非公開情報として使い、LINE側で本人認証が完了しているため `emailVerifiedAt` を設定する。既存のLINEユーザーは同じローカルアカウントへログインする。

認証情報が環境変数にないローカル環境ではLINEボタンを表示しない。既存のメール登録・ログインはそのまま利用できる。

## 理由

LINE Login v2.1はOAuth 2.0認可コードフローとOpenID Connectに対応し、プロフィール取得には `profile` と `openid` のスコープを使える。アプリ側でLINEパスワードを扱わず、`state` 検証でログイン要求をセッションに結び付けることで、既存のメール認証と独立したログイン手段にする。

## 運用上の設定

- LINE Developers ConsoleにコールバックURLを登録する。
- `LINE_CHANNEL_ID`、`LINE_CHANNEL_SECRET`、必要に応じて `LINE_REDIRECT_URI` を実行環境の秘密情報として設定する。
- 公開環境ではHTTPSのコールバックURLを使う。
