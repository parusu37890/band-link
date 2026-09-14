# ST（Playwright MCP）— 2026-09-14 PW-E（ST-035..043: DM・画像拡大・既読・SSE）

`docs/test-plan/playwright-mcp-spec.md` のバッチ定義 PW-E に対応。公式 `@playwright/mcp`（`mcp__playwright__*`）で実施。PW-A（`357fe5e`）・PW-B（`9f53409`）・PW-C（`bf07eb9`）・PW-D（`ed02d20`、ST-027..032まで）の続き。ユーザーからPW-E最優先の指定があったため本バッチを完了し、続けてST-033・ST-034（PW-D残り）も実施した。

**状態: PW-E（ST-035..043）とST-033・ST-034を全件実施・記録済み。バグ3件発見・修正済み（`09055d4`）。**

## 実施環境

PW-A/B/C/Dと同一の`band_link_release_test`（`scripts/test/seed-release-users.sql`で都度再投入）。viewport 1440×900（デスクトップのみ）。加えて今回はユーザー指定により、画像・リンク系の検証はすべて実物のアセットを使用した。

- **アイコン画像**: ユーザー提供の実キャラクター顔アイコンPNG 4枚（256×256、`vocal-yuki.png`/`guitar-ren.png`/`drum-sora.png`/`bass-hana.png`）。Playwright MCPの`browser_file_upload`がリポジトリルート外のパスを拒否するため、`uploads/`配下（gitignore対象）へコピーして使用
- **メディアリンク**: プロフィール設定フォームの6項目（YouTube、TikTok、SoundCloud、Spotify、Apple Music、その他のURL）すべてに実在・稼働中のURLを設定し、公開プロフィールページで実際に埋め込みが描画されることをスクリーンショットで確認（アクセシビリティsnapshotだけではcanvas/iframe内部の描画有無が分からないため）

## 結果サマリ

| ケース | 結果 | 備考 |
|---|---|---|
| ST-035 profileのメッセージ送信からDM作成へ到達する | PASS | U08プロフィールの「メッセージを送る」CTAから`/messages?to=910008`→新規会話へ正しく遷移。相手名と入力欄が表示され送信可能 |
| ST-036 YouTube/TikTok/SoundCloud/Spotify/Apple Music/その他URLの埋め込み | PASS | 6項目すべてを実URLで保存し、公開プロフィールページで実際にレンダリングされることをスクリーンショットで確認。埋め込みコードのURL形状分岐（media-embed.js）に不具合なし |
| ST-037 本文DMをEnter規則と送信ボタンで送る | PASS（500文字境界で再検証） | 詳細下記。`82f2cd3`でDM文字数上限が1000→500に変更されたため、当初1000文字を前提に着手していた検証を破棄し、500文字ちょうどで再実施。Enter押下では送信されず（内容・件数とも不変）、「送信」ボタンのみで1件追加されることを確認 |
| ST-038 PNG/JPEG/WebP画像DMと画像のみ送信 | PASS（**バグ2件発見・修正**） | 詳細下記。実物のWebP画像を使うと必ず失敗するバグと、DM画像アップロードの検証エラーがすべて素の500になるバグを発見・修正 |
| ST-039 画像拡大表示とEscape/外側クリックでの復帰 | PASS（**バグ1件発見・修正**） | 詳細下記。共有`#dialog`に外側クリックで閉じる処理がなかった不具合を修正。Escapeキーはネイティブ`<dialog>`の挙動によりもともと正常 |
| ST-040 （ST-039と統合実施） | 上記に含む | 通報フォーム・画像lightboxが同じ`#dialog`を共有しているため、backdrop-click-to-closeの修正はST-039の検証と合わせて確認 |
| ST-041 連続送受信の会話順・自他配置・時刻順 | PASS | U07→U08の4件、U08→U07の3件（一部はミリ秒差）を含む計10件が重複・欠落なく時系列順に描画され、`mine`フラグが閲覧者ごとに正しく反転することをDOM抽出で確認。スクリーンショットで左右位置と色（緑/白）の両方で自他が識別できることも確認 |
| ST-042 会話を開くと相手の未読だけ既読になる | PASS | DBレベルで検証。U07が会話を開くとU08送信分のみ`read_at`が更新され、U07自身の送信分は無変化。逆方向（U08が開く）でも同様。要件109行目の「相手が読んだメッセージには送信者側でも『既読』を表示する」も、読了後に送信者側の表示に「既読」ラベルが現れることをDOM上で確認 |
| ST-043 SSE切断・復帰と非表示tab復帰で欠落を回収する | PASS | `page.route()`でSSEストリームへの接続を強制失敗させ「リアルタイム接続を再試行しています…」表示を確認。切断中にDBへ直接1件挿入（＝ネットワーク的に取得不能な新着を模擬）→接続復旧後のリフレッシュ（画面再読込）で1回だけ表示されステータスも正常に戻ることを確認。さらに`document.hidden`のトグルで非表示tab復帰も模擬し、同様に1回だけ回収されることを確認（重複なし） |
| ST-033 募集詳細の基準線 | PASS | P002/1440pxで、投稿タイトル直下の見出し線（`.fit-summary`のborder-top）とサイドバー`投稿者`の罫線（`.sidebar`のborder-top）が約5px差で視覚的に揃っており、背景色も単一トークン（`var(--bg)`）で統一されていることを確認 |
| ST-034 プロフィール・一覧導線 | PASS | 「募集一覧へ」「プロフィールを読む」「メッセージを送る」のクリック領域がいずれも高さ44px（要件の44px以上を満たす）、フォントサイズ15px・高コントラスト色で可読、Tab移動時に明瞭な青色フォーカスリングが表示されることを確認。詳細→プロフィール→一覧の往復導線も正常 |

## 発見した不具合（修正済み・`09055d4`）

### 1. 実物のWebP画像がDM・プロフィール画像として一切アップロードできない（`ImageStorageService`）

- 修正前: WebPのマジックバイト検証が「ファイルサイズがちょうど12バイト（RIFF/WEBPの裸ヘッダーのみ、画素データなし）」の場合のみ通過する実装になっていた。実在するWebPファイルは必ずヘッダーの後にVP8/VP8L/VP8Xチャンク（実際の画素データ）が続くため12バイトを超え、結果としてどんな実物のWebPも「画像形式を確認できません」で拒否されていた。プロジェクト自身の合成テストフィクスチャがたまたま12バイトちょうどだったため、この検証がこれまで一度も実物のWebPで検証されていなかったと考えられる
- 再現: ユーザー提供の実キャラクターアイコン（WebP変換）をDM画像として送信→拒否を確認
- 修正後: `b.length >= 12`（ヘッダー以上のサイズがあればよい）に変更。実物サイズのWebPが正しく受理されることを確認
- 回帰テスト: `ReleaseImageUnitTest.codeUt023_realisticallySizedWebpWithPayloadAfterTheHeaderIsAccepted`

### 2. DM画像アップロードの検証エラーがすべて素の500エラーになる（`ApiExceptionHandler`）

- 修正前: `POST /api/messages/images`は成功時`produces=text/plain`を宣言しており、フロントエンド（`community.js`）もそれに合わせて`Accept: text/plain`を明示送信している。一方エラーレスポンスは常にJSONボディで返す実装だったため、この組み合わせでは検証エラー（不正な画像、5MB超過など）が発生するたび、Springのコンテンツネゴシエーションが「text/plainでJSONは返せない」と判断し二重に例外を起こし、呼び出し側には理由不明の空の500だけが返っていた。上記バグ1のWebP拒否も、実際にはこの二重障害を通して単なる500として観測されていた
- 再現: 破損したWebPファイル（`qa-truncated.png`相当の壊れたバイト列）をDM画像として送信→本来は「画像形式を確認できません」等の400が返るべきところ、空の500が返ることをネットワークログで確認
- 修正後: すべてのエラーレスポンスに`contentType(MediaType.APPLICATION_JSON)`を明示。成功レスポンスの形式に関わらずエラーは常にJSONで返るようになった
- 回帰テスト: `ReleaseApiIntegrationTest.it033_invalidMessageImageReturnsReadableJsonNotABareServerError`（`QA_RELEASE_IT=true`）

### 3. 共有`#dialog`に外側クリックで閉じる操作がない（`app.js`）

- 修正前: 確認ダイアログ・通報フォーム・画像lightboxが共有するネイティブ`<dialog>`要素（`#dialog`）には、Escapeキー（ネイティブ挙動でもともと動作）はあったが、`::backdrop`（ダイアログ外側）をクリックしたときに閉じる処理が一切実装されていなかった。ST-039/ST-040が要求する「外側クリックで戻る」がどの用途（確認・通報・画像拡大）でも機能していなかった
- 再現: 画像lightbox表示中に`dialog`要素自体（＝backdrop相当）へクリックイベントを発火→`dialog.open`が`true`のまま変化しないことを確認
- 修正後: `#dialog`に`click`リスナーを追加し、`event.target.id === 'dialog'`（＝コンテンツではなくbackdropそのものがクリックされた）の場合のみ`close()`する処理を追加
- キャッシュバスティング: `posts.html`の`app.js`バージョンクエリを`?v=20260913-6`→`?v=20260913-7`に更新

## 検証

- `mvnw.cmd clean test`（`DB_PASSWORD`設定）: 14,322件実行 / 0 failures / 0 errors / 18 skipped（うち`ReleaseImageUnitTest`7件、`ReleaseMessageUnitTest`10件を含む）
- `ReleaseApiIntegrationTest`（`QA_RELEASE_IT=true`、`band_link_release_test`）: 18件実行 / 0 failures / 0 errors / 0 skipped（it033含む新規1ケースを含めクラス全体が合格）
- `82f2cd3`（bio/DM文字数上限を1000→500に変更）へのrebaseを実施後、上記2回のテスト実行はいずれもrebase後の状態で確認したもの
- CI（GitHub Actions `main` ブランチ）: `09055d4`のpushで緑を確認（`gh run watch`）

## 未実施・持ち越し

- モバイル/タブレット幅での再実施は未着手（PW-A/B/C/Dから継続の持ち越し）
- PW-F（ST-044..053: block/report/admin/feedback/notifications）は本ドキュメントの後続として別途実施・記録（`docs/test-results/2026-09-14-st-playwright-pwf.md`参照）
- PW-G・PW-H・PW-I（障害回復、権限・攻撃入力、a11y）は元々本タスクの対象範囲外
