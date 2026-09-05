# Band Link — DB・API・権限・状態遷移 設計書

- 状態: 設計案（実装前）。2026-09-05時点。
- 前提: `requirements.md`（版0.2）+ `docs/decisions/0001〜0004`を確定事項として反映。
- 本書の役割: requirements.md 14章「3. DB・API・権限・状態遷移を具体設計する」に対応する成果物。
- 11章の保留事項は、本書内で判断理由付きで解消する（解消箇所には「(11章対応)」と注記）。未確定のまま残す項目は末尾「未解決事項」にまとめる。

## 1. DBスキーマ

### 1.1 users

| カラム | 型 | 制約 | 備考 |
|---|---|---|---|
| id | bigint | PK | |
| username | varchar | NOT NULL | 表示名 |
| email | varchar | NOT NULL, UNIQUE | ログインID |
| password_hash | varchar | NOT NULL | BCrypt |
| email_verified_at | timestamp | NULL可 | NULLなら未確認。投稿・送信を禁止する判定に使用 |
| status | enum | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE / SUSPENDED / WITHDRAWN |
| role | enum | NOT NULL, DEFAULT 'USER' | USER / ADMIN（付与方法は§4参照） |
| age | int | NULL可 | 生の年齢。公開時は年代のみ表示（アプリ側で算出） |
| gender | varchar | NULL可 | |
| bio | text | NULL可、最大1000文字 | 自己紹介（4章） |
| experience_years | int | NULL可 | |
| video_url | varchar | NULL可 | |
| profile_image_url | varchar | NULL可 | |
| last_edited_at | timestamp | NULL可 | 直近の投稿作成・編集日時。12時間ロック判定に使用（新規/編集用） |
| last_rank_boosted_at | timestamp | NULL可 | 直近の再公開による順位アップ日時。12時間に1回の判定に使用。last_edited_atとは別管理（矛盾点の解消） |
| created_at | timestamp | NOT NULL | |

**プロフィールの活動エリア(11章対応)**: 募集投稿と同じ「都道府県から3つまで」を採用し、`user_prefectures`で管理する。市区町村・駅の自由記入はプロフィールには設けない（検索対象ではなくプロフィール表示用途のため、投稿ほどの精度は不要と判断）。

**年齢の保存方法(11章対応)**: 生年ではなく年齢そのものを保存する現行方式を維持する。年ごとの自動更新は行わず、ユーザー自身がプロフィール編集で更新する前提（既存挙動を踏襲、シンプルさを優先）。

### 1.2 マスタ系（parts / genres / stances / prefectures）

| カラム | 型 | 制約 | 備考 |
|---|---|---|---|
| id | bigint | PK | |
| name | varchar | NOT NULL | |
| display_order | int | NOT NULL | 選択肢の表示順（9章「後から追加できる設計」に対応、追加時は末尾の番号を採番） |

`prefectures`は47都道府県を初期データとして投入する（新規マスタ、9章の初期マスタ一覧には無いため追加）。

### 1.3 join tables

`user_parts` / `user_genres` / `user_stances` / `user_prefectures`
`post_parts` / `post_genres` / `post_stances` / `post_prefectures`
`post_age_ranges`（post_id, age_range） — age_rangeは固定値enum: `10S, 20S, 30S, 40S, 50S_PLUS, ANY`（「年齢不問」= `ANY`）

いずれも(左カラム, 右カラム)の複合PKのみ、追加カラムなし。

### 1.4 posts

| カラム | 型 | 制約 | 備考 |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | NOT NULL, FK | |
| type | enum | NOT NULL | MEMBER_WANTED（メンバー募集） / WANTS_TO_JOIN（参加希望） |
| title | varchar(100) | NOT NULL | 文字数上限は実装案（11章対応、下記§5参照） |
| content | text | NOT NULL, 最大2000文字 | 実装案（11章対応） |
| area_sub | varchar(100) | NULL可 | 市区町村・駅など自由記入 |
| activity_frequency | enum | NOT NULL | WEEKLY_2PLUS / WEEKLY_1 / MONTHLY_2_3 / MONTHLY_1 / IRREGULAR / NEGOTIABLE |
| status | enum | NOT NULL, DEFAULT 'OPEN' | OPEN / CLOSED（一覧表示可否はこれだけを見る） |
| closed_reason | enum | NULL可 | MANUAL / EXPIRED / WITHDRAWN / SUSPENDED / DELETED_BY_ADMIN（8章「通常の終了と区別する」に対応） |
| created_at | timestamp | NOT NULL | |
| updated_at | timestamp | NOT NULL | 本文・条件・画像の変更で更新 |
| closed_at | timestamp | NULL可 | |
| expires_at | timestamp | NOT NULL | created_at（or 再公開時）+30日。バッチ or 参照時判定でEXPIREDに倒す（§3.2参照） |
| rank_updated_at | timestamp | NOT NULL | 一覧の並び順キー。新規作成時=created_at、順位アップ時=now()（11章「再公開の順位更新基準」対応） |

**業務ルールの実装方針:**
- 「同時公開は1ユーザー1件まで」は、`(user_id) WHERE status='OPEN'`の部分ユニークインデックスでDBレベルに強制する（11章「並行操作時の公開件数制限」対応。アプリ側のcheck-then-writeだけだと競合状態で2件同時OPENが作れてしまうため）。
- 新規作成・編集（本文・条件・画像すべて含む）は`users.last_edited_at`から12時間経過を条件に許可し、成功時に`last_edited_at`を更新する。
- 再公開は`status`を`OPEN`に戻し`expires_at`を+30日更新するだけなら常に可能（editロックの対象外、既存確定事項どおり）。ただし「順位を上げる」動作は`users.last_rank_boosted_at`から12時間経過している場合のみ`rank_updated_at`を更新し、経過していなければ`rank_updated_at`は変更しない（再公開自体は成功する)。

### 1.5 post_images

| カラム | 型 | 制約 | 備考 |
|---|---|---|---|
| id | bigint | PK | |
| post_id | bigint | NOT NULL, FK | |
| image_url | varchar | NOT NULL | |
| sort_order | int | NOT NULL | 並べ替え編集に対応 |
| created_at | timestamp | NOT NULL | |

最大5枚は アプリ層でカウントチェック。

### 1.6 conversations / messages

`conversations`: id, user_a_id, user_b_id, created_at, last_message_at
- 2人の組は常に`user_a_id < user_b_id`になるよう保存し、`UNIQUE(user_a_id, user_b_id)`で1組1会話を強制する。

`messages`: id, conversation_id, sender_id, content(NULL可,最大2000文字), image_url(NULL可), created_at, **read_at**(timestamp, NULL可)
- 既存実装は`read`真偽値だったが、モック画面(`band-link-preview.html`)が「既読 14:24」と既読**時刻**を表示する設計になっているため、`read`→`read_at`(nullable timestamp)に変更する。

### 1.7 notifications

id, user_id, type(enum: NEW_MESSAGE, 将来拡張用に他の値を追加可能), content, related_id, read_at(NULL可), created_at

### 1.8 search_histories

| カラム | 型 | 備考 |
|---|---|---|
| id | bigint | |
| user_id | bigint | |
| conditions | jsonb | type・prefectures[]・parts[]・genres[]・stances[]・keyword・age_ranges[]・activity_frequency をまとめて保持 |
| conditions_hash | varchar | 配列を正規化(ソート)した上でのハッシュ。重複判定に使用 |
| searched_at | timestamp | |

**理由**: 検索条件が複数の多値項目（都道府県・パート・ジャンル等）を含み正規化テーブルにすると結合が煩雑になるため、JSON列＋ハッシュでの重複検出を採用する。「直近5件・重複は最新へ移動」は`conditions_hash`が一致する既存行があれば`searched_at`を更新、なければ新規追加した上で6件目以降を削除、で実現する。

### 1.9 blocks

id, blocker_id, blocked_id, created_at。`UNIQUE(blocker_id, blocked_id)`。

### 1.10 reports

| カラム | 型 | 備考 |
|---|---|---|
| id | bigint | |
| reporter_id | bigint | |
| target_type | enum | POST / USER / MESSAGE |
| target_id | bigint | |
| reason_text | text | |
| content_snapshot | text NULL可 | MESSAGE通報時のみ、対象メッセージの本文・画像URLをこの時点でコピー保存（8章「前後の会話は含めない」に対応。元メッセージが後で編集・削除されても通報時点の内容を保持する目的も兼ねる） |
| status | enum | PENDING / REVIEWED / DISMISSED / ACTIONED |
| reviewed_by | bigint NULL可, FK→users | |
| reviewed_at | timestamp NULL可 | |
| created_at | timestamp | |

### 1.11 email_verification_tokens / password_reset_tokens

共通構造: id, user_id, token(varchar, UNIQUE, ランダム), expires_at, used_at(NULL可), created_at。有効期限は実装案として24時間を提案。

## 2. 権限設計

| ロール | 説明 |
|---|---|
| 未ログイン | 募集一覧・詳細・公開プロフィールの閲覧のみ |
| USER（メール確認済み） | 投稿・編集・メッセージ送信・ブロック・通報が可能 |
| USER（メール未確認） | 閲覧のみ。投稿・送信系は全て403 + 「メール確認が必要」エラー |
| ADMIN | 通報確認・投稿削除・利用停止／解除。一般ユーザー向けエンドポイントも通常どおり利用可 |

**管理者権限の付与方法(11章対応)**: 自己申請の仕組みは設けない。`users.role`を運営者がDB上で直接`ADMIN`に変更する運用とする（招待制・自動昇格なし）。初期は運営者本人のアカウントのみ。

**エンドポイント単位の認可ルール（抜粋、詳細は§3のAPI一覧に記載）:**
- 投稿の編集・削除・終了・再公開・画像操作 → 投稿者本人のみ
- 会話の閲覧・既読操作・メッセージ送信 → 会話参加者のみ
- 通報対応・投稿削除（運営）・利用停止/解除 → ADMINのみ
- ブロック関係にある相手への投稿・返信操作 → 送信元がブロックされている/している場合は`POST /api/messages`を403

## 3. 状態遷移

### 3.1 ユーザー状態（`users.status`）

```
ACTIVE --(本人操作: 退会)--> WITHDRAWN（不可逆）
ACTIVE --(管理者操作: 利用停止)--> SUSPENDED
SUSPENDED --(管理者操作: 解除)--> ACTIVE
```
WITHDRAWNからの復帰なし。SUSPENDED/WITHDRAWN中は投稿・メッセージ送信・プロフィール公開を停止する（[[0001]]の表示ルールが適用される）。

### 3.2 投稿状態（`posts.status` + `closed_reason`）

```
OPEN --(本人: 終了)--> CLOSED(reason=MANUAL)
OPEN --(30日経過、参照時 or バッチ判定)--> CLOSED(reason=EXPIRED)
OPEN --(本人が退会)--> CLOSED(reason=WITHDRAWN)
OPEN --(本人が利用停止)--> CLOSED(reason=SUSPENDED)
OPEN --(管理者が削除)--> CLOSED(reason=DELETED_BY_ADMIN)

CLOSED(reason=MANUAL or EXPIRED) --(本人: 再公開、かつ他にOPENな投稿がない)--> OPEN
CLOSED(reason=WITHDRAWN or SUSPENDED or DELETED_BY_ADMIN) --> 再公開不可
```

**expires_atの判定方式**: バッチジョブを別途起動せず、一覧取得・詳細取得のタイミングで`expires_at < now()`かつ`status='OPEN'`の投稿を`CLOSED(EXPIRED)`に遷移させる遅延評価方式を提案する（インフラがシンプルになる）。正確性を優先するなら日次バッチ方式に変更可能、要検討。

### 3.3 通報状態（`reports.status`）

```
PENDING --(管理者: 対応不要と判断)--> DISMISSED
PENDING --(管理者: 対象を削除/利用停止等の措置)--> ACTIONED
PENDING --(管理者: 確認のみ完了)--> REVIEWED
```

## 4. API仕様（概要）

既存の`band-recruitment`との主な違い: 未ログイン閲覧の許可、単一`area`→都道府県配列+自由記入への変更、検索・ページングをDB側で実施（旧実装は全件取得後にJavaでフィルタしていたが変更する、requirements.md 2章の確定事項）。

### 4.1 認証・アカウント（`/api/auth/**`）

| メソッド | パス | 認証 | 備考 |
|---|---|---|---|
| POST | /api/auth/register | 不要 | 登録直後は`email_verified_at`がNULL |
| POST | /api/auth/verify-email | 不要 | トークン検証、成功で`email_verified_at`設定 |
| POST | /api/auth/login | 不要 | |
| POST | /api/auth/logout | 要 | |
| GET | /api/auth/me | 要 | |
| POST | /api/auth/password-reset/request | 不要 | メール送信（トークン発行） |
| POST | /api/auth/password-reset/confirm | 不要 | トークン検証+新パスワード設定 |
| POST | /api/auth/withdraw | 要 | 退会（確認ダイアログ等はUI側） |

### 4.2 プロフィール（`/api/users/**`）

| メソッド | パス | 認証 | 備考 |
|---|---|---|---|
| GET | /api/users/me | 要 | |
| PUT | /api/users/me | 要 | 自分のプロフィール情報を更新。メール未確認でもプロフィール編集は可能。投稿・メッセージ送信だけがメール確認必須 |
| POST | /api/users/me/profile-image | 要 | 5MB上限、jpg/png/webpのみ（11章対応、§5参照） |
| GET | /api/users/{id} | 不要 | 公開プロフィール。対象がSUSPENDED/WITHDRAWNなら404相当の非公開表示 |

### 4.3 募集投稿（`/api/posts/**`）

| メソッド | パス | 認証 | 備考 |
|---|---|---|---|
| GET | /api/posts | 不要 | クエリ: type, prefectures[], parts[], genres[], stances[], ageRanges[], activityFrequency, keyword, cursor, limit。DB側でページング（無限スクロール用にカーソル方式を提案） |
| GET | /api/posts/{id} | 不要 | 通常終了（MANUAL/EXPIRED）は本文・画像と「募集終了」を返す。退会・利用停止・管理者削除による非公開は本文・画像を返さず非公開表示とする。ブロック関係でも到達可（[[0004]]） |
| POST | /api/posts | 要（メール確認要） | 業務ルールは§1.4参照 |
| PUT | /api/posts/{id} | 要、本人のみ | |
| DELETE | /api/posts/{id} | 要、本人のみ | |
| POST | /api/posts/{id}/images | 要、本人のみ | 5枚上限、5MB/枚 |
| PATCH | /api/posts/{id}/close | 要、本人のみ | |
| PATCH | /api/posts/{id}/reopen | 要、本人のみ | |

ログイン中のみ、レスポンスからブロック相手の投稿を除外する（8章）。

### 4.4 検索履歴（`/api/search-history`）

| メソッド | パス | 認証 |
|---|---|---|
| GET | /api/search-history | 要 |

`GET /api/posts`実行時、ログイン中かつ何らかの検索条件（type/keyword/prefectures/parts/genres/stances/ageRanges/activityFrequency のいずれか）が指定されていた場合のみ記録する。ページング目的の`cursor`と`limit`だけが指定された場合は記録しない。同じ正規化済み条件（typeを含む）は重複させず最新へ移動する。

### 4.5 メッセージ・会話（`/api/conversations/**`, `/api/messages`）

| メソッド | パス | 認証 | 備考 |
|---|---|---|---|
| GET | /api/conversations | 要 | |
| GET | /api/conversations/{id}/messages | 要、参加者のみ | |
| PATCH | /api/conversations/{id}/read | 要、参加者のみ | |
| POST | /api/messages | 要（メール確認要）、ブロック関係なし | |
| GET | /api/messages/{id}/image | 要、参加者のみ | **新設**。メッセージ画像は`/uploads/**`の`permitAll`から外し、このエンドポイント経由でのみ配信する（11章「会話画像の参加者認可」対応。現行は静的ファイル配信で誰でもURLを知れば閲覧できてしまうため） |

### 4.6 通知（`/api/notifications/**`）

既存同様。加えて未読件数取得`GET /api/notifications/unread-count`をポーリング用に追加提案（11章「自動更新間隔」対応、§5参照）。

### 4.7 ブロック（`/api/blocks`）

| メソッド | パス | 認証 |
|---|---|---|
| GET | /api/blocks | 要 |
| POST | /api/blocks | 要 |
| DELETE | /api/blocks/{userId} | 要 |

### 4.8 通報（`/api/reports`）

| メソッド | パス | 認証 |
|---|---|---|
| POST | /api/reports | 要 |

### 4.9 管理者（`/api/admin/**`）

| メソッド | パス | 認証 |
|---|---|---|
| GET | /api/admin/reports | ADMIN |
| PATCH | /api/admin/reports/{id} | ADMIN |
| PATCH | /api/admin/users/{id}/suspend | ADMIN |
| PATCH | /api/admin/users/{id}/unsuspend | ADMIN |
| DELETE | /api/admin/posts/{id} | ADMIN |

## 5. 実装案として提示する数値（11章対応）

| 項目 | 提案値 | 備考 |
|---|---|---|
| 投稿タイトル文字数上限 | 100文字 | |
| 投稿本文文字数上限 | 2,000文字 | |
| メッセージ本文文字数上限 | 2,000文字 | |
| 活動エリア自由記入上限 | 100文字 | |
| 画像許可形式 | jpg, png, webp | 拡張子だけでなくマジックバイト検証も行う（11章「画像実体検証」対応） |
| 1ページ取得件数 | 20件 | 無限スクロールのカーソル方式と併用 |
| メッセージ・通知の自動更新間隔 | 15秒ポーリング | WebSocket/SSEは将来検討、初期はシンプルなポーリングを提案 |
| トークン有効期限（メール確認・PW再設定） | 24時間 | |

## 6. 未解決事項（本書でも解消しなかったもの）

- 公開環境・ドメイン・実メール配信サービスの選定（requirements.md 11章に記載の通り、ローカル確認後に判断）
- Elasticsearch/Kibanaの収集方式・バージョン・保持期間・Docker構成（別途インフラ設計で確定する。本書はDB/API/権限が対象のため詳細を扱わない）
- CSRF保護の有効化方式（Thymeleaf側フォームへのトークン組み込み、`/api/**`側の扱い）は実装時に具体化
- 利用停止・退会・通報に関する運営向け問い合わせ先の実際の連絡先（内容/文言の話であり設計事項ではないため、確定した連絡先が決まり次第、画面文言に反映）

---

次のステップ（requirements.md 14章の4）は、ブランチ運用・ローカル環境・JUnit・Playwright MCP・ログ収集の土台構築です。この設計書の内容に問題なければ、そちらに進みます。
