# Band Link リリーステストケース

生成元: `scripts/test/generate-release-test-catalog.ps1`  
状態: 161件のリリースゲート用代表ケース（全件未実行）  
総数: 161件

全組み合わせのケース台帳は `docs/test-plan/full-case-inventory.csv` に出力する。これは機能ごとの入力値・ユーザー状態・データ状態・操作を直積で列挙した14,659件の実行単位であり、161件の代表ケースで全網羅とみなしてはならない。再生成コマンドは `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test/generate-full-case-inventory.ps1`。

各ケースは単独で結果を記録します。複数のデータ値が書かれたケースは、列挙値をすべて実施して初めてPASSです。期待結果と現実装が違う場合、期待結果を変更せずFAILとして記録します。

## UT-001 — FeedbackServiceが通常のSpring構成で生成できる
- **要件ID:** R-FBK-03
- **テスト層:** 単体
- **優先度:** P0
- **目的:** FeedbackServiceが通常のSpring構成で生成できる
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** なし
- **使用データ:** mock repositories
- **操作手順:** ApplicationContextへrepositoryだけ登録してservice取得
- **期待結果:** Bean生成と問い合わせ保存が成功
- **DB/API/UIで確認する内容:** 生成Bean、保存引数
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-002 — 問い合わせと機能要望を別種別で保存する
- **要件ID:** R-FBK-01
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 問い合わせと機能要望を別種別で保存する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U04
- **使用データ:** 改行と前後空白を含む本文
- **操作手順:** 2種別を順にserviceへ渡す
- **期待結果:** 本文をtrimし種別と時刻を正しく保存
- **DB/API/UIで確認する内容:** Feedback全列
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-003 — 空の自由記述を拒否する
- **要件ID:** R-FBK-02
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 空の自由記述を拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U04
- **使用データ:** null、空、空白のみ
- **操作手順:** 各値で作成を呼ぶ
- **期待結果:** 例外となりrepositoryへ触れない
- **DB/API/UIで確認する内容:** 例外型、副作用0
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-004 — feedback画像URLの外部・走査・不正拡張子を拒否する
- **要件ID:** R-IMG-04
- **テスト層:** 単体
- **優先度:** P0
- **目的:** feedback画像URLの外部・走査・不正拡張子を拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U04
- **使用データ:** https、../、svg、query、javascript
- **操作手順:** 各URLで作成を呼ぶ
- **期待結果:** 全件拒否し保存0
- **DB/API/UIで確認する内容:** URL検査、save回数
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-005 — 存在しない利用者のfeedbackを作らない
- **要件ID:** R-FBK-01
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 存在しない利用者のfeedbackを作らない
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** 不明ID
- **使用データ:** userId=99
- **操作手順:** 作成を呼ぶ
- **期待結果:** 利用者不明で失敗し孤児行0
- **DB/API/UIで確認する内容:** find結果、save回数
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-006 — 自由記述3000文字と画像URL1000文字の境界
- **要件ID:** R-FBK-02
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 自由記述3000文字と画像URL1000文字の境界
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U04
- **使用データ:** 2999/3000/3001、999/1000/1001文字
- **操作手順:** Bean Validationを実行
- **期待結果:** 上限以下だけ制約違反0
- **DB/API/UIで確認する内容:** constraint pathとmessage
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-007 — 未確認利用者のDM送信を拒否する
- **要件ID:** R-AUTH-04
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 未確認利用者のDM送信を拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U01
- **使用データ:** 本文hello
- **操作手順:** sendを呼ぶ
- **期待結果:** 会話・message・通知を一切作らない
- **DB/API/UIで確認する内容:** RuleViolation、副作用0
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-008 — 停止・退会状態の送受信を拒否する
- **要件ID:** R-SEC-03
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 停止・退会状態の送受信を拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U07/U11/U24
- **使用データ:** sender/recipient各状態
- **操作手順:** 組合せごとにsend
- **期待結果:** 全組合せ拒否
- **DB/API/UIで確認する内容:** 会話、message、通知0
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-009 — 自分宛DMを拒否する
- **要件ID:** R-MSG-01
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 自分宛DMを拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U07
- **使用データ:** recipient=U07
- **操作手順:** sendを呼ぶ
- **期待結果:** 業務例外、副作用0
- **DB/API/UIで確認する内容:** 例外、repository呼出し
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-010 — どちら向きのブロックでも送信不可
- **要件ID:** R-BLK-03
- **テスト層:** 単体
- **優先度:** P1
- **目的:** どちら向きのブロックでも送信不可
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U09/U10
- **使用データ:** blocker→blockedと逆向き
- **操作手順:** 各向きでsend
- **期待結果:** 双方とも拒否
- **DB/API/UIで確認する内容:** block照会、message0
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-011 — 本文も画像もないDMと公開画像URLを拒否する
- **要件ID:** R-MSG-03
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 本文も画像もないDMと公開画像URLを拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U07/U08
- **使用データ:** null、空白、/uploads/x.png
- **操作手順:** sendを呼ぶ
- **期待結果:** 全件拒否
- **DB/API/UIで確認する内容:** 正規化結果、副作用0
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-012 — 既存会話を再利用して受信者へ通知する
- **要件ID:** R-MSG-02
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 既存会話を再利用して受信者へ通知する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U07/U08
- **使用データ:** C001、前後空白本文
- **操作手順:** 逆方向からsend
- **期待結果:** 会話重複なし、本文trim、lastMessage更新、通知1
- **DB/API/UIで確認する内容:** 全entityとsave回数
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-013 — 画像だけのDMを許可する
- **要件ID:** R-MSG-03
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 画像だけのDMを許可する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U07/U08
- **使用データ:** 認可URL
- **操作手順:** content=nullでsend
- **期待結果:** 画像URLを持つmessageと通知を保存
- **DB/API/UIで確認する内容:** Message/Notification
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-014 — 第三者の会話一覧・既読・購読を拒否する
- **要件ID:** R-MSG-05
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 第三者の会話一覧・既読・購読を拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U23
- **使用データ:** C001
- **操作手順:** messages、markRead、requireParticipant
- **期待結果:** 全操作を拒否
- **DB/API/UIで確認する内容:** 例外と更新0
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-015 — 既読は相手の未読だけを一度更新する
- **要件ID:** R-MSG-06
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 既読は相手の未読だけを一度更新する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U07/U08
- **使用データ:** 受信未読、送信、自身既読
- **操作手順:** markReadを2回
- **期待結果:** 受信未読だけT0、再実行で不変
- **DB/API/UIで確認する内容:** readAt、通知不変
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-016 — DM本文1000文字境界
- **要件ID:** R-MSG-03
- **テスト層:** 単体
- **優先度:** P0
- **目的:** DM本文1000文字境界
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U07
- **使用データ:** 1000/1001文字、画像URL1001文字
- **操作手順:** Bean Validation
- **期待結果:** 1000だけ許可
- **DB/API/UIで確認する内容:** constraint path
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-017 — 画像保存・読込・削除を専用dir内で完結する
- **要件ID:** R-IMG-01
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 画像保存・読込・削除を専用dir内で完結する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U14
- **使用データ:** 有効PNG、走査を含む元名
- **操作手順:** store/load/delete
- **期待結果:** UUID名で往復し外へ書かない
- **DB/API/UIで確認する内容:** 実path、byte列
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-018 — MIME欠落時に拡張子推定し実体照合する
- **要件ID:** R-IMG-02
- **テスト層:** 単体
- **優先度:** P0
- **目的:** MIME欠落時に拡張子推定し実体照合する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U14
- **使用データ:** null/空/octet-stream、PNG
- **操作手順:** 各MIMEでstore
- **期待結果:** PNGだけ受理し偽jpg拒否
- **DB/API/UIで確認する内容:** 返却拡張子、保存数
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-019 — 空・5MB超・未対応画像を拒否する
- **要件ID:** R-IMG-03
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 空・5MB超・未対応画像を拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U14
- **使用データ:** null、0byte、5MB+1、SVG
- **操作手順:** 各fileでstore
- **期待結果:** 全件拒否しdir空
- **DB/API/UIで確認する内容:** 例外、ファイル数
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-020 — 5MBちょうどを許可する
- **要件ID:** R-IMG-03
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 5MBちょうどを許可する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U14
- **使用データ:** 5,242,880byte PNG
- **操作手順:** store
- **期待結果:** 保存成功
- **DB/API/UIで確認する内容:** 実サイズとURL
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-021 — 画像読込のpath traversalを拒否する
- **要件ID:** R-IMG-04
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 画像読込のpath traversalを拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U23
- **使用データ:** ../、backslash、絶対path、query
- **操作手順:** load
- **期待結果:** 全件拒否
- **DB/API/UIで確認する内容:** root外アクセス0
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-022 — シグネチャだけの切断画像を拒否する
- **要件ID:** R-IMG-02
- **テスト層:** 単体
- **優先度:** P0
- **目的:** シグネチャだけの切断画像を拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U14
- **使用データ:** 8byte PNG header
- **操作手順:** store
- **期待結果:** デコード不能として拒否
- **DB/API/UIで確認する内容:** 例外、ファイル0
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-023 — 自己ブロックを拒否する
- **要件ID:** R-BLK-01
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 自己ブロックを拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U09
- **使用データ:** 同一ID
- **操作手順:** block
- **期待結果:** 例外、副作用0
- **DB/API/UIで確認する内容:** repository interaction
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-024 — ブロック再送を冪等に扱う
- **要件ID:** R-BLK-01
- **テスト層:** 単体
- **優先度:** P1
- **目的:** ブロック再送を冪等に扱う
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U09/U10
- **使用データ:** 同一組合せ2回
- **操作手順:** blockを2回
- **期待結果:** 関係は1件
- **DB/API/UIで確認する内容:** save回数1
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-025 — 解除は自分向きだけを削除し再送はno-op
- **要件ID:** R-BLK-02
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 解除は自分向きだけを削除し再送はno-op
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U09/U10
- **使用データ:** 既存関係
- **操作手順:** unblockを2回
- **期待結果:** 1回だけ削除
- **DB/API/UIで確認する内容:** delete回数と向き
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-026 — 登録の名前・mail・password境界を検証する
- **要件ID:** R-AUTH-01
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 登録の名前・mail・password境界を検証する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** 新規
- **使用データ:** 名前0/1/80/81、mail320/321、password7/8/128/129
- **操作手順:** Bean Validationを全組合せ実行
- **期待結果:** 定義範囲だけ有効
- **DB/API/UIで確認する内容:** constraint field
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-027 — 年齢0..120と経験0..100の境界
- **要件ID:** R-PROF-02
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 年齢0..120と経験0..100の境界
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U15/U16
- **使用データ:** -1/0/120/121、-1/0/100/101
- **操作手順:** 登録・profile DTOを検証
- **期待結果:** 上下限内だけ有効
- **DB/API/UIで確認する内容:** age/experience constraint
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-028 — 性別を男性・女性だけに限定する
- **要件ID:** R-PROF-03
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 性別を男性・女性だけに限定する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U04
- **使用データ:** 男性、女性、未回答、その他、空白付き
- **操作手順:** DTO validation
- **期待結果:** 完全一致2値だけ有効
- **DB/API/UIで確認する内容:** gender constraint
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-029 — プロフィール必須マスタと活動エリア1..3件を検証する
- **要件ID:** R-PROF-04
- **テスト層:** 単体
- **優先度:** P1
- **目的:** プロフィール必須マスタと活動エリア1..3件を検証する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U03/U04
- **使用データ:** 空、1、3、4、未知ID
- **操作手順:** validationとservice更新
- **期待結果:** 空と4件と未知IDを拒否
- **DB/API/UIで確認する内容:** 関連行数と例外
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-030 — 投稿文字数・必須値の境界を検証する
- **要件ID:** R-POST-04
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 投稿文字数・必須値の境界を検証する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U05
- **使用データ:** title0/1/30/31、body0/1/500/501、area100/101
- **操作手順:** DTO validation
- **期待結果:** 上限以下かつ非空だけ有効
- **DB/API/UIで確認する内容:** constraint field
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-031 — 投稿選択上限と未知masterを拒否する
- **要件ID:** R-POST-05
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 投稿選択上限と未知masterを拒否する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U05
- **使用データ:** part0/5/6、genre0/3/4、stance0/1/2、pref0/3/4
- **操作手順:** service作成
- **期待結果:** 承認上限内・存在IDだけ保存
- **DB/API/UIで確認する内容:** 関連件数、例外
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-032 — 公開・終了・期限切れ・再公開の状態遷移を固定する
- **要件ID:** R-POST-06
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 公開・終了・期限切れ・再公開の状態遷移を固定する
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U20
- **使用データ:** P006
- **操作手順:** expire、close、reopen順に実行
- **期待結果:** 許可遷移だけ成立し理由・時刻が整合
- **DB/API/UIで確認する内容:** status/reason/timestamps
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-033 — 12時間編集と順位更新の境界
- **要件ID:** R-POST-07
- **テスト層:** 単体
- **優先度:** P1
- **目的:** 12時間編集と順位更新の境界
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U17-U20
- **使用データ:** 11:59:59、12:00:00、12:00:01
- **操作手順:** update/reopen
- **期待結果:** 境界時刻から許可
- **DB/API/UIで確認する内容:** lastEdited/rankUpdated
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-034 — オンライン5分と最終ログイン区分の境界
- **要件ID:** R-PRES-01
- **テスト層:** 単体
- **優先度:** P1
- **目的:** オンライン5分と最終ログイン区分の境界
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U05/U06
- **使用データ:** 4:59、5:00、5:01、未来、なし、3/7/30日
- **操作手順:** DTO変換
- **期待結果:** 仕様区分だけ返し生時刻を漏らさない
- **DB/API/UIで確認する内容:** JSON対象field
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## UT-035 — 公開DTOにmail・hash・正確な活動時刻を含めない
- **要件ID:** R-SEC-06
- **テスト層:** 単体
- **優先度:** P0
- **目的:** 公開DTOにmail・hash・正確な活動時刻を含めない
- **前提条件:** 対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない
- **使用ユーザー:** U04
- **使用データ:** 全個人field設定済みUser
- **操作手順:** 全公開DTOへ変換
- **期待結果:** 許可fieldだけ返す
- **DB/API/UIで確認する内容:** reflection/JSON field集合
- **証跡:** JUnit XML、失敗時stack trace、検証した引数
- **後処理:** mock・一時dirをテスト終了時に破棄
- **失敗時の切り分け方法:** 最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認

## IT-001 — 登録が利用者と必須プロフィール関連を原子的に保存する
- **要件ID:** R-AUTH-01
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 登録が利用者と必須プロフィール関連を原子的に保存する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** 新規
- **使用データ:** 一意mail、全選択1件
- **操作手順:** CSRF付きPOST /api/auth/register
- **期待結果:** 201、session作成、全関連保存
- **DB/API/UIで確認する内容:** usersと4関連表、response
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-002 — login成功・失敗・logout後session無効
- **要件ID:** R-AUTH-02
- **テスト層:** 結合
- **優先度:** P0
- **目的:** login成功・失敗・logout後session無効
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U02
- **使用データ:** 正解/誤りpassword
- **操作手順:** login→me→logout→me
- **期待結果:** 200→200→204→401
- **DB/API/UIで確認する内容:** session id、lastLoginAt
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-003 — 同一sessionで再login時にsession idを更新する
- **要件ID:** R-AUTH-03
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 同一sessionで再login時にsession idを更新する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U02
- **使用データ:** 既存anonymous session
- **操作手順:** csrf取得後login
- **期待結果:** 固定化されず認証状態維持
- **DB/API/UIで確認する内容:** Set-Cookie/session id
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-004 — 確認tokenの有効・期限切れ・使用済み・再利用
- **要件ID:** R-AUTH-04
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 確認tokenの有効・期限切れ・使用済み・再利用
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U01
- **使用データ:** 3固定token
- **操作手順:** POST verify-emailを順に実行
- **期待結果:** 有効だけ204、他は4xx、再利用不可
- **DB/API/UIで確認する内容:** verifiedAt、usedAt
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-005 — 確認再送が旧tokenを無効化し新規1件を作る
- **要件ID:** R-AUTH-05
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 確認再送が旧tokenを無効化し新規1件を作る
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U01
- **使用データ:** 未使用token
- **操作手順:** 認証sessionでresend
- **期待結果:** 202、旧無効、新規期限24h
- **DB/API/UIで確認する内容:** token行数・値非ログ
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-006 — 再設定依頼はmail存在を応答から漏らさない
- **要件ID:** R-AUTH-06
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 再設定依頼はmail存在を応答から漏らさない
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U02/未知mail
- **使用データ:** 2つのmail
- **操作手順:** POST request
- **期待結果:** 双方202、存在時だけtoken
- **DB/API/UIで確認する内容:** status/body/time差、DB
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-007 — 再設定tokenの有効・期限・使用済み・再利用
- **要件ID:** R-AUTH-07
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 再設定tokenの有効・期限・使用済み・再利用
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U02
- **使用データ:** 3固定token
- **操作手順:** confirm後に新旧passwordでlogin
- **期待結果:** 有効1回のみ更新
- **DB/API/UIで確認する内容:** hash、usedAt、login結果
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-008 — 退会が全関連データと画像参照を削除し再登録可能にする
- **要件ID:** R-AUTH-08
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 退会が全関連データと画像参照を削除し再登録可能にする
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U22
- **使用データ:** P012/C004/F003/画像
- **操作手順:** withdraw→DB照合→同mail登録
- **期待結果:** 204、個人データ0、再登録201
- **DB/API/UIで確認する内容:** 全FK表、files、session
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-009 — 本人プロフィールと公開プロフィールの契約を分離する
- **要件ID:** R-PROF-01
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 本人プロフィールと公開プロフィールの契約を分離する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U04/U23
- **使用データ:** U04全field
- **操作手順:** GET meとGET /{id}
- **期待結果:** 本人のみmail/status/role、公開は非公開fieldなし
- **DB/API/UIで確認する内容:** JSON key集合
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-010 — profile更新で関連集合を置換し未知IDを拒否する
- **要件ID:** R-PROF-04
- **テスト層:** 結合
- **優先度:** P0
- **目的:** profile更新で関連集合を置換し未知IDを拒否する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U04
- **使用データ:** 全master変更、未知999999
- **操作手順:** PUTを2回
- **期待結果:** 正常時置換、異常時全rollback
- **DB/API/UIで確認する内容:** user関連4表
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-011 — profile画像差替えで新規保存後に旧画像を削除する
- **要件ID:** R-IMG-01
- **テスト層:** 結合
- **優先度:** P0
- **目的:** profile画像差替えで新規保存後に旧画像を削除する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U04
- **使用データ:** 既存PNGと新JPEG
- **操作手順:** multipart POST
- **期待結果:** URL更新、旧file削除、新file存在
- **DB/API/UIで確認する内容:** users、filesystem、MIME
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-012 — 2種別の投稿をAPI契約どおり作成する
- **要件ID:** R-POST-01
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 2種別の投稿をAPI契約どおり作成する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U05/U06
- **使用データ:** 全必須field
- **操作手順:** POST /api/posts
- **期待結果:** 201、型・30日期限・関連保存
- **DB/API/UIで確認する内容:** response、posts、関連表
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-013 — 他人の投稿編集・終了・再公開を拒否する
- **要件ID:** R-POST-02
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 他人の投稿編集・終了・再公開を拒否する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U07
- **使用データ:** P001/P005
- **操作手順:** PUT/PATCH
- **期待結果:** 全て403相当、DB不変
- **DB/API/UIで確認する内容:** status/body/updatedAt
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-014 — 同一利用者の公開投稿1件制限をDB境界で守る
- **要件ID:** R-POST-03
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 同一利用者の公開投稿1件制限をDB境界で守る
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U05
- **使用データ:** 既存P001
- **操作手順:** 別内容をPOST
- **期待結果:** 競合応答、行追加0
- **DB/API/UIで確認する内容:** posts count、status
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-015 — 期限到来時に一覧から消え詳細は扱いどおりになる
- **要件ID:** R-POST-06
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 期限到来時に一覧から消え詳細は扱いどおりになる
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U21
- **使用データ:** P007、T0前後
- **操作手順:** 一覧・詳細取得
- **期待結果:** 期限後一覧非表示、状態と理由整合
- **DB/API/UIで確認する内容:** posts status/reason、API
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-016 — 本文と画像の一括保存・5枚上限・並べ替え・削除をDBとfilesで一致させる
- **要件ID:** R-POST-08
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 本文と画像の一括保存・5枚上限・並べ替え・削除をDBとfilesで一致させる
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U14
- **使用データ:** P009、追加画像、途中upload失敗
- **操作手順:** 本文+複数画像作成→6枚目追加→並替→削除
- **期待結果:** 一括成功または全rollback、上限拒否、順序一意、削除fileなし
- **DB/API/UIで確認する内容:** posts/post_images、filesystem、orphan
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-017 — キーワードがタイトル・本文・補足エリアを部分一致する
- **要件ID:** R-SRCH-01
- **テスト層:** 結合
- **優先度:** P0
- **目的:** キーワードがタイトル・本文・補足エリアを部分一致する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** 匿名
- **使用データ:** P001/P002と固有語
- **操作手順:** 3fieldの語でGET
- **期待結果:** 対象だけ返す
- **DB/API/UIで確認する内容:** SQL結果、JSON IDs
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-018 — 同一条件OR・異種条件ANDで検索する
- **要件ID:** R-SRCH-02
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 同一条件OR・異種条件ANDで検索する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** 匿名
- **使用データ:** 複数masterのP001
- **操作手順:** 複数query parameter
- **期待結果:** 集合論どおり重複なし
- **DB/API/UIで確認する内容:** IDs、件数、distinct
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-019 — 最新順・ログイン順のtie breakが安定する
- **要件ID:** R-SRCH-03
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 最新順・ログイン順のtie breakが安定する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** 匿名
- **使用データ:** 同時刻fixture
- **操作手順:** 各sortで2回取得
- **期待結果:** 順序安定、欠落重複なし
- **DB/API/UIで確認する内容:** ID列、rank/login時刻
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-020 — cursorの正常・不正・巨大・limit境界
- **要件ID:** R-SRCH-04
- **テスト層:** 結合
- **優先度:** P0
- **目的:** cursorの正常・不正・巨大・limit境界
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** 匿名
- **使用データ:** 74投稿
- **操作手順:** cursor null/12/abc/巨大、limit0/1/50/51
- **期待結果:** 500なし、規定件数、連結で全件一度
- **DB/API/UIで確認する内容:** status、nextCursor、ID集合
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-021 — 検索履歴は認証時だけ全条件を重複なく直近5件に保つ
- **要件ID:** R-SRCH-05
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 検索履歴は認証時だけ全条件を重複なく直近5件に保つ
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U02/匿名
- **使用データ:** keywordと全filters/type/sort、同条件、6条件、page cursor
- **操作手順:** 検索→同条件再検索→6条件検索→続きを取得→history取得
- **期待結果:** 認証時だけ保存、同条件は最新へ移動、最大5、page追加0、条件欠落なし
- **DB/API/UIで確認する内容:** search_histories件数/順序/hash/JSON
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-022 — 双方向の初回同時送信でも会話を1件にする
- **要件ID:** R-MSG-02
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 双方向の初回同時送信でも会話を1件にする
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U07/U08
- **使用データ:** barrier付き2要求
- **操作手順:** 同時POST
- **期待結果:** 会話1件、message2件、500なし
- **DB/API/UIで確認する内容:** unique制約、response
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-023 — DM送信と通知保存を同一transactionにする
- **要件ID:** R-MSG-03
- **テスト層:** 結合
- **優先度:** P0
- **目的:** DM送信と通知保存を同一transactionにする
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U07/U08
- **使用データ:** 通知save障害
- **操作手順:** POST send
- **期待結果:** 失敗時messageもrollback
- **DB/API/UIで確認する内容:** messages/notifications count
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-024 — DM画像は当事者と対象通報を見る管理者だけ取得できる
- **要件ID:** R-MSG-05
- **テスト層:** 結合
- **優先度:** P0
- **目的:** DM画像は当事者と対象通報を見る管理者だけ取得できる
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U07/U08/U13/U23
- **使用データ:** C001画像、報告有無画像
- **操作手順:** 各sessionでGET
- **期待結果:** 当事者200、該当管理者200、第三者403、未報告管理者403
- **DB/API/UIで確認する内容:** status、byte/MIME
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-025 — 個別・一括既読が本人通知だけを更新する
- **要件ID:** R-MSG-06
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 個別・一括既読が本人通知だけを更新する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U07/U08
- **使用データ:** 既読/未読混在
- **操作手順:** PATCH individual/read-all
- **期待結果:** 対象だけ更新、再送冪等
- **DB/API/UIで確認する内容:** notification readAt
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-026 — ブロック関係を投稿検索とDMの双方へ反映する
- **要件ID:** R-BLK-03
- **テスト層:** 結合
- **優先度:** P0
- **目的:** ブロック関係を投稿検索とDMの双方へ反映する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U09/U10
- **使用データ:** P010/P011/C002
- **操作手順:** block前後に一覧・送信
- **期待結果:** 双方の投稿非表示、送信拒否、解除後復旧
- **DB/API/UIで確認する内容:** API IDs、block row
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-027 — DM通報は参加者だけが対象message snapshotを固定する
- **要件ID:** R-RPT-02
- **テスト層:** 結合
- **優先度:** P0
- **目的:** DM通報は参加者だけが対象message snapshotを固定する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U07/U23
- **使用データ:** M002
- **操作手順:** 両者でPOST report
- **期待結果:** 参加者だけ成功、前後本文なし
- **DB/API/UIで確認する内容:** reports snapshot、件数
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-028 — 一般利用者と管理者の管理API権限を分離する
- **要件ID:** R-ADM-01
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 一般利用者と管理者の管理API権限を分離する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U02/U13
- **使用データ:** reports/feedback/users/posts admin paths
- **操作手順:** 各roleで全method
- **期待結果:** 一般403、管理者は規定成功
- **DB/API/UIで確認する内容:** status、監査対象行
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-029 — feedback本文と画像URLを利用者へ紐付ける
- **要件ID:** R-FBK-01
- **テスト層:** 結合
- **優先度:** P0
- **目的:** feedback本文と画像URLを利用者へ紐付ける
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U04
- **使用データ:** 問い合わせ・要望各1件
- **操作手順:** upload→POST2種
- **期待結果:** 201、2種別を本人IDで保存
- **DB/API/UIで確認する内容:** feedback、file
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## IT-030 — 全変更APIがCSRFなしを拒否し有効tokenを受理する
- **要件ID:** R-SEC-01
- **テスト層:** 結合
- **優先度:** P0
- **目的:** 全変更APIがCSRFなしを拒否し有効tokenを受理する
- **前提条件:** 専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得
- **使用ユーザー:** U02
- **使用データ:** 全POST/PUT/PATCH/DELETE代表値
- **操作手順:** tokenなし/偽/正で送信
- **期待結果:** なし・偽403、正は認可/validation段階へ
- **DB/API/UIで確認する内容:** status、DB副作用
- **証跡:** JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧
- **後処理:** transaction rollbackまたはseed再投入。生成画像を専用dirから削除
- **失敗時の切り分け方法:** HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定

## ST-001 — 初見登録を完了し確認案内へ到達する
- **要件ID:** R-AUTH-01
- **テスト層:** システム
- **優先度:** P0
- **目的:** 初見登録を完了し確認案内へ到達する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 新規
- **使用データ:** 一意mail、正常プロフィール
- **操作手順:** /registerをキーボードだけで入力・送信
- **期待結果:** 201相当、ログイン状態、確認案内表示
- **DB/API/UIで確認する内容:** UI、me API、users
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-002 — 登録全項目の必須・境界エラーを入力位置へ示す
- **要件ID:** R-AUTH-01
- **テスト層:** システム
- **優先度:** P0
- **目的:** 登録全項目の必須・境界エラーを入力位置へ示す
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 新規
- **使用データ:** 空、上限超、年齢文字
- **操作手順:** 順に入力して送信
- **期待結果:** 送信せず対象fieldへfocus、理解できる日本語
- **DB/API/UIで確認する内容:** UI validation、API未送信
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-003 — login成功・誤password・未知mailを回復できる
- **要件ID:** R-AUTH-02
- **テスト層:** システム
- **優先度:** P0
- **目的:** login成功・誤password・未知mailを回復できる
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U02
- **使用データ:** 正誤資格情報
- **操作手順:** 3パターン実行
- **期待結果:** 成功は一覧、失敗は入力保持と安全な案内
- **DB/API/UIで確認する内容:** UI、login status、session
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-004 — logout後に戻る操作でも認証画面を再表示しない
- **要件ID:** R-AUTH-08
- **テスト層:** システム
- **優先度:** P0
- **目的:** logout後に戻る操作でも認証画面を再表示しない
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U02
- **使用データ:** login済み
- **操作手順:** logout→browser back→保護URL
- **期待結果:** 認証情報・個人画面が復元されない
- **DB/API/UIで確認する内容:** UI、me=401、cache
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-005 — 未確認利用者の許可・禁止導線を統一する
- **要件ID:** R-AUTH-04
- **テスト層:** システム
- **優先度:** P0
- **目的:** 未確認利用者の許可・禁止導線を統一する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U01
- **使用データ:** 未確認session
- **操作手順:** posts/messages/settings/supportへ直URL
- **期待結果:** verify画面へ誘導、再送とlogoutだけ可能
- **DB/API/UIで確認する内容:** route、各API status
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-006 — 確認mailのlinkで一度だけ確認完了する
- **要件ID:** R-AUTH-04
- **テスト層:** システム
- **優先度:** P0
- **目的:** 確認mailのlinkで一度だけ確認完了する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U01
- **使用データ:** SMTP受信link
- **操作手順:** linkを開き再読込
- **期待結果:** 成功案内と一覧遷移、再利用は安全な案内
- **DB/API/UIで確認する内容:** mail、UI、token DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-007 — 確認再送の連打を二重送信にしない
- **要件ID:** R-AUTH-05
- **テスト層:** システム
- **優先度:** P0
- **目的:** 確認再送の連打を二重送信にしない
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U01
- **使用データ:** 遅延SMTP
- **操作手順:** 再送を素早く2回操作
- **期待結果:** busy表示、1回だけ有効token
- **DB/API/UIで確認する内容:** UI disabled、mail/DB件数
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-008 — password再設定依頼から新password loginまで完了する
- **要件ID:** R-AUTH-06
- **テスト層:** システム
- **優先度:** P0
- **目的:** password再設定依頼から新password loginまで完了する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U02
- **使用データ:** SMTP受信code
- **操作手順:** 依頼→confirm→login
- **期待結果:** 成功し旧password拒否
- **DB/API/UIで確認する内容:** UI、API、DB hash非表示
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-009 — 存在しないmailでもアカウント有無を画面に出さない
- **要件ID:** R-AUTH-06
- **テスト層:** システム
- **優先度:** P0
- **目的:** 存在しないmailでもアカウント有無を画面に出さない
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 未知
- **使用データ:** 未登録mail
- **操作手順:** 再設定依頼
- **期待結果:** 登録済みと同じ文言・遷移
- **DB/API/UIで確認する内容:** UI text、status
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-010 — LINE無効時はボタン非表示、有効時はstate付き遷移
- **要件ID:** R-AUTH-09
- **テスト層:** システム
- **優先度:** P0
- **目的:** LINE無効時はボタン非表示、有効時はstate付き遷移
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 新規/U25
- **使用データ:** 設定なし・テストチャネル
- **操作手順:** login/registerを表示しOAuth往復
- **期待結果:** 設定別表示、state不一致拒否、成功後適切な遷移
- **DB/API/UIで確認する内容:** UI、redirect、session
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-011 — 公開プロフィールが連絡判断情報を読みやすく示す
- **要件ID:** R-PROF-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** 公開プロフィールが連絡判断情報を読みやすく示す
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名/U04
- **使用データ:** 完全profile
- **操作手順:** /users/910004表示
- **期待結果:** 名前、年齢、性別、経験、地域、part、genre、stance、bio、media表示
- **DB/API/UIで確認する内容:** UIとpublic API
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-012 — 未完成・停止・退会プロフィールの表示を安全に扱う
- **要件ID:** R-PROF-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** 未完成・停止・退会プロフィールの表示を安全に扱う
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U03/U11/U24
- **使用データ:** 各公開URL
- **操作手順:** 匿名で順に開く
- **期待結果:** 欠落で崩れず、停止/退会は規定の匿名化・非公開
- **DB/API/UIで確認する内容:** UI、status
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-013 — プロフィール全必須項目を編集保存し再読込で保持する
- **要件ID:** R-PROF-04
- **テスト層:** システム
- **優先度:** P1
- **目的:** プロフィール全必須項目を編集保存し再読込で保持する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U03
- **使用データ:** 全正常値
- **操作手順:** settings/profile入力→保存→reload
- **期待結果:** 完全状態になり同値表示
- **DB/API/UIで確認する内容:** UI、me API、関連DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-014 — 年齢と経験を自由入力しspinnerを表示しない
- **要件ID:** R-PROF-02
- **テスト層:** システム
- **優先度:** P1
- **目的:** 年齢と経験を自由入力しspinnerを表示しない
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U04
- **使用データ:** 0、120、0、100
- **操作手順:** mouse/keyboardで入力
- **期待結果:** 上下余白、矢印なし、保存可能
- **DB/API/UIで確認する内容:** screenshot、input type/style
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-015 — 性別は男性・女性の二択で単一選択する
- **要件ID:** R-PROF-03
- **テスト層:** システム
- **優先度:** P1
- **目的:** 性別は男性・女性の二択で単一選択する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U04
- **使用データ:** 2値
- **操作手順:** Tab/矢印/Space操作
- **期待結果:** 1つだけ選択、青線の不要な残留なし
- **DB/API/UIで確認する内容:** UI、request body
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-016 — 地域最大3件と各マスタ必須を画面で止める
- **要件ID:** R-PROF-04
- **テスト層:** システム
- **優先度:** P1
- **目的:** 地域最大3件と各マスタ必須を画面で止める
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U04
- **使用データ:** 0/1/3/4選択
- **操作手順:** 選択・解除・保存
- **期待結果:** 0と4を明示、1..3保存
- **DB/API/UIで確認する内容:** UI count、API、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-017 — profile画像を選択・preview・差替え・削除する
- **要件ID:** R-PROF-05
- **テスト層:** システム
- **優先度:** P1
- **目的:** profile画像を選択・preview・差替え・削除する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U04
- **使用データ:** PNG/JPEG/不正画像
- **操作手順:** 各操作後reload
- **期待結果:** 正常画像だけ反映し旧画像残存なし
- **DB/API/UIで確認する内容:** UI、API、files
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-018 — YouTube/TikTok/SoundCloud/Spotify/Apple Musicを安全に表示する
- **要件ID:** R-PROF-06
- **テスト層:** システム
- **優先度:** P1
- **目的:** YouTube/TikTok/SoundCloud/Spotify/Apple Musicを安全に表示する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U04
- **使用データ:** 正常、短縮、不正host、javascript URL
- **操作手順:** 保存して公開profile表示
- **期待結果:** 対応URLだけembed/安全link、不正は実行不可
- **DB/API/UIで確認する内容:** DOM src/href、console
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-019 — オンライン中と最終ログイン表示を境界どおり示す
- **要件ID:** R-PRES-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** オンライン中と最終ログイン表示を境界どおり示す
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U05/U06/U02
- **使用データ:** T0相対時刻
- **操作手順:** 一覧とprofile表示
- **期待結果:** 5分以内だけonline、正確時刻は非表示
- **DB/API/UIで確認する内容:** UI text、API fields
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-020 — メンバー募集を段階入力・preview・公開する
- **要件ID:** R-POST-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** メンバー募集を段階入力・preview・公開する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U18
- **使用データ:** 正常全fieldと画像2枚
- **操作手順:** newで全stepをkeyboard操作
- **期待結果:** 確認内容一致、公開後detail
- **DB/API/UIで確認する内容:** UI、POST body、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-021 — 参加希望の種別と見出しを明確に公開する
- **要件ID:** R-POST-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** 参加希望の種別と見出しを明確に公開する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U06再seed後
- **使用データ:** WANTS_TO_JOIN
- **操作手順:** 作成し一覧・詳細を開く
- **期待結果:** メンバー募集と混同せず大きく表示
- **DB/API/UIで確認する内容:** UI label、type API
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-022 — 投稿の空・文字数超過・不足選択と活動頻度6択をstep内で確認する
- **要件ID:** R-POST-04
- **テスト層:** システム
- **優先度:** P1
- **目的:** 投稿の空・文字数超過・不足選択と活動頻度6択をstep内で確認する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U18
- **使用データ:** 境界値、週2回以上/週1回/月2〜3回/月1回/不定期/相談
- **操作手順:** 各値を選び各stepで次へ/公開
- **期待結果:** 6択が選べ、違反時は進行を止め該当fieldへfocus、入力保持
- **DB/API/UIで確認する内容:** UI options、request enum、API未作成
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-023 — 自分の募集を編集し12時間制限を説明する
- **要件ID:** R-POST-02
- **テスト層:** システム
- **優先度:** P1
- **目的:** 自分の募集を編集し12時間制限を説明する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U18/U17
- **使用データ:** P003/P004
- **操作手順:** edit→保存
- **期待結果:** U18成功、U17は理由と次の可能時刻
- **DB/API/UIで確認する内容:** UI、PUT status、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-024 — 募集終了確認と再公開後も既存DMを継続する
- **要件ID:** R-POST-06
- **テスト層:** システム
- **優先度:** P1
- **目的:** 募集終了確認と再公開後も既存DMを継続する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U20/U07
- **使用データ:** P006と既存会話
- **操作手順:** 終了→既存DM送受信→再公開をconfirm含め操作
- **期待結果:** 状態表示と一覧反映、終了中もDM成功、二重操作なし
- **DB/API/UIで確認する内容:** UI、API、posts/messages DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-025 — 本人削除と管理削除・停止終了を区別し不正再公開を防ぐ
- **要件ID:** R-POST-06
- **テスト層:** システム
- **優先度:** P1
- **目的:** 本人削除と管理削除・停止終了を区別し不正再公開を防ぐ
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U06/U11
- **使用データ:** 本人所有投稿、P008/P014
- **操作手順:** my/postsで削除確認後、各終了理由の再公開を画面と直APIで試す
- **期待結果:** 本人だけ削除可能、管理削除/停止はCTAなし・直API拒否、理由保持
- **DB/API/UIで確認する内容:** UI、API status、posts/画像
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-026 — 投稿画像5枚のpreview・並替え・拡大・削除
- **要件ID:** R-POST-08
- **テスト層:** システム
- **優先度:** P1
- **目的:** 投稿画像5枚のpreview・並替え・拡大・削除
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U14
- **使用データ:** P009
- **操作手順:** drag/keyboard代替、画像click、delete
- **期待結果:** 順序保存、modal拡大、削除反映、6枚目拒否
- **DB/API/UIで確認する内容:** DOM、API、DB/files
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-027 — keywordを入力・Enter・clearして結果を更新する
- **要件ID:** R-SRCH-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** keywordを入力・Enter・clearして結果を更新する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名
- **使用データ:** タイトル/本文/補足固有語
- **操作手順:** 検索・clear
- **期待結果:** 該当結果、0件案内、復元
- **DB/API/UIで確認する内容:** URL、API query、UI
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-028 — 全filterのAND/ORと年齢不問の一致規則を理解できる
- **要件ID:** R-SRCH-02
- **テスト層:** システム
- **優先度:** P1
- **目的:** 全filterのAND/ORと年齢不問の一致規則を理解できる
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名
- **使用データ:** 全master、年代複数、ANY、任意未入力
- **操作手順:** 同項目複数・異項目・ANYを選択して解除
- **期待結果:** OR/ANDとANY規則どおり、summary・件数・結果が同期
- **DB/API/UIで確認する内容:** UI、query、IDs
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-029 — 最新順・ログイン順切替に不要な青枠を残さない
- **要件ID:** R-SRCH-03
- **テスト層:** システム
- **優先度:** P1
- **目的:** 最新順・ログイン順切替に不要な青枠を残さない
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名
- **使用データ:** 74投稿
- **操作手順:** selectをmouse/keyboardで切替
- **期待結果:** 順序更新、focusは見失わず不要な線なし
- **DB/API/UIで確認する内容:** screenshot、ID順
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-030 — 下端scrollの自動追加で重複・飛び・scroll jumpがない
- **要件ID:** R-SRCH-04
- **テスト層:** システム
- **優先度:** P1
- **目的:** 下端scrollの自動追加で重複・飛び・scroll jumpがない
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名
- **使用データ:** 74投稿
- **操作手順:** 下端へ繰り返しscrollして最後まで読む
- **期待結果:** 自動で次頁を追加し全74件が一度、操作位置維持
- **DB/API/UIで確認する内容:** DOM ID集合、API cursors、scroll位置
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-031 — 検索中の通信失敗から同条件で再試行する
- **要件ID:** R-SRCH-04
- **テスト層:** システム
- **優先度:** P1
- **目的:** 検索中の通信失敗から同条件で再試行する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名
- **使用データ:** API offline/500
- **操作手順:** 検索→失敗→復旧→retry
- **期待結果:** 理解できる案内、条件保持、成功
- **DB/API/UIで確認する内容:** UI、network log
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-032 — 認証時の検索履歴を確認し匿名時は残さない
- **要件ID:** R-SRCH-05
- **テスト層:** システム
- **優先度:** P1
- **目的:** 認証時の検索履歴を確認し匿名時は残さない
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U02/匿名
- **使用データ:** 全条件
- **操作手順:** 検索→履歴API
- **期待結果:** 認証分だけ時系列保存
- **DB/API/UIで確認する内容:** UI/API/DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-033 — 募集詳細の本文・条件・投稿者情報を同じ基準線で示す
- **要件ID:** R-UI-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** 募集詳細の本文・条件・投稿者情報を同じ基準線で示す
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名
- **使用データ:** P001
- **操作手順:** detailを表示
- **期待結果:** 投稿者位置と見出し線が揃い背景一色
- **DB/API/UIで確認する内容:** 1440 screenshot
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-034 — プロフィールを読む・募集一覧への導線が見つけやすい
- **要件ID:** R-UI-02
- **テスト層:** システム
- **優先度:** P1
- **目的:** プロフィールを読む・募集一覧への導線が見つけやすい
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名
- **使用データ:** P001/U05
- **操作手順:** 詳細から往復
- **期待結果:** 44px以上、文字可読、focus可視
- **DB/API/UIで確認する内容:** UI geometry、tab order
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-035 — profileのメッセージ送信からDM作成へ到達する
- **要件ID:** R-MSG-01
- **テスト層:** システム
- **優先度:** P0
- **目的:** profileのメッセージ送信からDM作成へ到達する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07
- **使用データ:** U08 profile
- **操作手順:** CTA click
- **期待結果:** 相手名と入力欄が表示され送信可能
- **DB/API/UIで確認する内容:** URL、UI、API
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-036 — 会話未選択時はX風の一覧中心画面として自然に見える
- **要件ID:** R-MSG-02
- **テスト層:** システム
- **優先度:** P0
- **目的:** 会話未選択時はX風の一覧中心画面として自然に見える
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07
- **使用データ:** 複数会話
- **操作手順:** /messages表示
- **期待結果:** 不要な説明文なし、一覧から選択できる
- **DB/API/UIで確認する内容:** desktop/mobile screenshot
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-037 — 本文DMをEnter規則と送信ボタンで送る
- **要件ID:** R-MSG-03
- **テスト層:** システム
- **優先度:** P0
- **目的:** 本文DMをEnter規則と送信ボタンで送る
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07/U08
- **使用データ:** 日本語、改行、1000文字
- **操作手順:** 入力・送信
- **期待結果:** 「送信」だけ表示、←なし、1件追加
- **DB/API/UIで確認する内容:** UI、POST、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-038 — PNG/JPEG/WebP画像DMと画像のみ送信
- **要件ID:** R-MSG-03
- **テスト層:** システム
- **優先度:** P0
- **目的:** PNG/JPEG/WebP画像DMと画像のみ送信
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07/U08
- **使用データ:** 3正常画像
- **操作手順:** 選択→preview→送信
- **期待結果:** エラーなく表示、本文なし可
- **DB/API/UIで確認する内容:** upload/send API、files
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-039 — 不正・超過画像から選択をやり直せる
- **要件ID:** R-MSG-03
- **テスト層:** システム
- **優先度:** P0
- **目的:** 不正・超過画像から選択をやり直せる
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07
- **使用データ:** empty/svg/mismatch/truncated/5MB+1
- **操作手順:** 各file選択・送信
- **期待結果:** 明確な制限案内、draft保持、再選択成功
- **DB/API/UIで確認する内容:** UI、status、files orphan0
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-040 — DM画像clickでdialog拡大しEsc・外側・閉じるで戻る
- **要件ID:** R-MSG-04
- **テスト層:** システム
- **優先度:** P0
- **目的:** DM画像clickでdialog拡大しEsc・外側・閉じるで戻る
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07/U08
- **使用データ:** M002画像
- **操作手順:** mouse/keyboardで拡大・閉じる
- **期待結果:** 原寸内表示、focus復帰、背景操作不可
- **DB/API/UIで確認する内容:** dialog DOM、screenshot
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-041 — 連続送受信を会話順・自他配置・時刻順で表示する
- **要件ID:** R-MSG-02
- **テスト層:** システム
- **優先度:** P0
- **目的:** 連続送受信を会話順・自他配置・時刻順で表示する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07/U08
- **使用データ:** 同秒含む10件
- **操作手順:** 交互送信
- **期待結果:** 欠落重複なし、左右と色で識別
- **DB/API/UIで確認する内容:** DOM、API IDs
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-042 — 会話を開くと相手の未読だけ既読になる
- **要件ID:** R-MSG-06
- **テスト層:** システム
- **優先度:** P0
- **目的:** 会話を開くと相手の未読だけ既読になる
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07/U08
- **使用データ:** C001
- **操作手順:** 一覧badge確認→open
- **期待結果:** badge/通知dot更新、自分送信不変
- **DB/API/UIで確認する内容:** UI、read API、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-043 — SSE切断・復帰と非表示tab復帰で欠落を回収する
- **要件ID:** R-MSG-07
- **テスト層:** システム
- **優先度:** P0
- **目的:** SSE切断・復帰と非表示tab復帰で欠落を回収する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07/U08
- **使用データ:** network断
- **操作手順:** 送信中に切断→復旧
- **期待結果:** 案内後refreshで一度だけ表示
- **DB/API/UIで確認する内容:** EventSource、GET、DOM IDs
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-044 — profileから確認後にblockし管理画面へ移る
- **要件ID:** R-BLK-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** profileから確認後にblockし管理画面へ移る
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U09
- **使用データ:** U10 profile
- **操作手順:** block CTA→confirm
- **期待結果:** 関係1件、一覧表示、DM不可
- **DB/API/UIで確認する内容:** UI、API、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-045 — block解除後に一覧とDMが復旧する
- **要件ID:** R-BLK-02
- **テスト層:** システム
- **優先度:** P1
- **目的:** block解除後に一覧とDMが復旧する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U09
- **使用データ:** U10 blocked
- **操作手順:** settings/blocksから解除
- **期待結果:** 確認後消え、投稿・送信復旧
- **DB/API/UIで確認する内容:** UI、API、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-046 — 投稿・利用者・DMを理由付きで通報する
- **要件ID:** R-RPT-01
- **テスト層:** システム
- **優先度:** P0
- **目的:** 投稿・利用者・DMを理由付きで通報する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07
- **使用データ:** P002/U11/M002
- **操作手順:** 各dialogをkeyboard操作
- **期待結果:** 1000文字以内を1件ずつ保存、完了toast
- **DB/API/UIで確認する内容:** UI、reports DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-047 — 管理者がstatus別通報とsnapshotだけを確認する
- **要件ID:** R-ADM-01
- **テスト層:** システム
- **優先度:** P0
- **目的:** 管理者がstatus別通報とsnapshotだけを確認する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U13
- **使用データ:** R001..R004
- **操作手順:** adminでfilter切替
- **期待結果:** 対象messageだけ、前後会話なし
- **DB/API/UIで確認する内容:** UI、API payload
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-048 — 管理者が通報状態変更・投稿非公開・利用停止解除する
- **要件ID:** R-ADM-02
- **テスト層:** システム
- **優先度:** P0
- **目的:** 管理者が通報状態変更・投稿非公開・利用停止解除する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U13
- **使用データ:** P002/U06/R001
- **操作手順:** confirm後各操作
- **期待結果:** 対象だけ変化、再読込一致
- **DB/API/UIで確認する内容:** UI、API、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-049 — login利用者が問い合わせを自由記述と画像で送る
- **要件ID:** R-FBK-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** login利用者が問い合わせを自由記述と画像で送る
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U04
- **使用データ:** 改行本文、PNG
- **操作手順:** header→contact→送信
- **期待結果:** 完了案内、CONTACT保存
- **DB/API/UIで確認する内容:** UI、API、DB/file
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-050 — login利用者が機能要望を別画面・種別で送る
- **要件ID:** R-FBK-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** login利用者が機能要望を別画面・種別で送る
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U04
- **使用データ:** 要望本文、画像なし
- **操作手順:** header→feature request→送信
- **期待結果:** CONTACTと混同せず保存
- **DB/API/UIで確認する内容:** UI、API、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-051 — 未login headerに問い合わせ・機能要望を表示しない
- **要件ID:** R-FBK-03
- **テスト層:** システム
- **優先度:** P1
- **目的:** 未login headerに問い合わせ・機能要望を表示しない
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 匿名
- **使用データ:** /, /posts, /users/910004
- **操作手順:** 各画面を表示
- **期待結果:** header/footer双方に導線なし、直URLはloginへ
- **DB/API/UIで確認する内容:** DOM link、route/API
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-052 — 管理者が問い合わせと要望の本文・画像を識別する
- **要件ID:** R-FBK-04
- **テスト層:** システム
- **優先度:** P1
- **目的:** 管理者が問い合わせと要望の本文・画像を識別する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U13
- **使用データ:** F001/F002
- **操作手順:** admin一覧
- **期待結果:** 新しい順、種別、投稿者、本文、画像表示
- **DB/API/UIで確認する内容:** UI、API
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-053 — 通知の自動追加・filter・個別既読・一括既読を操作する
- **要件ID:** R-NOT-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** 通知の自動追加・filter・個別既読・一括既読を操作する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** U07/U08
- **使用データ:** 既読未読混在、別contextから新規DM
- **操作手順:** 通知画面中に相手が送信→自動反映後に各control操作
- **期待結果:** 再読込なしで追加され、件数・badge・dot・focusが同期
- **DB/API/UIで確認する内容:** UI、poll/network、API、DB
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-054 — 全主要画面が401/403/404/409/422/500から回復できる
- **要件ID:** R-ERR-01
- **テスト層:** システム
- **優先度:** P1
- **目的:** 全主要画面が401/403/404/409/422/500から回復できる
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 各状態
- **使用データ:** response interception
- **操作手順:** 各代表画面で失敗→retry
- **期待結果:** 白画面にならず原因別案内、draft保持
- **DB/API/UIで確認する内容:** UI、console、network
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## ST-055 — 全25 routeの背景色・header・footerを一貫表示する
- **要件ID:** R-UI-03
- **テスト層:** システム
- **優先度:** P1
- **目的:** 全25 routeの背景色・header・footerを一貫表示する
- **前提条件:** 専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み
- **使用ユーザー:** 全actor
- **使用データ:** route一覧
- **操作手順:** desktop/mobileで全route撮影
- **期待結果:** 意図しない白/灰面なし、水平段差なし
- **DB/API/UIで確認する内容:** screenshots、computed background
- **証跡:** ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合
- **後処理:** 変更ケースはseed再投入。browser contextと添付画像を破棄
- **失敗時の切り分け方法:** UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け

## UAT-001 — 初見利用者が登録後に次の行動を説明できる
- **要件ID:** R-UX-01
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** 初見利用者が登録後に次の行動を説明できる
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** シナリオだけ提示
- **操作手順:** 「仲間を探すため登録する」と依頼
- **期待結果:** 5分以内、確認mailの必要性を自力理解
- **DB/API/UIで確認する内容:** 観察票、発話、時間
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-002 — 初見利用者が自分に合う募集を探す
- **要件ID:** R-UX-01
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** 初見利用者が自分に合う募集を探す
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** 探す条件を口頭提示
- **操作手順:** 検索方法は教えず依頼
- **期待結果:** 3分以内、filterとsortを説明できる
- **DB/API/UIで確認する内容:** 経路、迷い回数、結果ID
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-003 — 募集と参加希望の違いを初見で判断する
- **要件ID:** R-UX-02
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** 募集と参加希望の違いを初見で判断する
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** P001/P002
- **操作手順:** 2カードを比較して説明してもらう
- **期待結果:** 種別と次行動を正答
- **DB/API/UIで確認する内容:** 発話、誤認箇所
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-004 — 公開前に投稿内容と公開条件を確認できる
- **要件ID:** R-UX-03
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** 公開前に投稿内容と公開条件を確認できる
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** 投稿シナリオ
- **操作手順:** 作成を依頼し最後に確認質問
- **期待結果:** 公開内容・30日・制限を理解
- **DB/API/UIで確認する内容:** 所要時間、戻り操作
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-005 — プロフィールから安全にDMを開始できる
- **要件ID:** R-UX-04
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** プロフィールから安全にDMを開始できる
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** U08 profile
- **操作手順:** 質問を送りたいと依頼
- **期待結果:** 2分以内、宛先を誤らず送信
- **DB/API/UIで確認する内容:** 経路、迷い、送信結果
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-006 — 画像送信失敗から本文を失わず回復する
- **要件ID:** R-UX-05
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** 画像送信失敗から本文を失わず回復する
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** 5MB超→正常PNG
- **操作手順:** 画像付きDMを依頼
- **期待結果:** 原因理解、再選択、本文維持
- **DB/API/UIで確認する内容:** 発話、回復時間
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-007 — 不快な相手をblock・reportし違いを理解する
- **要件ID:** R-UX-06
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** 不快な相手をblock・reportし違いを理解する
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** U10/M002
- **操作手順:** 連絡停止と運営報告を依頼
- **期待結果:** 両操作を区別し完了
- **DB/API/UIで確認する内容:** 選択理由、安心度
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-008 — 通知から未読会話を見つけ返信する
- **要件ID:** R-UX-07
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** 通知から未読会話を見つけ返信する
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** 未読3件
- **操作手順:** 新着へ返信を依頼
- **期待結果:** 2分以内、既読状態を理解
- **DB/API/UIで確認する内容:** 経路、未読誤認
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-009 — スマートフォンで登録・検索・DMを片手操作する
- **要件ID:** R-UX-08
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** スマートフォンで登録・検索・DMを片手操作する
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 新規参加者
- **使用データ:** 390x844
- **操作手順:** 3主要taskを依頼
- **期待結果:** 横scrollなし、zoom不要、CTA到達
- **DB/API/UIで確認する内容:** 録画、誤tap、時間
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## UAT-010 — 退会の影響を理解して中止・実行を選べる
- **要件ID:** R-UX-09
- **テスト層:** ユーザー
- **優先度:** P1
- **目的:** 退会の影響を理解して中止・実行を選べる
- **前提条件:** 専用環境と録画同意を準備。参加者へ操作方法を説明しない
- **使用ユーザー:** 既存参加者
- **使用データ:** U22複製
- **操作手順:** 設定から退会判断を依頼
- **期待結果:** 削除範囲と不可逆性を説明できる
- **DB/API/UIで確認する内容:** 発話、confirm操作
- **証跡:** 観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話
- **後処理:** 作成データをseedで戻し、録画を管理領域へ移す
- **失敗時の切り分け方法:** UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認

## SEC-001 — 匿名の公開GETと保護APIをallowlistどおり分離する
- **要件ID:** R-SEC-01
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 匿名の公開GETと保護APIをallowlistどおり分離する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** 匿名
- **使用データ:** 全route/API inventory
- **操作手順:** GET/変更methodを機械的に巡回
- **期待結果:** 公開だけ200、保護は401/redirect
- **DB/API/UIで確認する内容:** status matrix、Location
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-002 — 未確認mail利用者を確認・再送・logout以外から遮断する
- **要件ID:** R-SEC-02
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 未確認mail利用者を確認・再送・logout以外から遮断する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U01
- **使用データ:** 全API inventory
- **操作手順:** 全method巡回
- **期待結果:** 例外以外403 EMAIL_NOT_VERIFIED、DB不変
- **DB/API/UIで確認する内容:** status/body/DB
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-003 — 停止利用者をsupport・問い合わせ・要望・logout以外から遮断する
- **要件ID:** R-SEC-03
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 停止利用者をsupport・問い合わせ・要望・logout以外から遮断する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U11
- **使用データ:** 全API inventory
- **操作手順:** 画面直URLとAPI直送
- **期待結果:** support/contact/feature-request/logoutだけ成功、投稿/DM/profile変更不可
- **DB/API/UIで確認する内容:** status matrix、feedback保存、他DB不変
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-004 — 退会利用者の旧cookie・資格情報・公開IDを無効化する
- **要件ID:** R-SEC-04
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 退会利用者の旧cookie・資格情報・公開IDを無効化する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U12/U24
- **使用データ:** 退会前sessionと旧行
- **操作手順:** withdraw後に再利用
- **期待結果:** session401、login拒否、個人情報非表示
- **DB/API/UIで確認する内容:** cookie、API、DB
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-005 — 一般利用者から全admin APIを拒否する
- **要件ID:** R-SEC-05
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 一般利用者から全admin APIを拒否する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U02
- **使用データ:** admin endpoint全件
- **操作手順:** GET/PATCH/DELETE
- **期待結果:** すべて403で存在有無も漏らさない
- **DB/API/UIで確認する内容:** status/body、DB不変
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-006 — 管理者権限を該当管理機能だけに限定する
- **要件ID:** R-SEC-05
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 管理者権限を該当管理機能だけに限定する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U13
- **使用データ:** 他人DM画像の報告有/無
- **操作手順:** admin APIとmessage image取得
- **期待結果:** 管理API成功、未報告DM画像403
- **DB/API/UIで確認する内容:** status、reports relation
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-007 — CSRF tokenなし・別session・再利用条件を検証する
- **要件ID:** R-SEC-07
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** CSRF tokenなし・別session・再利用条件を検証する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U02
- **使用データ:** 3session token
- **操作手順:** 全変更API代表
- **期待結果:** 不正組合せ403、同session正tokenだけ通る
- **DB/API/UIで確認する内容:** status、Cookie、DB
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-008 — 保存型・反射型XSSを全自由記述へ無害化する
- **要件ID:** R-SEC-08
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 保存型・反射型XSSを全自由記述へ無害化する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U04/U07
- **使用データ:** script、img onerror、svg、閉じtag
- **操作手順:** profile/post/DM/report/feedback/searchへ投入し再表示
- **期待結果:** 文字表示のみ、script/handler実行0
- **DB/API/UIで確認する内容:** DOM、CSP/console、DB原文
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-009 — 他人ID差替えでprofile/post/message/notification/blockを変更できない
- **要件ID:** R-SEC-09
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 他人ID差替えでprofile/post/message/notification/blockを変更できない
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U07/U23
- **使用データ:** 他人resource IDs
- **操作手順:** URL/body IDを差替え
- **期待結果:** 403/404、対象不変
- **DB/API/UIで確認する内容:** status、owner DB
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-010 — DM画像の/uploads公開URL迂回を防ぐ
- **要件ID:** R-SEC-10
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** DM画像の/uploads公開URL迂回を防ぐ
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** 匿名/U23
- **使用データ:** M002物理名
- **操作手順:** 認可URLと/uploads URLへGET
- **期待結果:** 当事者外は両方401/403/404、byteを返さない
- **DB/API/UIで確認する内容:** status、body length、access log
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-011 — feedback添付の公開範囲を運営だけに制限する
- **要件ID:** R-SEC-10
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** feedback添付の公開範囲を運営だけに制限する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** 匿名/U02/U13
- **使用データ:** F001画像
- **操作手順:** 候補URLを各主体でGET
- **期待結果:** 管理者だけ閲覧できる
- **DB/API/UIで確認する内容:** status、bytes、referer
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-012 — multipartの偽MIME・二重拡張子・polyglot・切断を拒否する
- **要件ID:** R-SEC-11
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** multipartの偽MIME・二重拡張子・polyglot・切断を拒否する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U14
- **使用データ:** 攻撃fixture
- **操作手順:** 全画像endpointへupload
- **期待結果:** 全件4xx、file/DB残骸0
- **DB/API/UIで確認する内容:** status、filesystem、DB
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-013 — login・reset・verifyの列挙と総当たり耐性を確認する
- **要件ID:** R-SEC-12
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** login・reset・verifyの列挙と総当たり耐性を確認する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** 匿名
- **使用データ:** 存在/不存在mail、反復要求
- **操作手順:** 同条件で連続送信
- **期待結果:** 情報差を最小化し制限・監視が働く
- **DB/API/UIで確認する内容:** status/text/time/log
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-014 — session cookie属性とlogout無効化を確認する
- **要件ID:** R-SEC-13
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** session cookie属性とlogout無効化を確認する
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U02
- **使用データ:** HTTP/HTTPS相当設定
- **操作手順:** login responseとlogout後再利用
- **期待結果:** HttpOnly/SameSite、公開時Secure、logout無効
- **DB/API/UIで確認する内容:** Set-Cookie、server session
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-015 — 不正JSON・未知enum・巨大数・重複parameterを安全に400にする
- **要件ID:** R-SEC-14
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 不正JSON・未知enum・巨大数・重複parameterを安全に400にする
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U02
- **使用データ:** malformed、unknown、Long超過
- **操作手順:** 全代表APIへ送信
- **期待結果:** 500なし、内部情報なし、DB不変
- **DB/API/UIで確認する内容:** status/body/log
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-016 — 第三者が任意message IDを通報して本文を取得できない
- **要件ID:** R-RPT-02
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** 第三者が任意message IDを通報して本文を取得できない
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** U23
- **使用データ:** M002
- **操作手順:** POST report後admin payload観察
- **期待結果:** 作成自体を403/404、snapshot新規0
- **DB/API/UIで確認する内容:** reports count、status
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-017 — request IDへ改行・長大値を入れてlog注入できない
- **要件ID:** R-SEC-15
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** request IDへ改行・長大値を入れてlog注入できない
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** 匿名
- **使用データ:** CRLF、Unicode control、10KB
- **操作手順:** X-Request-Id付きGET
- **期待結果:** sanitize/再発行し1request1log
- **DB/API/UIで確認する内容:** response header、ECS JSON lines
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## SEC-018 — password・token・mail本文・画像pathをlogへ残さない
- **要件ID:** R-SEC-16
- **テスト層:** セキュリティ
- **優先度:** P0
- **目的:** password・token・mail本文・画像pathをlogへ残さない
- **前提条件:** 専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用
- **使用ユーザー:** 各actor
- **使用データ:** 認証/失敗/画像操作
- **操作手順:** 一連操作後log検索
- **期待結果:** secret一致0、必要なrequest metadataのみ
- **DB/API/UIで確認する内容:** application/Filebeat/Kibana log
- **証跡:** 要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない
- **後処理:** 全session失効、seed再投入、攻撃fixture削除
- **失敗時の切り分け方法:** 入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認

## NFT-001 — 74投稿の一覧初回と主要API p95を基準内にする
- **要件ID:** R-NFR-01
- **テスト層:** 非機能
- **優先度:** P1
- **目的:** 74投稿の一覧初回と主要API p95を基準内にする
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** 匿名/U02
- **使用データ:** seed全件
- **操作手順:** 各API30回、cold/warm分離
- **期待結果:** 一覧3秒以内、API p95 1秒以内、error0
- **DB/API/UIで確認する内容:** trace、duration、DB query count
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-002 — 二重click・Enter連打で重複登録しない
- **要件ID:** R-NFR-02
- **テスト層:** 非機能
- **優先度:** P1
- **目的:** 二重click・Enter連打で重複登録しない
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** 各作成者
- **使用データ:** 遅延response
- **操作手順:** register/post/DM/report/feedbackを連打
- **期待結果:** 業務上1件、button busy
- **DB/API/UIで確認する内容:** HTTP count、unique rows
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-003 — 同時投稿で公開1件制限を破らない
- **要件ID:** R-NFR-03
- **テスト層:** 非機能
- **優先度:** P1
- **目的:** 同時投稿で公開1件制限を破らない
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** U18
- **使用データ:** 2 parallel requests
- **操作手順:** barrierで同時POST
- **期待結果:** 1成功1競合、OPEN 1件
- **DB/API/UIで確認する内容:** responses、transaction log、DB
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-004 — 同時画像追加で5枚上限を破らない
- **要件ID:** R-NFR-03
- **テスト層:** 非機能
- **優先度:** P1
- **目的:** 同時画像追加で5枚上限を破らない
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** U14
- **使用データ:** P009が4枚、2画像
- **操作手順:** parallel upload
- **期待結果:** 合計5以下、orphan0
- **DB/API/UIで確認する内容:** responses、DB/files
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-005 — 同時初回DMで会話一意制約エラーを利用者へ露出しない
- **要件ID:** R-NFR-03
- **テスト層:** 非機能
- **優先度:** P1
- **目的:** 同時初回DMで会話一意制約エラーを利用者へ露出しない
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** U07/U08
- **使用データ:** 双方同時send
- **操作手順:** barrierで送信
- **期待結果:** 会話1、message2、5xx0
- **DB/API/UIで確認する内容:** responses、DB、log
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-006 — seed再投入を3回行い同じ固定状態へ戻す
- **要件ID:** R-NFR-04
- **テスト層:** 非機能
- **優先度:** P1
- **目的:** seed再投入を3回行い同じ固定状態へ戻す
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** 専用DB
- **使用データ:** seed SQL
- **操作手順:** 各実行後manifest/count/checksum
- **期待結果:** 全回成功、84 users/74 posts、重複0
- **DB/API/UIで確認する内容:** psql log、集計checksum
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-007 — 2利用者のsession・検索履歴・通知・下書きを分離する
- **要件ID:** R-NFR-05
- **テスト層:** 非機能
- **優先度:** P1
- **目的:** 2利用者のsession・検索履歴・通知・下書きを分離する
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** U07/U08
- **使用データ:** 2 browser contexts
- **操作手順:** 同時に異なる操作
- **期待結果:** 相互混入0
- **DB/API/UIで確認する内容:** cookies、API IDs、DB owner
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-008 — 主要画面を375/390/768/1440幅で操作可能にする
- **要件ID:** R-NFR-06
- **テスト層:** 非機能
- **優先度:** P2
- **目的:** 主要画面を375/390/768/1440幅で操作可能にする
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** 全actor
- **使用データ:** 全25 routes
- **操作手順:** 各viewportで巡回・主要操作
- **期待結果:** 横scrollなし、44px target、固定要素被りなし
- **DB/API/UIで確認する内容:** full screenshots、geometry
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-009 — 全機能をkeyboardだけで完了する
- **要件ID:** R-NFR-07
- **テスト層:** 非機能
- **優先度:** P2
- **目的:** 全機能をkeyboardだけで完了する
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** 全actor
- **使用データ:** 全interactive controls
- **操作手順:** Tab/ShiftTab/Enter/Space/矢印/Escape
- **期待結果:** 順序論理的、focus常時可視、trapなし
- **DB/API/UIで確認する内容:** 録画、activeElement列
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-010 — 名前・役割・状態・errorを支援技術へ伝える
- **要件ID:** R-NFR-08
- **テスト層:** 非機能
- **優先度:** P2
- **目的:** 名前・役割・状態・errorを支援技術へ伝える
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** 全actor
- **使用データ:** 主要route
- **操作手順:** accessibility snapshotとscreen reader確認
- **期待結果:** label/heading/live region/dialog妥当
- **DB/API/UIで確認する内容:** AX tree、読み上げ記録
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-011 — 本文・control・focusのcontrastをAA基準にする
- **要件ID:** R-NFR-09
- **テスト層:** 非機能
- **優先度:** P2
- **目的:** 本文・control・focusのcontrastをAA基準にする
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** 全route
- **使用データ:** 通常/hover/focus/disabled/error
- **操作手順:** computed colorを測定
- **期待結果:** 本文4.5:1、大文字等3:1、UI/focus3:1
- **DB/API/UIで確認する内容:** 測定表、screenshots
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-012 — 長文・日本語・絵文字・合成文字でlayoutと保存を壊さない
- **要件ID:** R-NFR-10
- **テスト層:** 非機能
- **優先度:** P2
- **目的:** 長文・日本語・絵文字・合成文字でlayoutと保存を壊さない
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** U04/U07
- **使用データ:** 上限文字、改行、emoji、結合文字
- **操作手順:** 全自由記述へ保存・表示
- **期待結果:** 切断規則一貫、overflowなし、再読込同値
- **DB/API/UIで確認する内容:** DOM geometry、API/DB length
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

## NFT-013 — DBのNOT NULL・FK・UNIQUE・CHECK制約が最終防衛線になる
- **要件ID:** R-NFR-11
- **テスト層:** 非機能
- **優先度:** P1
- **目的:** DBのNOT NULL・FK・UNIQUE・CHECK制約が最終防衛線になる
- **前提条件:** 専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録
- **使用ユーザー:** 専用DB直接接続
- **使用データ:** users email重複、会話pair重複、block pair重複、孤児FK、未知enum、必須null
- **操作手順:** 各違反を個別transactionでINSERT/UPDATEしrollback
- **期待結果:** 全違反をDBが拒否し既存行不変、アプリ経由違反は4xxで500非公開
- **DB/API/UIで確認する内容:** SQLSTATE、constraint名、行数、API status/log
- **証跡:** 動画/画像、計測生データ、集計方法、DB件数、環境情報
- **後処理:** 負荷・network interceptionを解除しseed再投入
- **失敗時の切り分け方法:** 再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解

