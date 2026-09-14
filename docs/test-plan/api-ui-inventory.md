# UI・API・永続化インベントリ

状態: 2026-09-14の作業ツリーを静的棚卸し。認可欄はリリース期待値です。

## UI route（25）

|route|匿名|確認済USER|停止USER|ADMIN|主なケース|
|---|---|---|---|---|---|
|`/`、`/posts`|可|可|停止案内|可|ST-027..034、055|
|`/posts/{id}`|可|可|停止案内|可|ST-033/034|
|`/users/{id}`|可|可|停止案内|可|ST-011/012/035|
|`/register`、`/login`|可|可|可|可|ST-001..003/010|
|`/verify-email`|可。tokenまたはsession条件|可|停止案内|可|ST-005..007|
|`/password-reset`、`/password-reset/confirm`|可|可|可|可|ST-008/009|
|`/posts/new`、`/posts/{id}/edit`|loginへ|可|不可|可|ST-020..023|
|`/my/posts`|loginへ|可|不可|可|ST-023..025|
|`/settings`、`/settings/profile`、`/settings/blocks`|loginへ|可|不可|可|ST-013..017/044/045|
|`/messages`、`/messages/{id}`|loginへ|可|不可|可|ST-035..043|
|`/notifications`|loginへ|可|不可|可|ST-042/053|
|`/blocks`|loginへ|可|不可|可|互換route確認、ST-044/045|
|`/admin`、`/admin/reports`|login/403|403|不可|可|ST-047/048|
|`/support`|可|可|可|可|SEC-003、ST-055|
|`/contact`、`/feature-request`|loginへ。header非表示|可|可|可|ST-049..052、SEC-003|

PageControllerは同じHTML shellを返し、`app.js`、`account.js`、`discovery.js`、`community.js` がrouteごとのDOMを構成します。したがってHTMLファイル数ではなく、route、認証状態、API応答の組合せを画面単位として確認します。

## API endpoint

|method/path|期待主体|目的|主なケース|
|---|---|---|---|
|`GET /api/csrf`|全員|CSRF token取得|IT-030、SEC-007|
|`POST /api/auth/register`|匿名|登録とsession開始|UT-026..029、IT-001、ST-001/002|
|`POST /api/auth/login`|匿名|mail login|IT-002/003、ST-003|
|`POST /api/auth/logout`|login|session破棄|IT-002、ST-004|
|`GET /api/auth/me`|login|本人session確認|IT-001/002/009|
|`GET /api/auth/line/enabled`|全員|LINE設定可否|ST-010|
|`GET /api/auth/line/start`、`GET /api/auth/line/callback`|匿名|OAuth stateとlogin|ST-010、SEC-013|
|`POST /api/auth/verify-email`|token所持|mail確認|IT-004、ST-006|
|`POST /api/auth/verify-email/resend`|未確認login|確認再送|IT-005、ST-007|
|`POST /api/auth/password-reset/request`|匿名|再設定依頼|IT-006、ST-008/009|
|`POST /api/auth/password-reset/confirm`|token所持|password更新|IT-007、ST-008|
|`POST /api/auth/withdraw`|login|本人退会|IT-008、UAT-010|
|`GET /api/masters`|全員|4種master取得|UT-029/031、ST-016/022|
|`GET /api/users/me`|login|本人profile|IT-009|
|`PUT /api/users/me`|確認済ACTIVE|profile更新|IT-010、ST-013..016|
|`POST/DELETE /api/users/me/image`|確認済ACTIVE|profile画像差替・削除|IT-011、ST-017|
|`GET /api/users/{id}`|全員|ACTIVE公開profile|IT-009、ST-011/012|
|`POST /api/posts`|確認済ACTIVE|2種投稿作成|IT-012/014、ST-020/021|
|`PUT /api/posts/{id}`|確認済ACTIVE所有者|投稿編集|IT-013、ST-023|
|`PATCH /api/posts/{id}/close`|確認済ACTIVE所有者|募集終了|IT-013、ST-024|
|`PATCH /api/posts/{id}/reopen`|確認済ACTIVE所有者|再公開|IT-013/015、ST-024/025|
|`GET /api/posts`|全員|旧一覧・認証時履歴|IT-021|
|`GET /api/posts/page`|全員|filter/sort/cursor一覧|IT-017..020、ST-027..032|
|`GET /api/posts/mine`|確認済ACTIVE|本人投稿管理|ST-023..025|
|`GET /api/posts/{id}`|全員|投稿詳細|IT-015、ST-033|
|`GET /api/posts/{postId}/images`|全員|投稿画像一覧|ST-026|
|`POST /api/posts/{postId}/images`、`POST .../batch`|確認済ACTIVE所有者|投稿画像追加|IT-016、ST-020/026|
|`PATCH /api/posts/{postId}/images`|確認済ACTIVE所有者|投稿画像順序|IT-016、ST-026|
|`DELETE /api/posts/{postId}/images/{imageId}`|確認済ACTIVE所有者|投稿画像削除|IT-016、ST-026|
|`GET /api/search-history`|確認済ACTIVE本人|直近5件|IT-021、ST-032|
|`POST /api/messages?recipientId=`|確認済ACTIVE|DM送信・会話/通知作成|UT-007..013、IT-022/023、ST-035..041|
|`GET /api/messages/conversations`|確認済ACTIVE|会話一覧|ST-036/041|
|`GET /api/messages/conversation/{id}`|参加者|message一覧|UT-014、ST-041|
|`PATCH /api/messages/conversation/{id}/read`|参加者|既読|UT-015、ST-042|
|`GET /api/messages/conversation/{id}/stream`|参加者|SSE|ST-043|
|`POST /api/messages/images`|確認済ACTIVE|DM画像一時保存|IT-024、ST-038/039|
|`GET /api/messages/images/{name}`|参加者/該当通報ADMIN|非公開画像|IT-024、SEC-010|
|`GET /api/notifications`、`GET .../unread-count`|login本人|通知一覧・未読数|IT-025、ST-053|
|`PATCH /api/notifications/{id}/read`、`PATCH .../read-all`|login本人|既読化|IT-025、ST-053|
|`GET /api/blocks`|確認済ACTIVE本人|block一覧|ST-044/045|
|`POST /api/blocks?userId=`|確認済ACTIVE本人|block|UT-023/024、IT-026|
|`DELETE /api/blocks/{userId}`|確認済ACTIVE本人|解除|UT-025、IT-026|
|`POST /api/reports`|確認済ACTIVE|POST/USER/MESSAGE通報|IT-027、ST-046、SEC-016|
|`POST /api/feedback/contact`|login。停止中も可|問い合わせ|UT-001..006、IT-029、ST-049|
|`POST /api/feedback/feature-request`|login。停止中も可|機能要望|IT-029、ST-050|
|`POST /api/feedback/images`|login。停止中も可|feedback画像|ST-049、SEC-011/012|
|`GET /api/admin/reports`、`PATCH .../{id}`|ADMIN|通報一覧・状態|IT-028、ST-047/048|
|`DELETE /api/admin/posts/{id}`|ADMIN|投稿非公開|ST-048|
|`PATCH /api/admin/users/{id}/suspend`、`.../unsuspend`|ADMIN|停止・解除|ST-048|
|`GET /api/admin/feedback`|ADMIN|問い合わせ・要望一覧|IT-028、ST-052|
|`POST /api/uploads`|確認済ACTIVE。互換用途を要確認|汎用画像保存|SEC-012|
|`GET /uploads/**`|公開投稿/profile画像だけ|静的画像。DM/feedbackへの迂回禁止|SEC-010/011|

## DB・filesystem

|分類|対象|
|---|---|
|本人・認証|`users`, `email_verification_tokens`, `password_reset_tokens`, `line_accounts`|
|プロフィール関連|`user_parts`, `user_genres`, `user_stances`, `user_prefectures`|
|募集|`posts`, `post_parts`, `post_genres`, `post_stances`, `post_prefectures`, `post_age_ranges`, `post_images`|
|発見|`search_histories`|
|会話|`conversations`, `messages`, `notifications`|
|安全・運営|`blocks`, `reports`, `feedback`|
|master|`parts`, `genres`, `stances`, `prefectures`|
|file|`BANDLINK_UPLOAD_DIR` 配下。DB URLと実fileの存在・削除・孤児を双方向照合|

DB制約はentity annotationだけを信用せず、実際のPostgreSQL catalogと `NFT-013` の違反transactionで確認します。`ddl-auto=update` は既存CHECK制約を更新しないため、新規DBと移行済み相当DBの双方が必要です。
