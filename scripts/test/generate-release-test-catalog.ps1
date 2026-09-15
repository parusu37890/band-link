$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$out = Join-Path $root 'docs/test-plan'
New-Item -ItemType Directory -Force $out | Out-Null

$rows = @'
UT-001§R-FBK-03§単体§P0§FeedbackServiceが通常のSpring構成で生成できる§なし§mock repositories§ApplicationContextへrepositoryだけ登録してservice取得§Bean生成と問い合わせ保存が成功§生成Bean、保存引数
UT-002§R-FBK-01§単体§問い合わせと機能要望を別種別で保存する§U04§改行と前後空白を含む本文§2種別を順にserviceへ渡す§本文をtrimし種別と時刻を正しく保存§Feedback全列
UT-003§R-FBK-02§単体§空の自由記述を拒否する§U04§null、空、空白のみ§各値で作成を呼ぶ§例外となりrepositoryへ触れない§例外型、副作用0
UT-004§R-IMG-04§単体§feedback画像URLの外部・走査・不正拡張子を拒否する§U04§https、../、svg、query、javascript§各URLで作成を呼ぶ§全件拒否し保存0§URL検査、save回数
UT-005§R-FBK-01§単体§存在しない利用者のfeedbackを作らない§不明ID§userId=99§作成を呼ぶ§利用者不明で失敗し孤児行0§find結果、save回数
UT-006§R-FBK-02§単体§自由記述3000文字と画像URL1000文字の境界§U04§2999/3000/3001、999/1000/1001文字§Bean Validationを実行§上限以下だけ制約違反0§constraint pathとmessage
UT-007§R-AUTH-04§単体§未確認利用者のDM送信を拒否する§U01§本文hello§sendを呼ぶ§会話・message・通知を一切作らない§RuleViolation、副作用0
UT-008§R-SEC-03§単体§停止・退会状態の送受信を拒否する§U07/U11/U24§sender/recipient各状態§組合せごとにsend§全組合せ拒否§会話、message、通知0
UT-009§R-MSG-01§単体§自分宛DMを拒否する§U07§recipient=U07§sendを呼ぶ§業務例外、副作用0§例外、repository呼出し
UT-010§R-BLK-03§単体§どちら向きのブロックでも送信不可§U09/U10§blocker→blockedと逆向き§各向きでsend§双方とも拒否§block照会、message0
UT-011§R-MSG-03§単体§本文も画像もないDMと公開画像URLを拒否する§U07/U08§null、空白、/uploads/x.png§sendを呼ぶ§全件拒否§正規化結果、副作用0
UT-012§R-MSG-02§単体§既存会話を再利用して受信者へ通知する§U07/U08§C001、前後空白本文§逆方向からsend§会話重複なし、本文trim、lastMessage更新、通知1§全entityとsave回数
UT-013§R-MSG-03§単体§画像だけのDMを許可する§U07/U08§認可URL§content=nullでsend§画像URLを持つmessageと通知を保存§Message/Notification
UT-014§R-MSG-05§単体§第三者の会話一覧・既読・購読を拒否する§U23§C001§messages、markRead、requireParticipant§全操作を拒否§例外と更新0
UT-015§R-MSG-06§単体§既読は相手の未読だけを一度更新する§U07/U08§受信未読、送信、自身既読§markReadを2回§受信未読だけT0、再実行で不変§readAt、通知不変
UT-016§R-MSG-03§単体§DM本文1000文字境界§U07§1000/1001文字、画像URL1001文字§Bean Validation§1000だけ許可§constraint path
UT-017§R-IMG-01§単体§画像保存・読込・削除を専用dir内で完結する§U14§有効PNG、走査を含む元名§store/load/delete§UUID名で往復し外へ書かない§実path、byte列
UT-018§R-IMG-02§単体§MIME欠落時に拡張子推定し実体照合する§U14§null/空/octet-stream、PNG§各MIMEでstore§PNGだけ受理し偽jpg拒否§返却拡張子、保存数
UT-019§R-IMG-03§単体§空・5MB超・未対応画像を拒否する§U14§null、0byte、5MB+1、SVG§各fileでstore§全件拒否しdir空§例外、ファイル数
UT-020§R-IMG-03§単体§5MBちょうどを許可する§U14§5,242,880byte PNG§store§保存成功§実サイズとURL
UT-021§R-IMG-04§単体§画像読込のpath traversalを拒否する§U23§../、backslash、絶対path、query§load§全件拒否§root外アクセス0
UT-022§R-IMG-02§単体§シグネチャだけの切断画像を拒否する§U14§8byte PNG header§store§デコード不能として拒否§例外、ファイル0
UT-023§R-BLK-01§単体§自己ブロックを拒否する§U09§同一ID§block§例外、副作用0§repository interaction
UT-024§R-BLK-01§単体§ブロック再送を冪等に扱う§U09/U10§同一組合せ2回§blockを2回§関係は1件§save回数1
UT-025§R-BLK-02§単体§解除は自分向きだけを削除し再送はno-op§U09/U10§既存関係§unblockを2回§1回だけ削除§delete回数と向き
UT-026§R-AUTH-01§単体§登録の名前・mail・password境界を検証する§新規§名前0/1/80/81、mail320/321、password7/8/128/129§Bean Validationを全組合せ実行§定義範囲だけ有効§constraint field
UT-027§R-PROF-02§単体§年齢0..120と経験0..100の境界§U15/U16§-1/0/120/121、-1/0/100/101§登録・profile DTOを検証§上下限内だけ有効§age/experience constraint
UT-028§R-PROF-03§単体§性別を男性・女性だけに限定する§U04§男性、女性、未回答、その他、空白付き§DTO validation§完全一致2値だけ有効§gender constraint
UT-029§R-PROF-04§単体§プロフィール必須マスタと活動エリア1..3件を検証する§U03/U04§空、1、3、4、未知ID§validationとservice更新§空と4件と未知IDを拒否§関連行数と例外
UT-030§R-POST-04§単体§投稿文字数・必須値の境界を検証する§U05§title0/1/30/31、body0/1/500/501、area100/101§DTO validation§上限以下かつ非空だけ有効§constraint field
UT-031§R-POST-05§単体§投稿選択上限と未知masterを拒否する§U05§part0/5/6、genre0/3/4、stance0/1/2、pref0/3/4§service作成§承認上限内・存在IDだけ保存§関連件数、例外
UT-032§R-POST-06§単体§公開・終了・期限切れ・再公開の状態遷移を固定する§U20§P006§expire、close、reopen順に実行§許可遷移だけ成立し理由・時刻が整合§status/reason/timestamps
UT-033§R-POST-07§単体§12時間編集と順位更新の境界§U17-U20§11:59:59、12:00:00、12:00:01§update/reopen§境界時刻から許可§lastEdited/rankUpdated
UT-034§R-PRES-01§単体§オンライン5分と最終ログイン区分の境界§U05/U06§4:59、5:00、5:01、未来、なし、3/7/30日§DTO変換§仕様区分だけ返し生時刻を漏らさない§JSON対象field
UT-035§R-SEC-06§単体§公開DTOにmail・hash・正確な活動時刻を含めない§U04§全個人field設定済みUser§全公開DTOへ変換§許可fieldだけ返す§reflection/JSON field集合
IT-001§R-AUTH-01§結合§登録が利用者と必須プロフィール関連を原子的に保存する§新規§一意mail、全選択1件§CSRF付きPOST /api/auth/register§201、session作成、全関連保存§usersと4関連表、response
IT-002§R-AUTH-02§結合§login成功・失敗・logout後session無効§U02§正解/誤りpassword§login→me→logout→me§200→200→204→401§session id、lastLoginAt
IT-003§R-AUTH-03§結合§同一sessionで再login時にsession idを更新する§U02§既存anonymous session§csrf取得後login§固定化されず認証状態維持§Set-Cookie/session id
IT-004§R-AUTH-04§結合§確認tokenの有効・期限切れ・使用済み・再利用§U01§3固定token§POST verify-emailを順に実行§有効だけ204、他は4xx、再利用不可§verifiedAt、usedAt
IT-005§R-AUTH-05§結合§確認再送が旧tokenを無効化し新規1件を作る§U01§未使用token§認証sessionでresend§202、旧無効、新規期限24h§token行数・値非ログ
IT-006§R-AUTH-06§結合§再設定依頼はmail存在を応答から漏らさない§U02/未知mail§2つのmail§POST request§双方202、存在時だけtoken§status/body/time差、DB
IT-007§R-AUTH-07§結合§再設定tokenの有効・期限・使用済み・再利用§U02§3固定token§confirm後に新旧passwordでlogin§有効1回のみ更新§hash、usedAt、login結果
IT-008§R-AUTH-08§結合§退会が全関連データと画像参照を削除し再登録可能にする§U22§P012/C004/F003/画像§withdraw→DB照合→同mail登録§204、個人データ0、再登録201§全FK表、files、session
IT-009§R-PROF-01§結合§本人プロフィールと公開プロフィールの契約を分離する§U04/U23§U04全field§GET meとGET /{id}§本人のみmail/status/role、公開は非公開fieldなし§JSON key集合
IT-010§R-PROF-04§結合§profile更新で関連集合を置換し未知IDを拒否する§U04§全master変更、未知999999§PUTを2回§正常時置換、異常時全rollback§user関連4表
IT-011§R-IMG-01§結合§profile画像差替えで新規保存後に旧画像を削除する§U04§既存PNGと新JPEG§multipart POST§URL更新、旧file削除、新file存在§users、filesystem、MIME
IT-012§R-POST-01§結合§2種別の投稿をAPI契約どおり作成する§U05/U06§全必須field§POST /api/posts§201、型・30日期限・関連保存§response、posts、関連表
IT-013§R-POST-02§結合§他人の投稿編集・終了・再公開を拒否する§U07§P001/P005§PUT/PATCH§全て403相当、DB不変§status/body/updatedAt
IT-014§R-POST-03§結合§同一利用者の募集・加入希望を各1件に制限する§U05§既存の募集1件、同種別の募集、異種別の加入希望§同種別POST→異種別POST§同種別は競合・異種別は成功、OPENが種別ごとに1件§user/type/status別posts count、status
IT-015§R-POST-06§結合§期限到来時に一覧から消え詳細は扱いどおりになる§U21§P007、T0前後§一覧・詳細取得§期限後一覧非表示、状態と理由整合§posts status/reason、API
IT-016§R-POST-08§結合§本文と画像の一括保存・5枚上限・並べ替え・削除をDBとfilesで一致させる§U14§P009、追加画像、途中upload失敗§本文+複数画像作成→6枚目追加→並替→削除§一括成功または全rollback、上限拒否、順序一意、削除fileなし§posts/post_images、filesystem、orphan
IT-017§R-SRCH-01§結合§検索APIは選択式条件だけを扱う§匿名§全選択式条件とkeyword付き要求§各条件でGET§選択式条件だけが作用しkeywordは契約・履歴に残らない§request contract、SQL、JSON IDs、history
IT-018§R-SRCH-02§結合§同一条件OR・異種条件ANDで検索する§匿名§複数masterのP001§複数query parameter§集合論どおり重複なし§IDs、件数、distinct
IT-019§R-SRCH-03§結合§最新順・ログイン順のtie breakが安定する§匿名§同時刻fixture§各sortで2回取得§順序安定、欠落重複なし§ID列、rank/login時刻
IT-020§R-SRCH-04§結合§cursorの正常・不正・巨大・limit境界§匿名§74投稿§cursor null/12/abc/巨大、limit0/1/50/51§500なし、規定件数、連結で全件一度§status、nextCursor、ID集合
IT-021§R-SRCH-05§結合§検索履歴は認証時だけ全条件を重複なく直近5件に保つ§U02/匿名§全選択式filters/type/sort、同条件、6条件、page cursor§検索→同条件再検索→6条件検索→続きを取得→history取得§認証時だけ保存、同条件は最新へ移動、最大5、page追加0、条件欠落なし§search_histories件数/順序/hash/JSON
IT-022§R-MSG-02§結合§双方向の初回同時送信でも会話を1件にする§U07/U08§barrier付き2要求§同時POST§会話1件、message2件、500なし§unique制約、response
IT-023§R-MSG-03§結合§DM送信と通知保存を同一transactionにする§U07/U08§通知save障害§POST send§失敗時messageもrollback§messages/notifications count
IT-024§R-MSG-05§結合§DM画像は当事者と対象通報を見る管理者だけ取得できる§U07/U08/U13/U23§C001画像、報告有無画像§各sessionでGET§当事者200、該当管理者200、第三者403、未報告管理者403§status、byte/MIME
IT-025§R-MSG-06§結合§個別・一括既読が本人通知だけを更新する§U07/U08§既読/未読混在§PATCH individual/read-all§対象だけ更新、再送冪等§notification readAt
IT-026§R-BLK-03§結合§ブロック関係を投稿検索とDMの双方へ反映する§U09/U10§P010/P011/C002§block前後に一覧・送信§双方の投稿非表示、送信拒否、解除後復旧§API IDs、block row
IT-027§R-RPT-02§結合§DM通報は参加者だけが対象message snapshotを固定する§U07/U23§M002§両者でPOST report§参加者だけ成功、前後本文なし§reports snapshot、件数
IT-028§R-ADM-01§結合§一般利用者と管理者の管理API権限を分離する§U02/U13§reports/feedback/users/posts admin paths§各roleで全method§一般403、管理者は規定成功§status、監査対象行
IT-029§R-FBK-01§結合§feedback本文と画像URLを利用者へ紐付ける§U04§問い合わせ・要望各1件§upload→POST2種§201、2種別を本人IDで保存§feedback、file
IT-030§R-SEC-01§結合§全変更APIがCSRFなしを拒否し有効tokenを受理する§U02§全POST/PUT/PATCH/DELETE代表値§tokenなし/偽/正で送信§なし・偽403、正は認可/validation段階へ§status、DB副作用
ST-001§R-AUTH-01§システム§初見登録を完了し確認案内へ到達する§新規§一意mail、正常プロフィール§/registerをキーボードだけで入力・送信§201相当、ログイン状態、確認案内表示§UI、me API、users
ST-002§R-AUTH-01§システム§登録全項目の必須・境界エラーを入力位置へ示す§新規§空、上限超、年齢文字§順に入力して送信§送信せず対象fieldへfocus、理解できる日本語§UI validation、API未送信
ST-003§R-AUTH-02§システム§login成功・誤password・未知mailを回復できる§U02§正誤資格情報§3パターン実行§成功は一覧、失敗は入力保持と安全な案内§UI、login status、session
ST-004§R-AUTH-08§システム§logout後に戻る操作でも認証画面を再表示しない§U02§login済み§logout→browser back→保護URL§認証情報・個人画面が復元されない§UI、me=401、cache
ST-005§R-AUTH-04§システム§未確認利用者の許可・禁止導線を統一する§U01§未確認session§posts/messages/settings/supportへ直URL§verify画面へ誘導、再送とlogoutだけ可能§route、各API status
ST-006§R-AUTH-04§システム§確認mailのlinkで一度だけ確認完了する§U01§SMTP受信link§linkを開き再読込§成功案内と一覧遷移、再利用は安全な案内§mail、UI、token DB
ST-007§R-AUTH-05§システム§確認再送の連打を二重送信にしない§U01§遅延SMTP§再送を素早く2回操作§busy表示、1回だけ有効token§UI disabled、mail/DB件数
ST-008§R-AUTH-06§システム§password再設定依頼から新password loginまで完了する§U02§SMTP受信code§依頼→confirm→login§成功し旧password拒否§UI、API、DB hash非表示
ST-009§R-AUTH-06§システム§存在しないmailでもアカウント有無を画面に出さない§未知§未登録mail§再設定依頼§登録済みと同じ文言・遷移§UI text、status
ST-010§R-AUTH-09§システム§LINE無効時はボタン非表示、有効時はstate付き遷移§新規/U25§設定なし・テストチャネル§login/registerを表示しOAuth往復§設定別表示、state不一致拒否、成功後適切な遷移§UI、redirect、session
ST-011§R-PROF-01§システム§公開プロフィールが連絡判断情報を読みやすく示す§匿名/U04§完全profile§/users/910004表示§名前、年齢、性別、経験、地域、part、genre、stance、bio、media表示§UIとpublic API
ST-012§R-PROF-01§システム§未完成・停止・退会プロフィールの表示を安全に扱う§U03/U11/U24§各公開URL§匿名で順に開く§欠落で崩れず、停止/退会は規定の匿名化・非公開§UI、status
ST-013§R-PROF-04§システム§プロフィール全必須項目を編集保存し再読込で保持する§U03§全正常値§settings/profile入力→保存→reload§完全状態になり同値表示§UI、me API、関連DB
ST-014§R-PROF-02§システム§年齢と経験を自由入力しspinnerを表示しない§U04§0、120、0、100§mouse/keyboardで入力§上下余白、矢印なし、保存可能§screenshot、input type/style
ST-015§R-PROF-03§システム§性別は男性・女性の二択で単一選択する§U04§2値§Tab/矢印/Space操作§1つだけ選択、青線の不要な残留なし§UI、request body
ST-016§R-PROF-04§システム§地域最大3件と各マスタ必須を画面で止める§U04§0/1/3/4選択§選択・解除・保存§0と4を明示、1..3保存§UI count、API、DB
ST-017§R-PROF-05§システム§profile画像を選択・preview・差替え・削除する§U04§PNG/JPEG/不正画像§各操作後reload§正常画像だけ反映し旧画像残存なし§UI、API、files
ST-018§R-PROF-06§システム§YouTube/TikTok/SoundCloud/Spotify/Apple Musicを安全に表示する§U04§正常、短縮、不正host、javascript URL§保存して公開profile表示§対応URLだけembed/安全link、不正は実行不可§DOM src/href、console
ST-019§R-PRES-01§システム§オンライン・最終ログイン・投稿時刻を境界どおり示す§U05/U06/U02§5分境界と投稿後1分・59分・1時間・1日・1週間・3週間・4週間・4週間超§一覧・詳細・profile表示§5分以内だけonline、投稿時刻は4週間前を表示上限にする§UI text、API fields
ST-020§R-POST-01§システム§募集を段階入力・preview・公開する§U18§正常全fieldと画像2枚§newで全stepをkeyboard操作§確認内容一致、公開後detail§UI、POST body、DB
ST-021§R-POST-01§システム§加入希望の種別と見出しを明確に公開する§U06再seed後§WANTS_TO_JOIN§作成し一覧・詳細を開く§募集と混同せず大きく表示§UI label、type API
ST-022§R-POST-04§システム§投稿の空・文字数超過・不足選択と活動頻度6択をstep内で確認する§U18§境界値、週2回以上/週1回/月2〜3回/月1回/不定期/相談§各値を選び各stepで次へ/公開§6択が選べ、違反時は進行を止め該当fieldへfocus、入力保持§UI options、request enum、API未作成
ST-023§R-POST-02§システム§自分の募集を編集し12時間制限を説明する§U18/U17§P003/P004§edit→保存§U18成功、U17は理由と次の可能時刻§UI、PUT status、DB
ST-024§R-POST-06§システム§募集終了確認と再公開後も既存DMを継続する§U20/U07§P006と既存会話§終了→既存DM送受信→再公開をconfirm含め操作§状態表示と一覧反映、終了中もDM成功、二重操作なし§UI、API、posts/messages DB
ST-025§R-POST-06§システム§本人削除と管理削除・停止終了を区別し不正再公開を防ぐ§U06/U11§本人所有投稿、P008/P014§my/postsで削除確認後、各終了理由の再公開を画面と直APIで試す§本人だけ削除可能、管理削除/停止はCTAなし・直API拒否、理由保持§UI、API status、posts/画像
ST-026§R-POST-08§システム§投稿画像5枚のpreview・並替え・拡大・削除§U14§P009§drag/keyboard代替、画像click、delete§順序保存、modal拡大、削除反映、6枚目拒否§DOM、API、DB/files
ST-027§R-SRCH-01 / R-UI-03§システム§キーワード欄とheader直下の重複見出しを表示しない§匿名§keyword付きURLと選択式条件§desktop/mobileで一覧表示・条件選択・解除§keyword欄と重複見出しなし、keywordはURL/APIに残らない§DOM、見出し階層、URL、API query
ST-028§R-SRCH-02§システム§全filterのAND/ORと年齢不問の一致規則を理解できる§匿名§全master、年代複数、ANY、任意未入力§同項目複数・異項目・ANYを選択して解除§OR/ANDとANY規則どおり、summary・件数・結果が同期§UI、query、IDs
ST-029§R-SRCH-03§システム§最新順・ログイン順切替に不要な青枠を残さない§匿名§74投稿§selectをmouse/keyboardで切替§順序更新、focusは見失わず不要な線なし§screenshot、ID順
ST-030§R-SRCH-04§システム§下端scrollの自動追加で重複・飛び・scroll jumpがない§匿名§74投稿§下端へ繰り返しscrollして最後まで読む§自動で次頁を追加し全74件が一度、操作位置維持§DOM ID集合、API cursors、scroll位置
ST-031§R-SRCH-04§システム§検索中の通信失敗から同条件で再試行する§匿名§API offline/500§検索→失敗→復旧→retry§理解できる案内、条件保持、成功§UI、network log
ST-032§R-SRCH-05§システム§認証時の検索履歴を確認し匿名時は残さない§U02/匿名§全条件§検索→履歴API§認証分だけ時系列保存§UI/API/DB
ST-033§R-UI-01§システム§募集詳細の本文・条件・投稿者情報を同じ基準線で示す§匿名§P001§detailを表示§投稿者位置と見出し線が揃い背景一色§1440 screenshot
ST-034§R-UI-02§システム§プロフィールを読む・募集一覧への導線が見つけやすい§匿名§P001/U05§詳細から往復§44px以上、文字可読、focus可視§UI geometry、tab order
ST-035§R-MSG-01§システム§profileのメッセージ送信からDM作成へ到達する§U07§U08 profile§CTA click§相手名と入力欄が表示され送信可能§URL、UI、API
ST-036§R-MSG-02§システム§会話未選択時はX風の一覧中心画面として自然に見える§U07§複数会話§/messages表示§不要な説明文なし、一覧から選択できる§desktop/mobile screenshot
ST-037§R-MSG-03§システム§本文DMをEnter規則と送信ボタンで送る§U07/U08§日本語、改行、1000文字§入力・送信§「送信」だけ表示、←なし、1件追加§UI、POST、DB
ST-038§R-MSG-03§システム§PNG/JPEG/WebP画像DMと画像のみ送信§U07/U08§3正常画像§選択→preview→送信§エラーなく表示、本文なし可§upload/send API、files
ST-039§R-MSG-03§システム§不正・超過画像から選択をやり直せる§U07§empty/svg/mismatch/truncated/5MB+1§各file選択・送信§明確な制限案内、draft保持、再選択成功§UI、status、files orphan0
ST-040§R-MSG-04§システム§DM画像clickでdialog拡大しEsc・外側・閉じるで戻る§U07/U08§M002画像§mouse/keyboardで拡大・閉じる§原寸内表示、focus復帰、背景操作不可§dialog DOM、screenshot
ST-041§R-MSG-02§システム§連続送受信を会話順・自他配置・時刻順で表示する§U07/U08§同秒含む10件§交互送信§欠落重複なし、左右と色で識別§DOM、API IDs
ST-042§R-MSG-06§システム§会話を開くと相手の未読だけ既読になる§U07/U08§C001§一覧badge確認→open§badge/通知dot更新、自分送信不変§UI、read API、DB
ST-043§R-MSG-07§システム§SSE切断・復帰と非表示tab復帰で欠落を回収する§U07/U08§network断§送信中に切断→復旧§案内後refreshで一度だけ表示§EventSource、GET、DOM IDs
ST-044§R-BLK-01§システム§profileから確認後にblockし管理画面へ移る§U09§U10 profile§block CTA→confirm§関係1件、一覧表示、DM不可§UI、API、DB
ST-045§R-BLK-02§システム§block解除後に一覧とDMが復旧する§U09§U10 blocked§settings/blocksから解除§確認後消え、投稿・送信復旧§UI、API、DB
ST-046§R-RPT-01§システム§投稿・利用者・DMを理由付きで通報する§U07§P002/U11/M002§各dialogをkeyboard操作§1000文字以内を1件ずつ保存、完了toast§UI、reports DB
ST-047§R-ADM-01§システム§管理者がstatus別通報とsnapshotだけを確認する§U13§R001..R004§adminでfilter切替§対象messageだけ、前後会話なし§UI、API payload
ST-048§R-ADM-02§システム§管理者が通報状態変更・投稿非公開・利用停止解除する§U13§P002/U06/R001§confirm後各操作§対象だけ変化、再読込一致§UI、API、DB
ST-049§R-FBK-01§システム§login利用者が問い合わせを自由記述と画像で送る§U04§改行本文、PNG§header→contact→送信§完了案内、CONTACT保存§UI、API、DB/file
ST-050§R-FBK-01§システム§login利用者が機能要望を別画面・種別で送る§U04§要望本文、画像なし§header→feature request→送信§CONTACTと混同せず保存§UI、API、DB
ST-051§R-FBK-03§システム§未login headerに問い合わせ・機能要望を表示しない§匿名§/, /posts, /users/910004§各画面を表示§header/footer双方に導線なし、直URLはloginへ§DOM link、route/API
ST-052§R-FBK-04§システム§管理者が問い合わせと要望の本文・画像を識別する§U13§F001/F002§admin一覧§新しい順、種別、投稿者、本文、画像表示§UI、API
ST-053§R-NOT-01§システム§通知の自動追加・filter・個別既読・一括既読を操作する§U07/U08§既読未読混在、別contextから新規DM§通知画面中に相手が送信→自動反映後に各control操作§再読込なしで追加され、件数・badge・dot・focusが同期§UI、poll/network、API、DB
ST-054§R-ERR-01§システム§全主要画面が401/403/404/409/422/500から回復できる§各状態§response interception§各代表画面で失敗→retry§白画面にならず原因別案内、draft保持§UI、console、network
ST-055§R-UI-03§システム§全25 routeの背景色・header・footerと一覧冒頭を一貫表示する§全actor§route一覧§desktop/mobileで全route撮影§意図しない白/灰面なし、募集一覧はheader直後から種別タブ・検索操作§screenshots、computed background、見出し階層
UAT-001§R-UX-01§ユーザー§初見利用者が登録後に次の行動を説明できる§新規参加者§シナリオだけ提示§「仲間を探すため登録する」と依頼§5分以内、確認mailの必要性を自力理解§観察票、発話、時間
UAT-002§R-UX-01§ユーザー§初見利用者が自分に合う募集を探す§新規参加者§探す条件を口頭提示§検索方法は教えず依頼§3分以内、filterとsortを説明できる§経路、迷い回数、結果ID
UAT-003§R-UX-02§ユーザー§募集と加入希望の違いを初見で判断する§新規参加者§P001/P002§2カードを比較して説明してもらう§種別と次行動を正答§発話、誤認箇所
UAT-004§R-UX-03§ユーザー§公開前に投稿内容と公開条件を確認できる§新規参加者§投稿シナリオ§作成を依頼し最後に確認質問§公開内容・30日・制限を理解§所要時間、戻り操作
UAT-005§R-UX-04§ユーザー§プロフィールから安全にDMを開始できる§新規参加者§U08 profile§質問を送りたいと依頼§2分以内、宛先を誤らず送信§経路、迷い、送信結果
UAT-006§R-UX-05§ユーザー§画像送信失敗から本文を失わず回復する§新規参加者§5MB超→正常PNG§画像付きDMを依頼§原因理解、再選択、本文維持§発話、回復時間
UAT-007§R-UX-06§ユーザー§不快な相手をblock・reportし違いを理解する§新規参加者§U10/M002§連絡停止と運営報告を依頼§両操作を区別し完了§選択理由、安心度
UAT-008§R-UX-07§ユーザー§通知から未読会話を見つけ返信する§新規参加者§未読3件§新着へ返信を依頼§2分以内、既読状態を理解§経路、未読誤認
UAT-009§R-UX-08§ユーザー§スマートフォンで登録・検索・DMを片手操作する§新規参加者§390x844§3主要taskを依頼§横scrollなし、zoom不要、CTA到達§録画、誤tap、時間
UAT-010§R-UX-09§ユーザー§退会の影響を理解して中止・実行を選べる§既存参加者§U22複製§設定から退会判断を依頼§削除範囲と不可逆性を説明できる§発話、confirm操作
SEC-001§R-SEC-01§セキュリティ§匿名の公開GETと保護APIをallowlistどおり分離する§匿名§全route/API inventory§GET/変更methodを機械的に巡回§公開だけ200、保護は401/redirect§status matrix、Location
SEC-002§R-SEC-02§セキュリティ§未確認mail利用者を確認・再送・logout以外から遮断する§U01§全API inventory§全method巡回§例外以外403 EMAIL_NOT_VERIFIED、DB不変§status/body/DB
SEC-003§R-SEC-03§セキュリティ§停止利用者をsupport・問い合わせ・要望・logout以外から遮断する§U11§全API inventory§画面直URLとAPI直送§support/contact/feature-request/logoutだけ成功、投稿/DM/profile変更不可§status matrix、feedback保存、他DB不変
SEC-004§R-SEC-04§セキュリティ§退会利用者の旧cookie・資格情報・公開IDを無効化する§U12/U24§退会前sessionと旧行§withdraw後に再利用§session401、login拒否、個人情報非表示§cookie、API、DB
SEC-005§R-SEC-05§セキュリティ§一般利用者から全admin APIを拒否する§U02§admin endpoint全件§GET/PATCH/DELETE§すべて403で存在有無も漏らさない§status/body、DB不変
SEC-006§R-SEC-05§セキュリティ§管理者権限を該当管理機能だけに限定する§U13§他人DM画像の報告有/無§admin APIとmessage image取得§管理API成功、未報告DM画像403§status、reports relation
SEC-007§R-SEC-07§セキュリティ§CSRF tokenなし・別session・再利用条件を検証する§U02§3session token§全変更API代表§不正組合せ403、同session正tokenだけ通る§status、Cookie、DB
SEC-008§R-SEC-08§セキュリティ§保存型・反射型XSSを全自由記述へ無害化する§U04/U07§script、img onerror、svg、閉じtag§profile/post/DM/report/feedback/searchへ投入し再表示§文字表示のみ、script/handler実行0§DOM、CSP/console、DB原文
SEC-009§R-SEC-09§セキュリティ§他人ID差替えでprofile/post/message/notification/blockを変更できない§U07/U23§他人resource IDs§URL/body IDを差替え§403/404、対象不変§status、owner DB
SEC-010§R-SEC-10§セキュリティ§DM画像の/uploads公開URL迂回を防ぐ§匿名/U23§M002物理名§認可URLと/uploads URLへGET§当事者外は両方401/403/404、byteを返さない§status、body length、access log
SEC-011§R-SEC-10§セキュリティ§feedback添付の公開範囲を運営だけに制限する§匿名/U02/U13§F001画像§候補URLを各主体でGET§管理者だけ閲覧できる§status、bytes、referer
SEC-012§R-SEC-11§セキュリティ§multipartの偽MIME・二重拡張子・polyglot・切断を拒否する§U14§攻撃fixture§全画像endpointへupload§全件4xx、file/DB残骸0§status、filesystem、DB
SEC-013§R-SEC-12§セキュリティ§login・reset・verifyの列挙と総当たり耐性を確認する§匿名§存在/不存在mail、反復要求§同条件で連続送信§情報差を最小化し制限・監視が働く§status/text/time/log
SEC-014§R-SEC-13§セキュリティ§session cookie属性とlogout無効化を確認する§U02§HTTP/HTTPS相当設定§login responseとlogout後再利用§HttpOnly/SameSite、公開時Secure、logout無効§Set-Cookie、server session
SEC-015§R-SEC-14§セキュリティ§不正JSON・未知enum・巨大数・重複parameterを安全に400にする§U02§malformed、unknown、Long超過§全代表APIへ送信§500なし、内部情報なし、DB不変§status/body/log
SEC-016§R-RPT-02§セキュリティ§第三者が任意message IDを通報して本文を取得できない§U23§M002§POST report後admin payload観察§作成自体を403/404、snapshot新規0§reports count、status
SEC-017§R-SEC-15§セキュリティ§request IDへ改行・長大値を入れてlog注入できない§匿名§CRLF、Unicode control、10KB§X-Request-Id付きGET§sanitize/再発行し1request1log§response header、ECS JSON lines
SEC-018§R-SEC-16§セキュリティ§password・token・mail本文・画像pathをlogへ残さない§各actor§認証/失敗/画像操作§一連操作後log検索§secret一致0、必要なrequest metadataのみ§application/Filebeat/Kibana log
NFT-001§R-NFR-01§非機能§74投稿の一覧初回と主要API p95を基準内にする§匿名/U02§seed全件§各API30回、cold/warm分離§一覧3秒以内、API p95 1秒以内、error0§trace、duration、DB query count
NFT-002§R-NFR-02§非機能§二重click・Enter連打で重複登録しない§各作成者§遅延response§register/post/DM/report/feedbackを連打§業務上1件、button busy§HTTP count、unique rows
NFT-003§R-NFR-03§非機能§同時投稿で種別ごとの公開1件制限を破らない§U18§同種別2要求、募集と加入希望各1要求§2パターンをbarrierで同時POST§同種別は1成功1競合、異種別は2成功、各種別OPEN 1件§responses、transaction log、user/type/status別DB
NFT-004§R-NFR-03§非機能§同時画像追加で5枚上限を破らない§U14§P009が4枚、2画像§parallel upload§合計5以下、orphan0§responses、DB/files
NFT-005§R-NFR-03§非機能§同時初回DMで会話一意制約エラーを利用者へ露出しない§U07/U08§双方同時send§barrierで送信§会話1、message2、5xx0§responses、DB、log
NFT-006§R-NFR-04§非機能§seed再投入を3回行い同じ固定状態へ戻す§専用DB§seed SQL§各実行後manifest/count/checksum§全回成功、84 users/74 posts、重複0§psql log、集計checksum
NFT-007§R-NFR-05§非機能§2利用者のsession・検索履歴・通知・下書きを分離する§U07/U08§2 browser contexts§同時に異なる操作§相互混入0§cookies、API IDs、DB owner
NFT-008§R-NFR-06§非機能§主要画面を375/390/768/1440幅で操作可能にする§全actor§全25 routes§各viewportで巡回・主要操作§横scrollなし、44px target、固定要素被りなし§full screenshots、geometry
NFT-009§R-NFR-07§非機能§全機能をkeyboardだけで完了する§全actor§全interactive controls§Tab/ShiftTab/Enter/Space/矢印/Escape§順序論理的、focus常時可視、trapなし§録画、activeElement列
NFT-010§R-NFR-08§非機能§名前・役割・状態・errorを支援技術へ伝える§全actor§主要route§accessibility snapshotとscreen reader確認§label/heading/live region/dialog妥当§AX tree、読み上げ記録
NFT-011§R-NFR-09§非機能§本文・control・focusのcontrastをAA基準にする§全route§通常/hover/focus/disabled/error§computed colorを測定§本文4.5:1、大文字等3:1、UI/focus3:1§測定表、screenshots
NFT-012§R-NFR-10§非機能§長文・日本語・絵文字・合成文字でlayoutと保存を壊さない§U04/U07§上限文字、改行、emoji、結合文字§全自由記述へ保存・表示§切断規則一貫、overflowなし、再読込同値§DOM geometry、API/DB length
NFT-013§R-NFR-11§非機能§DBのNOT NULL・FK・UNIQUE・CHECK制約が最終防衛線になる§専用DB直接接続§users email重複、会話pair重複、block pair重複、孤児FK、未知enum、必須null§各違反を個別transactionでINSERT/UPDATEしrollback§全違反をDBが拒否し既存行不変、アプリ経由違反は4xxで500非公開§SQLSTATE、constraint名、行数、API status/log
'@

$layerDefaults = @{
    '単体' = @('対象クラスをmock/固定Clock/一時dirで分離し、外部I/Oを行わない','JUnit XML、失敗時stack trace、検証した引数','mock・一時dirをテスト終了時に破棄','最初にassertion差分、次にservice分岐、最後にmock呼出し順を確認')
    '結合' = @('専用DBへseed済み。QA_RELEASE_IT=true。テスト用upload dir。対象利用者のsessionとCSRFを取得','JUnit XML、MockMvc要求応答、匿名化DB照合、必要時file一覧','transaction rollbackまたはseed再投入。生成画像を専用dirから削除','HTTP層、Security filter、Controller、Service、JPA/DB、filesystemの順に境界を特定')
    'システム' = @('専用DBへseed済み。テスト版アプリ起動済み。公式Playwright MCP接続済み','ケースID入りscreenshot、Playwright MCP操作記録、network status、匿名化DB照合','変更ケースはseed再投入。browser contextと添付画像を破棄','UI表示、browser console/network、API応答、server log、DB/filesの順で切り分け')
    'ユーザー' = @('専用環境と録画同意を準備。参加者へ操作方法を説明しない','観察票、許可済み録画、所要時間、迷い/誤操作、本人の発話','作成データをseedで戻し、録画を管理領域へ移す','UI不具合と文言理解を分け、観察者の誘導有無、端末差、再現STを確認')
    'セキュリティ' = @('専用DB・専用画像dir。各主体の独立session。攻撃文字列はテスト環境だけで使用','要求応答、権限matrix、匿名化DB差分、files/log。secretは保存しない','全session失効、seed再投入、攻撃fixture削除','入口のSecurity、所有者判定、service規則、DB制約、公開resource handler、logの順に確認')
    '非機能' = @('専用環境を固定し、他負荷を止める。時刻・viewport・試行回数を記録','動画/画像、計測生データ、集計方法、DB件数、環境情報','負荷・network interceptionを解除しseed再投入','再現性を3回確認し、client/server/DB/filesystem/環境資源へ分解')
}

$cases = [System.Collections.Generic.List[object]]::new()
foreach ($line in ($rows -split "`r?`n")) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $v = $line.Split('§')
    if ($v.Count -eq 9) {
        $id = $v[0]; $req = $v[1]; $layer = $v[2]
        $priority = if ($layer -eq 'セキュリティ' -or $layer -eq '結合') { 'P0' }
            elseif ($layer -eq 'ユーザー') { 'P1' }
            elseif ($layer -eq '非機能') { if ($id -in @('NFT-008','NFT-009','NFT-010','NFT-011','NFT-012')) { 'P2' } else { 'P1' } }
            elseif ($layer -eq 'システム') { if ($req -match '^R-(AUTH|MSG|SEC|RPT|ADM|IMG)') { 'P0' } else { 'P1' } }
            else { if ($req -match '^R-(AUTH|MSG|SEC|IMG)') { 'P0' } else { 'P1' } }
        $v = @($v[0],$v[1],$v[2],$priority,$v[3],$v[4],$v[5],$v[6],$v[7],$v[8])
    }
    if ($v.Count -ne 10) { throw "Invalid case row ($($v.Count)): $line" }
    $d = $layerDefaults[$v[2]]
    if ($null -eq $d) { throw "Unknown layer: $($v[2])" }
    $cases.Add([pscustomobject]@{
        'テストID'=$v[0]; '要件ID'=$v[1]; 'テスト層'=$v[2]; '優先度'=$v[3]; '目的'=$v[4]
        '前提条件'=$d[0]; '使用ユーザー'=$v[5]; '使用データ'=$v[6]; '操作手順'=$v[7]
        '期待結果'=$v[8]; 'DB/API/UIで確認する内容'=$v[9]; '証跡'=$d[1]
        '後処理'=$d[2]; '失敗時の切り分け方法'=$d[3]
    })
}

$ids = $cases.'テストID'
if (($ids | Select-Object -Unique).Count -ne $cases.Count) { throw 'Duplicate test ID' }
if ($cases.Count -ne 161) { throw "Expected 161 cases, got $($cases.Count)" }

$header = @"
# Band Link リリーステストケース

生成元: ``scripts/test/generate-release-test-catalog.ps1``  
状態: **設計済み・全件未実行**  
総数: $($cases.Count)件

各ケースは単独で結果を記録します。複数のデータ値が書かれたケースは、列挙値をすべて実施して初めてPASSです。期待結果と現実装が違う場合、期待結果を変更せずFAILとして記録します。

"@
$builder = [Text.StringBuilder]::new($header)
[void]$builder.AppendLine()
foreach ($c in $cases) {
    [void]$builder.AppendLine("## $($c.'テストID') — $($c.'目的')")
    foreach ($field in @('要件ID','テスト層','優先度','目的','前提条件','使用ユーザー','使用データ','操作手順','期待結果','DB/API/UIで確認する内容','証跡','後処理','失敗時の切り分け方法')) {
        [void]$builder.AppendLine("- **${field}:** $($c.$field)")
    }
    [void]$builder.AppendLine()
}
[IO.File]::WriteAllText((Join-Path $out 'test-cases.md'), $builder.ToString(), [Text.UTF8Encoding]::new($false))

$sourceByPrefix = @{
 'R-AUTH'='requirements.md 3,7; AuthController/AuthService/SecurityConfig'; 'R-PROF'='requirements.md 4; decision 0010; ProfileController/ProfileService';
 'R-PRES'='requirements.md 4,6; DESIGN.md'; 'R-POST'='requirements.md 5; decisions 0001,0003,0005; PostService';
 'R-SRCH'='requirements.md 6; decision 0002; PostController/PostService'; 'R-MSG'='requirements.md 7; decision 0004; MessageService';
 'R-NOT'='requirements.md 7; NotificationController'; 'R-BLK'='requirements.md 8; BlockService'; 'R-RPT'='requirements.md 8; ReportService';
 'R-ADM'='requirements.md 8; Admin controllers'; 'R-FBK'='decision 0006; FeedbackController/FeedbackService'; 'R-IMG'='requirements.md 4,5,7; ImageStorageService';
 'R-SEC'='requirements.md 3,8,13; SecurityConfig and ownership rules'; 'R-UI'='DESIGN.md and three design references';
 'R-ERR'='requirements.md 12,13; UI error handlers'; 'R-UX'='requirements.md 12; DESIGN.md'; 'R-NFR'='requirements.md 12,13; DESIGN.md'
}
$matrix = foreach ($group in ($cases | Group-Object '要件ID' | Sort-Object Name)) {
    $prefix = ($sourceByPrefix.Keys | Where-Object { $group.Name.StartsWith($_) } | Select-Object -First 1)
    [pscustomobject]@{
        '要件ID'=$group.Name
        '要件概要'=(($group.Group.'目的' | Select-Object -Unique) -join ' / ')
        '根拠'=$sourceByPrefix[$prefix]
        'テストケース'=(($group.Group.'テストID' | Sort-Object) -join ' ')
        '件数'=$group.Count
        '最高優先度'=(($group.Group.'優先度' | Sort-Object) | Select-Object -First 1)
        '設計対応'='COVERED'
        '実行状態'='NOT_RUN'
    }
}
$csvPath = Join-Path $out 'traceability-matrix.csv'
$matrix | Export-Csv -Path $csvPath -NoTypeInformation -Encoding utf8

$stats = $cases | Group-Object 'テスト層' | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Count)" }
$priorities = $cases | Group-Object '優先度' | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Count)" }
Write-Output "cases=$($cases.Count); $($stats -join ', '); $($priorities -join ', '); requirements=$($matrix.Count)"
