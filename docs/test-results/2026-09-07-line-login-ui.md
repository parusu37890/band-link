# LINE Login・認証・会話UIの検証（2026-09-07）

## 実装確認

- LINE Loginの認可コード交換、プロフィール取得、state照合、OAuthエラー、同一LINE IDの再利用、競合時のエラー化をコードレビューした。
- LINE連携は`line_accounts`にIDだけを保存し、アクセストークンは保存しない。本人退会時の連携削除は既存の`AccountDeletionService`を維持している。
- `LINE_CHANNEL_ID`、`LINE_CHANNEL_SECRET`、`LINE_REDIRECT_URI`が未設定の環境ではLINEボタンを表示せず、メール登録・ログインは通常どおり利用できる。
- 未確認ユーザー向け確認メール再送APIを追加し、メール確認ゲートの許可リストにも追加した。
- メッセージ本文の上限を1000文字に統一し、会話単位のSSEで新着・既読をページ再読み込みなしに反映する。送信画面の常時制限説明は削除し、空状態は短い一文に揃えた。
- 認証画面を中央フォームへ統一し、パスワード表示切替、再送ボタン、新しいBand Link SVGマーク、募集入力の30/500文字制限、白地＋青選択円、青いフォーカス表示を実装した。

## 自動テスト

```
.\mvnw.cmd -B test
```

ローカルPostgreSQL接続用の環境変数を設定して実行し、**33件成功、失敗・エラー・スキップ0件**。
`LineLoginServiceTest`はLINE APIを`MockRestServiceServer`でモックした4件。`AuthServiceTest`には確認メール再送で旧リンクを無効化するテストを追加した。

変更したJavaScriptで`node --check`を実行し、成功。`git diff --check`も空白エラーなし。

## ブラウザ確認

8080の既存Spring Bootプロセスに対し、Codex内ブラウザで次を確認した。

- `/login`：中央フォーム、目のアイコン、Band Linkマーク、不要な販促コピーなし。
- `/register`：中央フォーム、LINE未設定時にメール登録のみ表示。
- `/verify-email`：未ログイン時は再送ボタンを表示せず、案内のみ表示。
- `/assets/mark.svg`：矢印ではなく音符と接続点のSVGを返す。

## 未検証

- Playwright MCPはこの実行環境で利用可能なツールとして公開されていないため、指定されたST（募集作成、検索、画像、メッセージのSSE、通知既読、1440/820/390の3幅）は未実施。
- 実LINE Developersチャネルを使うOAuth往復は、資格情報を使用していないため未実施。
- Safari、実機キーボード、スクリーンリーダー、Elasticsearch/Kibanaは未検証。
- 8081は待受を確認できなかった。
