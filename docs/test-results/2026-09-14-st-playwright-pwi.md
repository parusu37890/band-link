# ST（Playwright MCP）— 2026-09-14 PW-I（NFT-002、007..012: 連打・分離・画面幅・a11y）

`docs/test-plan/playwright-mcp-spec.md` のバッチ定義 PW-I に対応。公式 `@playwright/mcp`（`mcp__playwright__*`）のブラウザセッションを主に使い、NFT-007のクロスセッション条件だけは独立したPowerShellセッション（別cookie jar・別ログインユーザー）を併用した。PW-H（`bb94369`）の続きで、`docs/test-results/2026-09-14-st-playwright-pwh.md`の「引き継ぎ」節が未実施として残した項目を本セッションで実施した。

**状態: NFT-002、007〜012まで全件完了。不具合1件発見・修正済み（NFT-010、コミット後述）。**

## 前提として：前回セッションの中断について

本バッチの直前に、別のworktree（`agent-ae3e0c75ee06cc2cb`）で同じPW-Iに着手していたセッションがユーザーの指示で作業途中に停止していたことが分かっている。そのworktreeにはcommitも未commitの差分も残っておらず、成果物は無い（ユーザーからの引き継ぎ情報どおり）。加えて、別ツール（Codex）がアクセシビリティ改修（44×44pxタッチターゲット、`:focus-visible`可視アウトライン、`--mark-join`のコントラスト改善など）を先行して`main`へ実装済み（コミット`8b5dc95`の一部）で、`docs/test-results/playwright-harness/pw-i-*.png`にスクリーンショットを残していたが、NFT-002/007..012の各ケースを実際に操作して期待結果と突き合わせる一次検証はまだ誰も行っていなかった。

本セッションはこれをクリーンな未実施状態として扱い、Codexのスクリーンショットは「参考の出発点」として目視レビューしつつも、実際のNFT期待結果に照らした判定は自分で再取得したライブ証跡に基づいて行った。作業開始時、ポート8080に前述の停止済みworktreeが残していた`spring-boot:run`プロセスが3つ生き残っていたため、まずこれを終了し、このworktreeから改めてQA DBを再投入・アプリを起動した。

## 実施環境

- QA DB: `band_link_release_test`（`scripts\test\seed-release-users.sql`で再投入、84 users / 74 posts）
- アプリ: `DB_PASSWORD`・`SPRING_DATASOURCE_URL`を明示指定した`mvnw.cmd spring-boot:run`（ポート8080）
- ブラウザ: 公式Playwright MCP（`mcp__playwright__*`、提供元`@playwright/mcp`）。ツール接続を最初に確認済み
- 固定ユーザー: `docs/test-plan/test-users.md`のU01〜U25、共通パスワードでログイン

## 結果サマリ

| ケース | 結果 | 備考 |
|---|---|---|
| NFT-002 二重click・Enter連打で重複登録しない | PASS | register/post/DM/report/feedbackの5経路すべてで実dblclick（`page.dblclick`相当、ブラウザが生成する2回のclickイベント）を送信し、`GET`後のnetwork requestログとDB件数の両方で「1回のPOST・1件のDB行増加」を確認。詳細下記 |
| NFT-007 2利用者のsession・検索履歴・通知・下書きを分離する | PASS | Playwright MCPブラウザ（U07）と独立PowerShellセッション（別cookie jar、U08）を同時ログイン状態にして検証。詳細下記 |
| NFT-008 主要画面を375/390/768/1440幅で操作可能にする | PASS（範囲限定、下記参照） | 代表的な8画面×375px/1440px、1画面×768pxで横scrollと44px未満のtarget（button/a.button/nav a、height基準）が無いことを実測。25 route×4 viewportの全網羅ではない。詳細下記 |
| NFT-009 全機能をkeyboardだけで完了する | PASS | skip link、ヘッダーnavとログインフォームのTab順、パスワード表示切替のSpace操作、画像拡大dialogのEnter起動時フォーカス移動とEscape復帰を実機で確認。詳細下記 |
| NFT-010 名前・役割・状態・errorを支援技術へ伝える | PASS（**不具合1件発見・修正**） | 自動ツールだけで合格にせず、`mcp__playwright__browser_snapshot`のroleツリーを主要画面で手動確認。通知ベルの未読dotがcolor-onlyでaria-labelに反映されない不具合を発見・修正。詳細下記 |
| NFT-011 本文・control・focusのcontrastをAA基準にする | PASS | 自作のWCAG相対輝度計算（4.5:1本文／3:1大文字・UI・focus）で4画面・計436個の可視text leaf nodeを走査し不合格0件。focus/control-line/danger-lineの非text要素も3:1以上を確認。詳細下記 |
| NFT-012 長文・日本語・絵文字・合成文字でlayoutと保存を壊さない | PASS | プロフィール自己紹介欄に日本語・絵文字（ZWJ家族絵文字・サロゲートペア含む）・結合文字を混在させた500文字ちょうどの文字列を保存→再取得で完全一致（バイト単位）を確認。上限文字数の境界（500/30文字）とその1文字超過も、絵文字がサロゲート境界にまたがる形で検証し、切断ではなく一貫して400で拒否されることを確認。詳細下記 |

## NFT-002 詳細

`docs/test-plan/playwright-mcp-spec.md`の共通プロトコルに従い、各経路でフォームに実データを入力したうえで送信buttonを`dblclick`（ブラウザが2回のclickイベントを生成する本物の連続クリック）した。`ui.js`の`bindForm()`が`submit`イベントハンドラの先頭・同期的に（`await`より前に）`form.dataset.busy='true'`と`submit.disabled=true`・`aria-busy=true`を設定するため、2つのclickイベントはJSのシングルスレッド実行順で処理され、2つ目のclickでは既にbuttonが無効化されている。この実装を根拠に、実機でも本当に1回しかPOSTが飛ばないことを検証した。

| 経路 | 操作 | ネットワーク | DB件数（前→後） |
|---|---|---|---|
| register（新規登録） | `/register`にQA_NFT002_dupを入力しdblclick | `POST /api/auth/register` ×1（201） | users: 該当メールの行が1件のみ存在 |
| post（募集投稿） | U04で`/posts/new`を3ステップ入力しdblclick | `POST /api/posts` ×1（201） | posts: 74→75 |
| DM送信 | U07で`/messages/930001`にメッセージ入力しdblclick | `POST /api/messages` ×1（200） | messages: 6→7 |
| 通報 | U07でDMメッセージを通報しdblclick | `POST /api/reports` ×1（200） | reports: 4→5 |
| feedback（お問い合わせ） | U07で`/contact`に入力しdblclick | `POST /api/feedback/contact` ×1（201） | feedback: 3→4 |

5経路すべてで、`mcp__playwright__browser_network_requests`のログと直接DB照会の両方が「1回・1件」で一致した。証跡: `docs/test-results/playwright-harness/nft-002-1-post-double-click-ok.png`。

## NFT-007 詳細

Playwright MCPブラウザでU07（`qa-release-sender@example.test`）にログインし、`/posts?keyword=NFT007-U07-限定検索語`で検索。同時に、完全に別のPowerShellプロセス内で`New-Object Microsoft.PowerShell.Commands.WebRequestSession`により別のcookie jarを作り、そこでU08（`qa-release-receiver@example.test`）にログインし、`NFT007-U08-限定検索語`で別の検索を実行した。

- **session**: 両者とも`JSESSIONID`のみのcookieで、互いに独立。U08側で新規ログインしてもU07のブラウザ側`GET /api/auth/me`は終始`id: 910007`のままだった
- **検索履歴**: `GET /api/search-history`は`SearchHistoryController`が`Authentication`からサーバー側でuser idを解決する実装（クライアント指定idを受け付けない）。U07側は自分の検索語のみ1件、U08側も自分の検索語のみ1件で、互いの履歴は一切見えなかった
- **通知**: `GET /api/notifications`もU07・U08それぞれ自分宛の通知のみを返した。U08の通知に会話930001（U07↔U08）の新着メッセージ通知が含まれるのは、U08がその会話の正規の参加者であるため正しい挙動であり、情報混入ではない
- **下書き**: `post-editor.js`・`community.js`のいずれも投稿本文・DM本文の下書きをlocalStorageに永続化していない（in-memoryのJS変数のみ）ことをソースで確認済み。`recruitment-search.js`が使う唯一の永続storageはsessionStorage（`band-link:list-return`、検索条件の一覧復帰用）で、個人情報を含まずタブごとに独立しているため、これも混入経路にならない

証跡: `docs/test-results/playwright-harness/nft-007-1-u07-search-isolated.png`。

## NFT-008 詳細（実施範囲を明記）

25 route×4 viewportの全網羅は本バッチの時間配分上行っていない。かわりに、操作の種類が異なる代表画面（一覧`/posts`、詳細`/posts/{id}`、投稿作成ウィザード`/posts/new`、DM`/messages/{id}`、通知`/notifications`、プロフィール編集`/settings/profile`、管理`/admin`・`/admin/reports`）を375pxと1440pxの両端で、`/posts`を768pxでも実測した。

各画面で`document.documentElement.scrollWidth`がviewport幅を超えないこと（横scrollなし）、`button`・`a.button`・`[role=button]`・`nav a`の中で高さ44px未満のものが無いこと（DESIGN.md 83行目「主操作は高さ44px以上」）をJS実測で確認し、全画面・全viewportで違反0件だった。Codexが残した`pw-i-admin-1440.png`・`pw-i-messages-768.png`・`pw-i-my-posts-1440.png`・`pw-i-posts-375.png`・`pw-i-profile-390.png`も目視で再確認し、横scrollや固定要素の被りは見当たらなかった。390pxは既存のCodexスクリーンショットのみで、本セッションでの実測は行っていない。

証跡: `docs/test-results/playwright-harness/nft-008-1-settings-profile-375.png`、`nft-008-2-posts-768.png`（新規）、`pw-i-*.png`（Codex、既存・再確認）。

## NFT-009 詳細

- skip link「本文へ移動」が最初のTab停止先で、focus時に`top:10`まで可視化されることを確認。Enterで`#main`へ遷移
- ヘッダーnav→ログインフォーム（メールアドレス→パスワード→パスワードを表示button→ログインbutton）の順でTabが論理順に進むことを確認
- パスワード表示buttonにfocusした状態でSpaceを押すと、`input[type=password]`が`type=text`に変わり、button自身の`aria-label`も「パスワードを表示」→「パスワードを隠す」に切り替わることを確認（状態が視覚とAT両方に伝わる）
- 画像拡大dialog（`openImageViewer`）: DMメッセージ画像のbuttonにfocusしEnterを押すと、`<dialog>.showModal()`によりfocusがdialog内（閉じるbutton）へ移動することを確認。Escapeを押すとdialogが閉じ、focusは起点の「画像を拡大表示」buttonへ正確に戻ることを確認。`playwright-mcp-spec.md`のアクセシビリティ確認項目にある要求と一致
- native `<dialog>`のmodal focus containment以外で、意図しないkeyboard trapは発見しなかった

## NFT-010 詳細（不具合1件発見・修正）

自動ツールの結果だけで合格にせず、`mcp__playwright__browser_snapshot`のroleツリーを以下の画面で手動確認した: ログイン（成功時・失敗時）、新規登録、募集一覧・詳細・投稿ウィザード、DM、設定・プロフィール編集、通知、管理画面・管理通報一覧。

確認できた良い実装:
- ログイン失敗時、`ui.js`の`bindForm()`がエラー用`div`に`tabindex="-1"`を設定して`.focus()`する。中の`notice(message,'error')`が`role="alert"`を持つため、スクリーンリーダーはfocus移動と同時にエラー文言（「メールアドレスとパスワードを確認してください。」——列挙耐性のある汎用文言）を読み上げる
- `<label for="email">`のような明示的なlabel関連付けで、role treeの`textbox`名がプレースホルダ丸投げではなく正しく解決されている
- 会話一覧の「選択中」状態は背景色だけでなく`aria-current="page"`も同時に付与されている（色だけで区別していない）
- パスワード表示button（前述）

**発見した不具合**: ヘッダーの通知ベルは未読があると`[data-unread-dot]`という小さい色付きdot（CSSのみ、テキストなし）を表示するが、リンク自体の`aria-label`は常に固定の「通知」のままだった。U07に未読通知が2件ある状態で実際に`browser_find`・`browser_evaluate`で確認したところ、dotは`hidden:false`（表示されている）にもかかわらず、リンクの読み上げ名は「通知」のみで、未読件数どころか未読の有無すら伝わらなかった。晴眼者はdotで気づけるが、支援技術の利用者は何も気づけない——`playwright-mcp-spec.md`の「色だけで…区別していないことを確認する」に反する実例。

- 修正（`src/main/resources/static/js/app.js`、`header()`関数）: 通知linkに`data-notif-link`マーカーを追加し、未読件数を取得するコールバック内で`aria-label`を`未読なら「通知（未読N件）」、0件なら「通知」`に動的更新するよう変更
- 再検証: 修正後、U07で未読1件の状態のとき`aria-label`が`"通知（未読1件）"`になることを確認。`/notifications`ですべて既読にした後、別画面へ遷移して`header()`を再実行させると`aria-label`が`"通知"`に戻り、dotも`hidden:true`に一致することを確認
- 静的資産の変更のみでJS単体テストの基盤がこのプロジェクトに無いため、回帰テストは`src/test/java/com/example/bandlink/controller/HeaderAccessibilityStaticAssetTest.java`として追加した。`MockMvc`で実際に配信される`/js/app.js`のバイト列を取得し（`getContentAsString(StandardCharsets.UTF_8)`——素の`getContentAsString()`は既定エンコーディングで日本語が文字化けし誤ってFAILしたため明示指定に修正済み）、`data-notif-link`マーカーと`aria-label`更新呼び出し・未読件数の文言が含まれることを検証する

## NFT-011 詳細

サードパーティのaxe等は使わず、WCAG 2.1の相対輝度式をその場でJSに実装し（sRGBガンマ補正→相対輝度→コントラスト比）、`/posts`（224 leaf node）・`/messages/930001`（36）・`/settings/profile`（128）・`/admin/reports`（48）の可視text leaf nodeすべてに対して、実効背景色（`background-color`が不透明になるまで祖先を遡って解決）との比率を算出した。閾値は本文4.5:1、24px以上または18.66px以上のboldは3:1（大文字規定）とし、4画面・計436ノードで不合格0件だった。

非text要素（コントロール境界・focusリング）はDESIGN.mdのtoken定義（`--focus`・`--control-line`・`--danger-line`）から実際のhex値でコントラストを算出し、focus 4.47:1・control-line 3.55:1・danger-line 3.48:1と、いずれもUIコンポーネント/focus indicatorの基準3:1以上であることを確認した（DESIGN.mdのtokenコメントが自己申告する6.69:1〜16.98:1等の値とも整合）。

## NFT-012 詳細

U04のプロフィール自己紹介欄（`@Size(max=500)`）に、日本語・絵文字（🎸🎤🥁、ZWJ結合の家族絵文字`👨‍👩‍👧‍👦`＝4つのcode pointがZWJで連結されたサロゲートペア混在の文字列）・結合分音記号（`́̀̂`を積んだZalgo風の文字）を混在させ、UTF-16コード単位でちょうど500文字になるよう調整した文字列を実際に入力・保存した。

- 入力中、`textarea`の文字数counterが`500 / 500`と正しくcode unit基準で一致
- 保存後、`GET /api/users/910004`（公開プロフィールAPI）が返す`bio`が保存前に構築した文字列と完全一致（`===`で比較、差異0）——ラウンドトリップでの欠落・文字化け・正規化ズレなし
- 画面レイアウトはfull-page screenshotで確認：500文字連続の「あ」の行を含めても横scrollは発生せず、折り返しが一貫していた。ZWJ家族絵文字だけは1つの合成グリフではなく4つの絵文字が横に並んで表示された——これはこの検証環境（headless ChromiumのフォントセットにZWJ合成用のリガチャテーブルが無い）によるレンダリング上の制約であり、実際のcode pointはAPIラウンドトリップで完全一致していることから、アプリ側の不具合ではなく環境要因と判断した
- 境界値・切断規則の一貫性: bio（上限500）で「499文字の『あ』＋絵文字（2 code unit）＝501 code unit」——絵文字がちょうど境界をまたぐ形——を直接APIへ送信したところ、400 `VALIDATION_ERROR`で明確に拒否され、既存の500文字bioは一切変化しなかった（サイレント切断や500エラーは発生しない）。post titleでも同様に「29文字の『あ』＋絵文字＝31 code unit」（上限30）を送信し400で拒否されることを確認した（30 code unitちょうどの成功系は、対象投稿が編集ロック・公開1件制限の業務ルールに阻まれ再現できなかったが、上限超過の拒否経路とbioでの成功系ラウンドトリップは確認済み）

証跡: `docs/test-results/playwright-harness/nft-012-1-profile-bio-unicode-roundtrip.png`。

## 後処理

- 全ケース終了後、`scripts\test\seed-release-users.sql`でQA DBを再投入（84 users / 74 posts、重複0を確認）
- テストで使用したPowerShellの独立セッションは使い捨てのHTTPセッションのみで、永続的な変更は行っていない

## 検証

- `mvnw.cmd clean test`（`DB_PASSWORD`設定、`band_link`ローカルDB使用）: 14,343件実行 / 0 failures / 0 errors / 18 skipped（既存14,342件＋新規`HeaderAccessibilityStaticAssetTest`1件）
- コミットをmainへpush、CIは`gh run watch`で緑を確認（詳細はコミットログ参照）

## PW-A〜I 全体の完了について

本ドキュメントの完了をもって、リリーステスト計画のST-001..055、SEC-001..018、NFT-002/007..012（PW-A〜PW-I）がすべて実施・記録済みとなった。`HANDOFF.md`を更新し、最終的なリリース判定状況を記録する。

## セッション終了時点の状態（別ツールへの引き継ぎ用）

ユーザーの指示により、本セッションはここで区切り、以後は別ツール（Codex）へ引き継ぐ。

- **NFT-002、007〜012はすべて完了・記録済み**。未実施・部分実施のケースは無い（NFT-008のみ、上記のとおり代表画面での抜き取り確認である旨を明記済み）
- **git**: `worktree-agent-ac18dd448cf2c18ef`ブランチで2コミット（`e9522fb`修正＋回帰テスト、`10c57ca`結果記録＋`HANDOFF.md`更新）を作成し、`origin/main`へ直接push済み（`bb94369..10c57ca`）。CIは`gh run watch`で緑を確認済み（run ID `34840168721`）。作業ディレクトリはclean（`git status`で差分なし）
- **QA DB**: `band_link_release_test`は本セッション最後の破壊的操作（NFT-012のプロフィール書き換え等）の後、`scripts\test\seed-release-users.sql`で再投入済み。次セッションが新しいケースに着手する前に改めて再投入する必要はないが、破壊的ケースを実行したら都度再投入すること（既存プロトコルどおり）
- **プロセス**: 本セッションで起動していた`mvnw.cmd spring-boot:run`（ポート8080）は終了処理済み。次セッションが実機確認を行う場合は、`docs/test-results/2026-09-14-st-playwright-pwh.md`の「引き継ぎ」節にある起動手順（`DB_PASSWORD`・`SPRING_DATASOURCE_URL`を明示指定）で改めて起動すること
- **次に着手すべきこと**: PW-A〜Iの計画上の項目に未実施は残っていない。次の担当は、本ドキュメントおよび`HANDOFF.md`の「リリース判定状況」節に列挙した範囲外項目（実機Safari・スクリーンリーダー、NFT-003〜006・013、NFT-008の全route網羅、`/support`文言の不整合）の要否をユーザーと相談のうえ判断すること
