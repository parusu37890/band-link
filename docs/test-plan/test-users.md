# リリースQA固定ユーザー・データ

状態: **作成済み・未投入**。すべて架空データです。

## 共通ルール

- 専用DB `band_link_release_test` 以外へはseedできません。
- メールは予約ドメイン `example.test`、表示名は `QA_RELEASE_`、本文は `QA_RELEASE` を接頭辞にします。
- 共通パスワードはリポジトリに保存しません。Runaが `QA_RELEASE_PASSWORD` に8〜72文字の使い捨て値を設定し、`ReleasePasswordHash.java` でbcryptを生成します。
- 時刻はseed実行時の `T0` を `qa_release_fixture.anchor` に記録し、オンライン、12時間、掲載期限の境界を相対時刻で作ります。
- `910012` は退会完了後の状態を表すため、意図的に存在しません。`910024` は旧仕様互換確認用のWITHDRAWN残存行です。
- ケースがユーザー・投稿・会話を更新した場合、次のケースの前にseedを再投入します。SQLは専用DB内のアプリ行を全置換します。

## 指定されたテストユーザー

|記号|ID|メール|表示名|役割・状態|固定データと用途|
|---|---:|---|---|---|---|
|U01|910001|qa-release-unverified@example.test|QA_RELEASE_unverified|USER / ACTIVE / メール未確認|有効・期限切れ・使用済み確認token。未確認ゲート|
|U02|910002|qa-release-general@example.test|QA_RELEASE_general|USER / ACTIVE / 確認済み|通常ログイン、再設定token、一般操作|
|U03|910003|qa-release-incomplete@example.test|QA_RELEASE_incomplete|USER / ACTIVE / 確認済み|年齢・性別・経験・全マスタ関連なし|
|U04|910004|qa-release-complete@example.test|QA_RELEASE_complete|USER / ACTIVE / 確認済み|全プロフィール、3都府県、1000文字自己紹介、画像、問い合わせ・要望|
|U05|910005|qa-release-wanted@example.test|QA_RELEASE_wanted|USER / ACTIVE / 確認済み|メンバー募集の公開投稿者、オンライン1分前|
|U06|910006|qa-release-join@example.test|QA_RELEASE_join|USER / ACTIVE / 確認済み|参加希望の公開投稿者、最終アクセス10分前|
|U07|910007|qa-release-sender@example.test|QA_RELEASE_sender|USER / ACTIVE / 確認済み|DM送信者、未読・通報・複数会話|
|U08|910008|qa-release-receiver@example.test|QA_RELEASE_receiver|USER / ACTIVE / 確認済み|DM受信者、画像付き受信、未読通知|
|U09|910009|qa-release-blocker@example.test|QA_RELEASE_blocker|USER / ACTIVE / 確認済み|U10をブロック済み|
|U10|910010|qa-release-blocked@example.test|QA_RELEASE_blocked|USER / ACTIVE / 確認済み|U09からブロック済み|
|U11|910011|qa-release-suspended@example.test|QA_RELEASE_suspended|USER / SUSPENDED / 確認済み|停止時の画面・API権限、停止理由投稿|
|U12|910012|qa-release-withdrawn@example.test|行なし|退会完了|退会後に個人・関連データが物理削除される期待状態|
|U13|910013|qa-release-admin@example.test|QA_RELEASE_admin|ADMIN / ACTIVE / 確認済み|通報、問い合わせ、停止・解除管理|
|U14|910014|qa-release-images@example.test|QA_RELEASE_images|USER / ACTIVE / 確認済み|投稿画像5枚、プロフィール・DM・feedback画像|
|U15|910015|qa-release-age-min@example.test|QA_RELEASE_age-min|USER / ACTIVE / 確認済み|年齢0、経験0の下限|
|U16|910016|qa-release-age-max@example.test|QA_RELEASE_age-max|USER / ACTIVE / 確認済み|年齢120、経験100、上限|

## 追加の状態遷移ユーザー

|記号|ID|用途|
|---|---:|---|
|U17|910017|最終編集1時間前。編集12時間制限中|
|U18|910018|最終編集13時間前。編集可能|
|U19|910019|順位更新1時間前。再公開しても順位更新不可|
|U20|910020|順位更新13時間前。再公開で順位更新可能|
|U21|910021|期限がT0の1分後。期限境界と遅延失効|
|U22|910022|退会処理対象。投稿、会話、通知、画像、問い合わせを保持|
|U23|910023|DM通報の第三者・IDOR確認|
|U24|910024|旧仕様のWITHDRAWN残存行。公開DTO匿名化・ログイン拒否|
|U25|910025|LINE連携済み、プロフィール未完成|

## ページング用データ

`U101..U160`（ID `910101..910160`）がそれぞれ1件の公開投稿 `P101..P160`（ID `920101..920160`）を持ちます。表示名、メール、投稿タイトルは3桁連番で一意です。募集種別、都道府県、パート、ジャンル、活動頻度を規則的に交互配置し、同順位、欠落、重複、カーソル境界を確認できます。

## 投稿・会話・管理データ

|範囲 / 主なID|内容|
|---|---|
|P001 `920001`|U05の公開メンバー募集。複数パート・3都府県|
|P002 `920002`|U06の公開参加希望。本文だけに固有語「藍色セッション」|
|P003/P004|編集可能・編集制限中|
|P005/P006|手動終了・期限終了。再公開条件|
|P007|T0+1分で期限切れになる公開投稿|
|P008|停止による終了|
|P009|画像5枚の公開投稿|
|P010/P011|ブロック当事者の公開投稿|
|P012|退会処理対象の投稿|
|P013/P014|手動終了、管理者削除による終了|
|C001 `930001`|U07とU08。本文3件、未読、DM画像あり|
|C002 `930002`|U09とU10。ブロック前の既存会話|
|C003 `930003`|U07とU11。停止前の既存会話|
|C004 `930004`|U07とU22。退会削除確認|
|R001..R004|MESSAGE/POST/USER、PENDING/REVIEWED/ACTIONED/DISMISSED|
|F001..F003|問い合わせ、機能要望、退会対象者の問い合わせ|

## 画像fixture

`scripts/test/create-release-images.ps1` が専用アップロードディレクトリに固定名を作ります。

- 正常: PNG、JPEG、WebP、5MBちょうど
- 異常: 0byte、5MB+1byte、SVG、拡張子と実体不一致、PNGシグネチャだけの切断ファイル
- `97000000-0000-4000-8000-000000000001.png` などseedが参照する9画像

画像生成先は必ず空の専用ディレクトリを `-Destination` で指定します。このスクリプトは既存ファイルがあるディレクトリを拒否します。

## tokenの扱い

固定tokenは専用DBだけで使います。証跡には値を載せません。

|用途|対象|状態|
|---|---|---|
|メール確認|U01|valid / expired / used|
|パスワード再設定|U02|valid / expired / used|

実際のメール送信ケースでは固定tokenを使わず、SMTP受信箱のURL・コードとDBのハッシュ化前提を確認します。確認後、メール本文と受信箱を削除します。
