# テストコード対応表

状態: **追加済み・単体98/98、PostgreSQL結合19/19、業務マトリクス14,227/14,227 PASS**。テスト名の `codeUtNNN` は `UT-NNN` に対応します。

## 単体テスト

|ケース|コード|
|---|---|
|UT-001..UT-006|`src/test/java/com/example/bandlink/service/ReleaseFeedbackUnitTest.java`|
|UT-007..UT-016|`src/test/java/com/example/bandlink/service/ReleaseMessageUnitTest.java`|
|UT-017..UT-022|`src/test/java/com/example/bandlink/service/ReleaseImageUnitTest.java`|
|UT-023..UT-025|`src/test/java/com/example/bandlink/service/ReleaseBlockUnitTest.java`|
|UT-026..UT-031|`src/test/java/com/example/bandlink/dto/ReleaseValidationUnitTest.java`|
|UT-032..UT-033|`src/test/java/com/example/bandlink/service/PostServiceTest.java` と `src/test/java/com/example/bandlink/entity/PostTest.java`|
|UT-034|`src/test/java/com/example/bandlink/dto/ActivitySignalTest.java`|
|UT-035|`src/test/java/com/example/bandlink/dto/PublicContractTest.java`|

UT-022、UT-029、UT-031は受け入れ側の制約をassertしており、静的確認では現実装との差が疑われます。FAILしてもassertを削除・緩和しません。

## 結合・セキュリティテスト

`src/test/java/com/example/bandlink/integration/ReleaseApiIntegrationTest.java` は、専用DBと `QA_RELEASE_IT=true` がある時だけ有効になります。通常の開発DBで誤実行しないためのclass-level guardです。

直接自動化済み:

- IT-001、IT-002、IT-004、IT-008、IT-009、IT-010
- IT-013、IT-020、IT-024、IT-027、IT-028、IT-030
- SEC-010

IT-003、005..007、011..012、015..016、018..019、021..023、025..026、029は、外部メール、時刻、同時要求、filesystemを含むため、`test-cases.md` の手順を専用アプリ/API/DBで実行します。これらも結合テスト件数に含み、未実行のままPASSにはしません。

IT-014とIT-017には旧仕様の自動テストが残っています。募集・加入希望を各1件許可するケースと、キーワードを除いた選択式検索へ更新して再実行するまで、現行仕様の自動化済みとは扱いません。

## 現在の実装差を検出するassertion

次は既知結果を固定したものではなく、リリース要件を固定するテストです。

- 切断画像を受理しない。
- 地域は最大3、パート最大5、ジャンル最大3、スタンス最大1。
- 登録時に4種類のプロフィール関連が保存される。
- 未知master IDを部分保存しない。
- 問い合わせ済み利用者も退会でき、feedbackが残らない。
- 検索条件にkeywordを含めず、選択式条件だけを扱う。
- 巨大cursorで500にしない。
- 第三者は任意messageを通報してsnapshotを作れない。
- DM画像を `/uploads/**` から迂回取得できない。

## 実行結果の記録

Surefire XML、console log、DB照合を `docs/test-results/<日時>/` に保存します。ここに成功記録は書きません。2026-09-15に通常単体・結合・全業務マトリクスを実行済みです。公式Playwright MCPを要するシステムテストと、外部メール・実機を要する手動ケースは未実行です。`r`n