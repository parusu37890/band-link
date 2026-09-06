# LINE Loginの実装検証（2026-09-07）

## 実装

- LINE Login v2.1の認可コード、state照合、アクセストークン交換、プロフィール取得を実装。
- LINEユーザーIDをローカルの `line_accounts` に保存し、初回ログイン時にローカルアカウントを作成。
- チャネル情報が環境変数にない場合は、ログイン画面にボタンを表示しない。
- LINEのアクセストークンは保存しない。

## 確認結果

- `./mvnw.cmd -B "-Dtest=AuthServiceTest,MailServiceTest,LineLoginServiceTest" test`：**8件成功、失敗・エラー・スキップ0件**。
- Javaソース97件のコンパイル：成功。
- `node --check src/main/resources/static/js/account.js`：成功。
- `git diff --check`：空白エラーなし。

## 未確認

実際のLINE Developersチャネルを使ったブラウザログインは、チャネルID・シークレット・コールバックURLが未設定のため未実施。`docs/HARNESS.md` の環境変数を設定し、LINE側にも同じコールバックURLを登録してからPlaywright MCPで確認する。
