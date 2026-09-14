# 公式 Playwright MCP システムテスト仕様

状態: **仕様作成済み・未実行**。対象は `ST-001..ST-055`、および画面操作を伴う `SEC`・`NFT` ケースです。

## 実行ツールの条件

Runaは、接続済みツール一覧に公式Playwright MCPの操作群が表示されていることを最初に確認します。ツール名は環境により変わっても、提供元が `@playwright/mcp` であることを確認します。Codexのin-app browser、汎用computer-use、手動ブラウザ操作を「Playwright MCP実施済み」と記録してはいけません。公式MCPが利用できなければ全STを `BLOCKED: PLAYWRIGHT_MCP_UNAVAILABLE` とし、代替実行でPASSにしません。

## 共通プロトコル

1. `test-cases.md` からケースIDを1件選び、前提ユーザーとデータを確認します。
2. ケースごとに新しいbrowser contextを作ります。ログイン継続を検証するケースだけ同じcontextを使います。
3. 最初に1440×900で実施し、指定があるケースは390×844、375×667、768×1024でも繰り返します。
4. 操作前のURL、viewport、ログイン主体、seedのanchorを記録します。password、Cookie、CSRF/tokenは記録しません。
5. locatorは表示文言、role、labelを優先します。CSS classやDOM順だけに依存しません。これによりアクセシビリティ上の名前も同時に検証します。
6. クリック前後で対象要素の可視・有効状態を確認します。固定waitを主手段にせず、URL、response、見出し、aria-busy解除など観測可能な状態を待ちます。
7. 変更要求ではmethod/path/statusを記録し、ケース指定のDB/API/UIを照合します。response bodyにsecretがある場合は保存しません。
8. browser console error、失敗request、ページ例外を各ケースで確認します。期待した4xxは失敗requestとして数えず、期待外5xxとJS例外はFAILです。
9. ケースIDを接頭辞に、開始画面、結果画面、エラー/境界画面のscreenshotを保存します。
10. 後処理を行いcontextを閉じます。破壊的ケース後はseedを再投入します。

## Playwright MCPへ渡す1ケース用指示

```text
Band LinkのリリースSTを公式Playwright MCPだけで実行してください。
対象ケースは <TEST_ID> です。docs/test-plan/test-cases.md の同IDにある全項目を読み、
docs/test-plan/test-users.md の固定ユーザー・固定IDを使ってください。
期待結果を現実装に合わせて変更しないでください。

実行前に @playwright/mcp のツール接続を確認し、別のブラウザ操作手段は使わないでください。
ケースごとに独立contextを作り、role/label/text locatorを優先してください。
操作、URL、viewport、要求status、console error、DB/API/UI照合、証跡path、後処理を記録し、
PASS / FAIL / BLOCKED のいずれかで判定してください。secretとtokenは出力しないでください。
FAIL時は再現手順を最小化し、UI / API / Security / Service / DB / filesystemのどこまで正常かを示してください。
```

## 実行バッチ

一度に多数のケースを渡さず、次のまとまりごとに新しいRunaターンで実施します。

|バッチ|ケース|主な目的|seed再投入|
|---|---|---|---|
|PW-A|ST-001..ST-010|登録、ログイン、メール、LINE|各token・登録変更後|
|PW-B|ST-011..ST-019|プロフィール、画像、media、活動表示|画像差替え後|
|PW-C|ST-020..ST-026|投稿作成、編集、終了、画像|各作成・状態変更後|
|PW-D|ST-027..ST-034|検索、ページング、詳細、導線|バッチ開始時|
|PW-E|ST-035..ST-043|DM、画像拡大、既読、SSE|ケースごと|
|PW-F|ST-044..ST-053|block、report、admin、feedback、通知|ケースごと|
|PW-G|ST-054..ST-055|障害回復、全route背景|バッチ開始時|
|PW-H|SEC-001..SEC-018|権限・攻撃入力のbrowser部分|ケースごと|
|PW-I|NFT-002、007..012|連打、分離、画面幅、a11y|ケースごと|

## 全route背景確認（ST-055）

次の25 routeを匿名、一般、未確認、停止、管理者の適用可能な主体で巡回します。

`/`, `/posts`, `/posts/new`, `/posts/920001`, `/posts/920001/edit`, `/my/posts`, `/users/910004`, `/settings`, `/settings/profile`, `/settings/blocks`, `/login`, `/register`, `/verify-email`, `/password-reset`, `/password-reset/confirm`, `/messages`, `/messages/930001`, `/notifications`, `/blocks`, `/admin`, `/admin/reports`, `/support`, `/contact`, `/feature-request`, 存在しないroute。

`html`、`body`、`#app`、`main`、ページwrapper、panel、入力欄外側についてcomputed background-colorを取得します。入力欄やdialogなど意味のあるsurfaceを除き、本文領域はDESIGN.mdの背景tokenと一致することを確認します。画面下部までfull-page screenshotを撮り、下だけ灰色、部分的な白、viewport後の色切れをFAILにします。

## 通信失敗の注入（ST-031、ST-043、ST-054）

Playwright MCPのnetwork routing機能が公開されている場合だけ、対象APIを1回だけabortまたは指定statusにします。利用できない場合は、専用のテストproxy等の承認済み手段がなければ該当部分をBLOCKEDにします。サーバー停止で他ケースを巻き込まないようにします。

## アクセシビリティ確認

- Tab順、フォーカス可視、Space/Enter/矢印/Escapeを操作として実施します。
- accessibility snapshotで、見出し階層、form label、button/link名、dialog名、live region、選択状態を保存します。
- 画像拡大dialogは開いた直後に内部へfocusし、閉じると起点画像へ戻ることを確認します。
- 色だけで「自分/相手」「募集種別」「error/success」「選択」を区別していないことを確認します。
- 自動snapshotだけで読み上げ合格にせず、NFT-010は実screen readerで別途確認します。

## 証跡ファイル名

`<TEST_ID>-<連番>-<短い内容>.png`、`<TEST_ID>-network.json`、`<TEST_ID>-console.txt`、`<TEST_ID>-result.md` とします。結果文書には実施時刻、HEAD、dirty差分一覧、browser engine/version、viewport、actor、結果、期待との差、issue候補を記録します。
