# ST（Playwright MCP）— 2026-09-14 PW-B（ST-011..019: プロフィール・画像・media・活動表示）

`docs/test-plan/playwright-mcp-spec.md` のバッチ定義 PW-B に対応。公式 `@playwright/mcp`（`mcp__playwright__*`）で実施。PW-A（ST-001..010、コミット `357fe5e`）の続き。

## 実施環境

- 専用ビルド起動: `mvnw.cmd spring-boot:run`、`SERVER_PORT=8083`、`SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/band_link_release_test`、`SPRING_JPA_HIBERNATE_DDL_AUTO=none`
- 専用DB `band_link_release_test` を `scripts/test/seed-release-users.sql` で再投入（`ReleasePasswordHash.java` で生成した使い捨てハッシュ。平文はリポジトリに保存しない）
- 画像fixtureは `scripts/test/create-release-images.ps1 -Destination uploads` で生成（Windows PowerShell 5.1がUTF-8/BOMなしの`.ps1`を誤読するため、`Get-Content -Raw -Encoding UTF8` で読み込んだ文字列から`[ScriptBlock]::Create`して実行する回避策が必要だった）
- viewport 1440×900（デスクトップのみ）

## 結果サマリ

| ケース | 結果 | 備考 |
|---|---|---|
| ST-011 公開プロフィールの連絡判断情報 | PASS | U04(910004)の`/users/910004`で名前・年齢・性別・経験・地域・part・genre・stance・bio・画像すべて表示、`/api/users/910004`のpublic APIも整合 |
| ST-012 未完成・停止・退会プロフィール | PASS（**バグ1件発見・修正**） | 詳細下記。修正後は3ケースとも安全に非開示 |
| ST-013 プロフィール全必須項目の編集保存・再読込保持 | PASS | U03(910003)で全必須項目を入力→保存→reloadで同値表示。DB(`age=25,experience_years=3,gender=男性,user_prefectures`)を照合 |
| ST-014 年齢・経験の自由入力とspinner非表示 | PASS（**バグ1件発見・修正**） | 詳細下記。境界値0/120/0/100すべて保存確認 |
| ST-015 性別二択の単一選択 | PASS | `ArrowLeft`キーで女性→男性へ切替、2つ同時選択不可（native radio group）。不要な青枠残留なし |
| ST-016 地域最大3件と各マスタ必須 | PASS | 4件目選択時に「活動エリアは3つまで選択できます。」、0件保存時に「活動エリアを1つ以上選択してください。」で画面上に明示的に停止。1〜3件は保存成功 |
| ST-017 profile画像の選択・preview・差替え・削除 | PASS（**バグ1件発見・修正**） | 詳細下記。正常画像(png/jpg)は反映・旧画像削除、不正画像(空・svg・拡張子偽装・破損)は選択取消かAPI 400で拒否、orphanファイルなしを確認 |
| ST-018 media URLの安全表示 | PASS（**バグ1件発見・修正**） | 詳細下記。YouTube(短縮url含む)/TikTok/Apple Musicは正しくembed、`javascript:`はDOMに一切出力されずXSS発生なし |
| ST-019 オンライン中・最終ログイン表示 | PASS（**バグ1件発見・修正**） | 詳細下記。修正後、U05(オンライン1分前)はprofile/listing両方で「オンライン中」、U06(10分前)は5分枠外のため活動テキストへフォールバック |

## 発見した不具合（修正済み）

### 1. 停止・退会・存在しないプロフィールが「入力内容を確認してください」という誤ったエラーになる（`ProfileService.getPublic`）

- 修正前: `getPublic`が存在しない/非ACTIVEユーザーに対し`IllegalArgumentException`を投げ、`ApiExceptionHandler`の汎用ハンドラで`400 INVALID_INPUT`「入力内容を確認してください。」になっていた。画面は「プロフィールを表示できません。入力内容を確認してください。」と表示し、URLをただ開いただけの閲覧者に「あなたの入力が間違っている」という的外れな案内を出していた
- 修正後: 同じ理由で`NoSuchElementException`を投げるよう変更。同じハンドラ内に既存の`404 NOT_FOUND`「対象が見つかりません。」ハンドラ（他の箇所で使われている確立済みパターン）に自然に合流し、画面は「プロフィールを表示できません。対象が見つかりません。」になる。ステータスコードも意味的に正しい404へ
- 停止(U11)・退会残存(U24)・存在しないID(999999)いずれも同一の非開示メッセージになることを確認（アカウントの有無や状態を外部に漏らさない）
- 回帰テスト: `src/test/java/com/example/bandlink/service/ProfileServiceTest.java`

### 2. 設定画面の年齢・経験年数inputにブラウザ標準のspinner矢印が出る（`account.css`のスコープ漏れ）

- 修正前: 数値inputのspinner非表示CSS（`appearance:textfield`等）が`.register-page`セレクタにしかスコープされておらず、登録画面では正しく非表示（PW-AのST-002で確認済み）だが、`/settings/profile`は`.settings-page`クラスのため対象外で、年齢・経験年数フィールドに矢印が出ていた（`getComputedStyle`で`appearance:auto`を確認）
- 修正後: ルールを`.register-page`スコープから外し、`components.css`の共通input定義の直後にアプリ全体向けとして移設。`getComputedStyle`で`appearance:textfield`になったことを確認し、境界値0/120/0/100すべて矢印なしで入力・保存できることを確認

### 3. 空(0byte)の画像ファイルを選択すると「保存すると公開されます」と表示されるが実際は無言で保存されない（`account.js`）

- 修正前: `imageInput`の`change`ハンドラは5MB超過とMIME不一致だけを弾き、0byteファイルはチェックしていなかった。0byteファイルの`type`は拡張子から`image/png`等と推定されるため素通りし、`qa-empty.png — 保存すると公開されます`という確定的な文言が出た。しかし保存処理側（`if(image instanceof File&&image.size){...}`）は`size`が0だと無言でアップロードをスキップしており、ユーザーには何のエラーも出ないまま画像が更新されない「サイレント失敗」になっていた
- 修正後: `change`ハンドラに`!image.size`のチェックを追加し、他の不正ファイルと同様に選択直後へ「このファイルは空です。別の画像を選んでください。」を表示して選択を取り消すよう変更。以後「保存すると公開されます」という表示は実際に公開される画像だけに限定される
- 他の不正画像（svg・拡張子偽装・破損PNG・5MB超過）は個別に確認し、svg/5MB超過はクライアント側で即座に拒否、拡張子偽装(PNGの中身をjpgと詐称)と破損PNGはクライアントを通過するがサーバー側`POST /api/users/me/image`が`400`で拒否、DB・filesystemに旧画像以外のorphanファイルが残らないことを確認

### 4. 実際と異なるサービスのURLを貼ると偽の「Spotifyで開く」等のリンクになる（`account.js`のmedia表示）

- 修正前: 公開プロフィールのmedia欄は、入力フィールドの名前（例:「Spotify URL」）をそのままリンクの見出しと文言（「Spotifyで開く」）に使っていた。ホスト名の妥当性は`mediaEmbed()`（埋め込み用）でしか検証しておらず、埋め込みできないURLはそのまま「(フィールド名)で開く」という信頼を装うリンクとして出力していた。実際に`spotifyUrl`に`https://evil.com/not-spotify`を保存すると、公開プロフィールに「Spotifyで開く」というリンクが表示され、クリックすると無関係な外部サイトへ遷移した
- 修正後: 既存の`mediaProvider(url)`（ホスト名からサービスを判定する関数。修正前は未使用のデッドコードだった）を使い、リンクの見出し・文言は常に実際のURLホストから判定した名称（不明なホストは汎用の「リンク」）にするよう変更。正しいSpotify/YouTube等のURLは引き続き正しいサービス名で表示され、ホストが一致しないURLは「リンクで開く」という中立表示になることを確認
- `javascript:`スキームのURLは元から`mediaHref()`のプロトコルチェックでDOMに一切出力されないことを別途確認済み（XSSは修正前から発生していない）

### 5. 個人プロフィール画面(`/users/{id}`)で「オンライン中」が一切表示されない（`ProfileResponse`が`lastLoginAt`しか見ていない）

- 修正前: `ProfileResponse.from`は`ActivitySignal.of(u.getLastLoginAt())`（日単位の粗い文言）だけを返し、`ActivitySignal.isOnline(lastSeenAt)`（5分以内の直近アクセスを判定する関数）を一切呼んでいなかった。一方、投稿一覧(`PostResponse`)は`authorOnline`フィールドで正しく`isOnline(lastSeenAt)`を使っていたため、同じユーザーが一覧では「オンライン中」、プロフィール本人ページでは「1週間以内にログイン」等の古い文言という食い違いが生じていた
- 再現: U05(`last_seen_at`=T0-1分、`last_login_at`=T0-5日)で`/api/users/910005`を直接確認すると`activity:"1週間以内にログイン"`のみで`online`フィールド自体が存在しなかった
- 修正後: `ProfileResponse`に`online`フィールドを追加し、`PostResponse.online()`と同じ規則（`UserStatus.ACTIVE`かつ`isOnline(lastSeenAt)`）で計算。フロント側(`account.js`)の「最近の活動」表示も`p.online`を優先し「オンライン中」を表示、そうでなければ従来の`p.activity`文言にフォールバックするよう変更
- 修正後、U05は`/users/910005`（プロフィール）と`/posts`（一覧）の両方で「オンライン中」表示に統一されたことを確認。U06(`last_seen_at`=T0-10分、5分枠外)は`online:false`のまま「1週間以内にログイン」相当の文言にフォールバックすることも確認し、境界を跨いで過剰にオンライン表示されないことも確認
- 回帰テスト: `src/test/java/com/example/bandlink/dto/ProfileResponseTest.java`

## 検証

- `mvnw.cmd -B clean test`（`DB_PASSWORD`を設定の上、`band_link`実DBへ接続する`@SpringBootTest`系も含む）: 14,317件実行 / 0 failures / 0 errors / 15 skipped

## 未実施・持ち越し

- モバイル/タブレット幅（390×844、375×667、768×1024）での再実施は未着手（PW-Aから継続の持ち越し）
- PW-C以降（投稿作成・検索・DM等）は本バッチの後で継続
