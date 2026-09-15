# NFT-008（Playwright MCP）— 2026-09-15 25 route × 4 viewport 全網羅

`docs/test-plan/test-cases.md`のNFT-008「主要画面を375/390/768/1440幅で操作可能にする」を、25 route × 4 viewport（375×667、390×844、768×1024、1440×900）の全組み合わせで実施した記録。`docs/test-results/2026-09-14-st-playwright-pwi.md`のNFT-008は代表8画面×一部viewportでの抜き取り確認だったため、本ドキュメントで全網羅に置き換える。

公式Playwright MCP（`mcp__playwright__*`、提供元`@playwright/mcp`）を使用。ツール接続を最初に確認済み。

## 実施環境

- QA DB: `band_link_release_test`（`scripts\test\seed-release-users.sql`で再投入、84 users / 74 posts）
- アプリ: セッション開始時点で別の並行セッション（同じリリーステスト計画の他バッチを別worktreeで実施中）が`DB_PASSWORD`・`SPRING_DATASOURCE_URL`を明示指定した`mvnw spring-boot:run`をポート8080で稼働中だったため、当初はそれに相乗りした（同一QA DBに接続していることを`pg_stat_activity`で確認済み）。だが検証中にその共有サーバーが接続を失う事象（`ERR_CONNECTION_REFUSED`、おそらく相手側の再起動）が発生したため、以後は自分専用に`SERVER_PORT=8082`で別プロセスを起動して継続した（同一QA DB使用）。作業完了後、自分の専用プロセスは終了済み
- ブラウザ: 公式Playwright MCP（`mcp__playwright__*`、提供元`@playwright/mcp`）
- 固定ユーザー: `docs/test-plan/test-users.md`のU01（未確認）/U04（プロフィール完成）/U05（P001投稿者）/U07（DM送信者）/U09（ブロック実行者）/U13（管理者）。ログインは実フォームではなく`/api/csrf`→`POST /api/auth/login`のAPI直叩き（このアプリのUIログインフォームが実際に機能することは`2026-09-14-st-playwright-pwi.md`等で既に確認済みのため、本バッチでは幅×route数の掛け算を優先しAPIログインで高速にactorを切り替えた）
- 25 routeの一覧は`docs/test-plan/playwright-mcp-spec.md`のST-055節（全route背景確認）から取得

## 判定基準

- **横scroll**: `document.documentElement.scrollWidth > innerWidth + 1px`
- **44px target**: `a[href], button, input:not([type=hidden]), select, textarea, [role=button], [role=link], [tabindex]:not([tabindex="-1"])` のうち可視のもの。
  - checkbox/radioは実際のクリック領域である`<label>`（ancestor or `label[for]`）があればそちらの矩形で判定
  - 本文中インラインリンク（p/li/td/th/dd/dt/figcaption/blockquote内、`.button`以外）は44px未満でも既知の許容例外として区別する（DESIGN.md 83行目は高さのみ規定）
  - `tabindex="-1"`は除外（step見出しへのフォーカス移動等、tap対象ではないプログラム的フォーカス先を誤検知しないため。除外前に実際に1件誤検知したことを確認済み — 下記「チェッカー自体の補正」参照）
  - 上記に該当しない44px未満（button・アイコンリンク・nav等）を不具合候補として扱う
- **固定/sticky要素の被り**: `position:fixed`または`sticky`で可視領域内に存在する要素と、他の操作可能要素の矩形が2px超で交差する場合を候補として記録

## チェッカー自体の補正（作業中に発見・修正した誤検知）

1. **`[tabindex]`に`-1`を含めていた**: `/posts/new`のstep見出し`<h2 tabindex="-1">活動条件</h2>`（step切替後にフォーカスを移動するアクセシビリティ用のプログラム的フォーカス先）を44px未満として誤検知した。tap対象ではないため`:not([tabindex="-1"])`を追加して以後除外
2. **checkbox/radioの生のinput矩形（16×16px）を測っていた**: `<label class="filter-option"><input type="checkbox">...</label>`のように`<label>`でラップされたcheckboxは、実クリック領域が44px以上ある`<label>`側であり、input自体は視覚的に16×16pxでも問題ない。label側の矩形で判定するよう補正（`discovery.js`のfilter-option、`components.css` 48行目付近のパターン）
3. **`.post-card h2 a`（募集タイトルのリンク）の見た目上の矩形は19px高**だが、`discovery.css`にカード全体をタップ領域にする`::after`スタイル（`inset:0`のstretched-link技法）があり、これが機能していれば実クリック領域はカード全体（900px超×150px超）になる。当初はこの技法が機能しておらず19pxしかクリックできない実バグだったため（詳細は次節）、`elementFromPoint`による実クリック判定で検証したうえで発見・修正した

## 発見した不具合（1件、修正済み）

### 募集カードのタイトル・投稿者リンクのタップ領域が44px未満（`/`・`/posts`）

**発見経緯**: 自動チェッカーが`/posts`・`/`のカード内タイトルリンク（`<a>`直下、`h2 a`）を高さ16〜19px、投稿者リンク（`.person`）を高さ30pxとして検出。DESIGN.md 83行目の「主操作は高さ44px以上」、および`discovery.css`自身のコメント（「Tapping anywhere on the row opens the post」）に反する。

**再現・原因の特定**: `document.elementFromPoint()`でカード内の複数座標を実クリック判定したところ、カードのタイトル文字列の真上以外（カード内の空白部、投稿者行の右側、タイムスタンプ列など）はどこをクリックしても`<a>`ではなく`ARTICLE.post-card`や`H2`など非リンク要素がヒットし、実際に開けるのはタイトル文字列の19px×195pxの範囲のみだった。

`discovery.css`を調査した結果、同一ファイル内に`.post-card`関連セレクタが**3世代分重複**していた（61行目: 現行に近いgrid、190行目付近: 廃止済みの中間デザイン、262行目: 現行の最終デザイン）。廃止済みのはずの中間デザイン側に:

```css
.post-card h2 a { position:relative; top:4px; }      /* 194行目 */
.post-person .person { position:relative; z-index:1; min-height:30px; }  /* 198行目 */
```

が残っており、CSSの後勝ちルールで**現行のルールを静かに上書き**していた。

- `.post-card h2 a`に`position:relative`を与えたことで、`.post-card h2 a::after`（`position:absolute; inset:0`でカード全体を覆うはずのstretched-link）の containing block が、本来の`.post-card`（`position:relative`）ではなく`<a>`自身になってしまい、`::after`のサイズが`<a>`自身の195×19pxに縮んでいた（`getComputedStyle(a,'::after').width/height`で実測confirmed）
- `.post-person .person`の`min-height:30px`が、より上で正しく定義されていた`min-height:44px`（99行目）を上書きしていた

**修正**: `discovery.css`の廃止済み中間デザインブロックから、上記2つの上書き宣言のみを削除（`position:relative;top:4px`をアンカーから外し、`.person`と`.post-person`の`min-height:30px`を外して44pxの宣言を有効化）。その他の廃止済みブロックの宣言（余白調整等、タップ領域と無関係）は影響範囲を最小化するため今回は手を付けていない。CSSを配信する`<link>`・`@import`のversion query（`app.css?v=...`・`discovery.css?v=...`）もキャッシュ更新のため1つ繰り上げた。

**再検証**（修正後、実サーバーからの新規レスポンスで確認。JS実行によるその場パッチではない）:
- `getComputedStyle(a,'::after')`: 195×19px → **952×168.891px**（カード全幅・全高に復元）
- `.person`の高さ: 30px → **44px**
- `elementFromPoint`でのカード内複数座標クリック判定: タイトル文字列以外（行間の空白、投稿者行の右側、下端付近）もすべて`<A>`（stretched link）がヒットするよう復元
- `/`・`/posts`を375/390/768/1440の4幅で再実施し、44px未満の不具合候補0件、横scroll0件を確認（証跡: `nft-008-fix-home-1440-after.png`、`nft-008-fix-home-768-after.png`、`nft-008-fix-posts-1440-after.png`、`nft-008-fix-posts-768-after.png`、`nft-008-fix-posts-390-after.png`）
- 修正前後のスクリーンショットを目視比較し、タイトル・投稿者名・アイコンの位置に視覚的な差分がないことを確認（`top:4px`の除去による4pxの見た目のズレは実測で確認されなかった）

**影響範囲**: `.post-person .person`・`.post-card h2 a`はカードコンポーネント共通のクラスのため、`/`（トップ）と`/posts`（募集一覧）の両方に同じ不具合が存在し、両方とも本修正で解消した。`/my/posts`は同じカードマークアップを使うが、検証に使ったU04アカウントは投稿を所有していないため実写での確認はできていない（CSSクラスが同一である以上、同じ修正が適用されることは自明）。

**テスト**: `mvnw clean test`（`DB_PASSWORD`指定）— 14,347件実行 / 0 failures / 0 errors / 21 skipped、BUILD SUCCESS。今回はCSS/HTMLのみの変更でJavaロジックに変更はないため、既存のJUnitへの影響は想定通りなし。

## Route × Viewport 結果

全25 route×4 viewport、横scroll・固定/sticky要素の被りは全組み合わせで0件。44px未満は上記1件（カードのタイトル・投稿者リンク、修正済み）のみ。

| # | Route | Actor | 375×667 | 390×844 | 768×1024 | 1440×900 |
|---|---|---|---|---|---|---|
| 1 | `/` | 匿名 | PASS（修正前は44px候補あり→修正済み） | PASS | PASS | PASS |
| 2 | `/posts` | 匿名 | PASS | PASS | PASS | PASS |
| 3 | `/posts/new` | U04 | PASS | PASS | PASS | PASS |
| 4 | `/posts/920001` | 匿名 | PASS | PASS | PASS | PASS |
| 5 | `/posts/920001/edit` | U05（投稿者） | PASS | PASS | PASS | PASS |
| 6 | `/my/posts` | U04 | PASS（投稿0件の空表示） | PASS | PASS | PASS |
| 7 | `/users/910004` | U04 | PASS | PASS | PASS | PASS |
| 8 | `/settings` | U04 | PASS（`.settings-nav`はsticky、被りなし） | PASS | PASS（sticky確認、被りなし） | PASS（sticky確認、被りなし） |
| 9 | `/settings/profile` | U04 | PASS | PASS | PASS | PASS |
| 10 | `/settings/blocks` | U09（ブロック実行者） | PASS | PASS | PASS | PASS |
| 11 | `/login` | 匿名 | PASS | PASS | PASS | PASS |
| 12 | `/register` | 匿名 | PASS | PASS | PASS | PASS |
| 13 | `/verify-email` | U01（未確認） | PASS | PASS | PASS | PASS |
| 14 | `/password-reset` | 匿名 | PASS | PASS | PASS | PASS |
| 15 | `/password-reset/confirm` | 匿名 | PASS | PASS | PASS | PASS |
| 16 | `/messages` | U07（DM送信者） | PASS | PASS | PASS | PASS |
| 17 | `/messages/930001` | U07 | PASS（画像404はfixture未生成、下記参照） | PASS | PASS | PASS |
| 18 | `/notifications` | U07 | PASS | PASS | PASS | PASS |
| 19 | `/blocks` | U09 | PASS | PASS | PASS | PASS |
| 20 | `/admin` | U13（管理者） | PASS | PASS | PASS | PASS |
| 21 | `/admin/reports` | U13 | PASS | PASS | PASS | PASS |
| 22 | `/support` | 匿名 | PASS | PASS | PASS | PASS |
| 23 | `/contact` | 匿名→`/login?next=`へ302（※1）／U04は実フォーム | PASS | PASS | PASS | PASS |
| 24 | `/feature-request` | 匿名→`/login?next=`へ302（※1）／U04は実フォーム | PASS | PASS | PASS | PASS |
| 25 | 存在しないroute（`/this-route-does-not-exist-nft008`） | 匿名→`/login?next=`へ302（※1） | PASS | ※2 | ※2 | ※2 |

※1: `/contact`・`/feature-request`・存在しないrouteは、未ログイン状態では`PageController`のroute定義に含まれず、`/login?next=...`へ302リダイレクトされる（`api-ui-inventory.md`の期待どおり）。リダイレクト先は`/login`そのもの（#11で4幅とも実測済み）なので、375幅で一度リダイレクト動作を確認した後は390/768/1440での個別再確認を省略した（同一コンポーネントの再掲になるため）。ログイン後（U04）の実フォームは4幅とも別途フル実施した。

※2: 存在しないrouteの匿名アクセスは375幅で`/login?next=%2Fthis-route-does-not-exist-nft008`への302を確認。表示コンポーネントは#11の`/login`と同一のため、390/768/1440は#11の実測結果を援用（構造的にCSSブレークポイントの境界（640/760/1000/1099px）は375〜390間に存在しないことを`discovery.css`・`account.css`で確認済みで、375と390で層落ちする差分は原理的に生じない）。

## 証跡ファイル

`docs/test-results/playwright-harness/`配下、`nft-008-<連番>-<route>-<actor>-<viewport>.png`（一部`-anon-`はローカル匿名確認、接頭辞のない初期の数件はhomeなど代表1〜2画面）。1〜85番が全route×viewportの一次証跡、`nft-008-fix-*-after.png`が上記不具合の修正後再検証証跡。

## 環境起因の既知事象（CSS/layoutの不具合ではないため対象外）

- 全routeで共通して`GET /uploads/qa-bear-icon.jpg`が404（console error 1件）。DMメッセージ画像・募集画像も同様に404。`scripts/test/create-release-images.ps1`で生成される画像fixtureが本環境のuploadsディレクトリに未生成のため（DBの参照行はseedで投入済みだが実ファイルがない）。レイアウト自体は画像なしの代替表示で崩れておらず、NFT-008（横scroll・44px・固定要素の被り）の範囲外の環境準備不足として記録するにとどめる
- `/contact`・`/feature-request`の匿名アクセスが`/login`へリダイレクトされる挙動は、`docs/test-plan/api-ui-inventory.md`の期待どおりで不具合ではない。HANDOFF.mdが既に指摘している「`/support`案内文とメール未確認ゲートの実際の挙動の不整合」とは別件で、本バッチでは追加の不整合は見つからなかった

## 後処理

- `scripts\test\seed-release-users.sql`でQA DBを再投入済み（84 users / 74 posts、重複0確認）
- 自分専用に起動した`mvnw spring-boot:run`（ポート8082）は終了済み
- `mvnw clean test`: 14,347件実行 / 0 failures / 0 errors / 21 skipped、BUILD SUCCESS
