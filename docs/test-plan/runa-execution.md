# Runa実行手順

状態: **実行手順完成**。ケース別業務JUnit 14,227件は2026-09-14に全件成功。実行モデルはLunaを使用します。

## 0. 絶対条件

- 本番コードを変更せず、FAILを期待値変更で消しません。
- DB名は `band_link_release_test` だけです。`band_link` へ接続しません。
- STは公式 `@playwright/mcp` だけを使用します。ツールがなければSTはBLOCKEDです。
- `mvn test`、SQL、アプリ起動、ブラウザ操作の結果を、この文書の過去記録から推測しません。
- password、DB password、Cookie、CSRF、確認token、再設定token、LINE資格情報を証跡へ書きません。

## 1. Runaへ最初に渡す指示

```text
あなたはBand Linkのリリーステスト実行担当Runaです。モデルはLunaを使用します。
最初に AGENTS.md、requirements.md、DESIGN.md、docs/development-workflow.md、
docs/engineering-method.md と docs/test-plan 配下を全文読んでください。

設計済み期待値を現実装に合わせて変更しないでください。本番コードは変更しません。
専用DB band_link_release_test と専用画像directoryだけを使います。
JUnit→結合→公式Playwright MCP→セキュリティ→非機能→ユーザーテストの順に実行し、
各ケースをPASS/FAIL/BLOCKEDで記録してください。テスト未実行をPASSにしないでください。
STは公式 @playwright/mcp だけを使い、別ツールをPlaywright MCPと呼ばないでください。
secret/token/Cookieを成果物へ残さず、FAILは最小再現と層別切り分けを添えてください。
```

## 2. 実行版の固定

作業開始時に次を `docs/test-results/<日時>/00-environment.txt` へ保存します。

- `git rev-parse HEAD`
- `git status --short`
- Java、Maven Wrapper、PostgreSQL、browser、Playwright MCP serverのversion
- Windows version、timezone、実行開始時刻
- `band_link_release_test` へ接続していることが分かるDB名（passwordは除く）
- 専用画像directoryの絶対path

実行中にHEADまたはdirty差分が変わった場合は中断し、新しい実行として最初から始めます。

## 3. 専用DBの準備

DBが存在しない場合だけ、PostgreSQL管理者が `band_link_release_test` を作成します。既存DBを削除して作り直す操作は行いません。以下の環境変数はPowerShellで**1行ずつ**設定します。

```powershell
Set-Location 'C:\Users\parus\Desktop\band'
$env:DB_PASSWORD = '<PostgreSQL password>'
$env:PGPASSWORD = $env:DB_PASSWORD
$env:PGCLIENTENCODING = 'UTF8'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/band_link_release_test'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
```

初回だけテスト版アプリを `ddl-auto=update` のまま専用DBへ起動し、全tableとmaster作成を確認して停止します。起動ログでDB名を確認してから続けます。

## 4. 固定password hashとseed

共通テストpassword例は `BandLink-QA-2026!` です。外部環境で使い回しません。hash生成用classをプロジェクトの依存classpathで一時compileします。

```powershell
$env:QA_RELEASE_PASSWORD = 'BandLink-QA-ST-2026-PWBE!'
& .\mvnw.cmd -q dependency:build-classpath '-Dmdep.outputFile=target\qa-release-classpath.txt'
$qaClasspath = Get-Content -Raw 'target\qa-release-classpath.txt'
New-Item -ItemType Directory -Force 'target\qa-fixture-tool' | Out-Null
& javac -cp $qaClasspath -d 'target\qa-fixture-tool' 'scripts\test\ReleasePasswordHash.java'
$qaPasswordHash = & java -cp "target\qa-fixture-tool;$qaClasspath" ReleasePasswordHash
```

新しい空directoryを画像専用に作ります。既存の `uploads` は指定しません。

```powershell
$qaUploadDir = Join-Path (Resolve-Path '.').Path ('target\qa-release-uploads-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
& 'scripts\test\create-release-images.ps1' -Destination $qaUploadDir
$env:BANDLINK_UPLOAD_DIR = $qaUploadDir
```

seedを投入します。2つの環境変数代入を同じ行へ続けて書きません。

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -h localhost -U postgres -d band_link_release_test -v "qa_password_hash=$qaPasswordHash" -f 'scripts/test/seed-release-users.sql'
```

終了時に `COMMIT`、`users_expected_84=84`、`posts_expected_74=74` を確認します。ERRORが1つでもあれば続行しません。

## 5. 実行順

### Gate 1: fixture静的確認と単体テスト

最初に `scripts/test/seed-release-users.sql` のDB名guard、固定ID重複、画像参照を確認します。続いてJUnit単体テストを実行します。新規release testだけを先に実行し、その後に全JUnitを実行します。

対象コード:

- `ReleaseFeedbackUnitTest`
- `ReleaseMessageUnitTest`
- `ReleaseImageUnitTest`
- `ReleaseBlockUnitTest`
- `ReleaseValidationUnitTest`
- 既存のservice/entity/dto/config tests

全組み合わせの業務JUnitも実行する。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test/run-full-business-matrix-junit.ps1
```

成功条件は14,227件検出、14,227件成功、失敗・中断・スキップ0件、および `docs/test-results/api-case-adapters/full-business-matrix-results.csv` が14,227データ行であること。結果詳細は `docs/test-results/full-business-matrix-junit.md` を参照する。

1件でも失敗したらGate 1はFAILです。ただし全件を最後まで実行して失敗一覧を取ります。`UT-022` など現在の実装差が疑われるassertionも弱めません。

### Gate 2: PostgreSQL結合テスト

```powershell
$env:QA_RELEASE_IT = 'true'
$env:QA_RELEASE_JDBC_URL = $env:SPRING_DATASOURCE_URL
$env:QA_RELEASE_DB_USERNAME = 'postgres'
$env:QA_RELEASE_DB_PASSWORD = $env:DB_PASSWORD
```

まず `ReleaseApiIntegrationTest`、次に既存を含む全結合テストを実行します。class-level guardにより `QA_RELEASE_IT=true` がない通常実行では専用結合testはskipされます。skipをPASSとして数えません。

結合ケース `IT-001..IT-030` のうちコードで直接自動化されたものは `code-tests.md` を参照します。残りも `test-cases.md` の手順でMockMvc/APIとDBを照合し、結果を同じ台帳へ記録します。

### Gate 3: テスト版アプリ

Gate 1/2の修正を行う場合は担当へ戻し、再seed後に再実行します。合格するまでSTへ進みません。アプリは専用DB・専用画像directoryで起動します。`GET /api/auth/me` が401、`GET /api/posts/page` が200、起動logのDBが専用DBであることをsmoke確認します。

### Gate 4: 公式Playwright MCP

`playwright-mcp-spec.md` のPW-AからPW-Iの順です。各バッチ開始前にseedを再投入します。状態変更ケースはケースごとに再seedします。P0/P1はChromiumで全件、画面互換の代表導線は利用可能なFirefox/WebKitでも実行します。

### Gate 5: セキュリティ・非機能

SEC-001..018、NFT-001..012を実行します。IDORと画像秘匿は主体ごとに独立contextを使います。同時操作は開始barrierを記録し、単なる素早い連続操作を同時実行と呼びません。性能は30回の生値とp95算出方法を保存します。

### Gate 6: ユーザーテスト

UAT-001..010を最低5名で行います。うち2名以上はスマートフォン中心の利用者、1名以上はキーボード中心、音楽活動経験の有無を混在させます。同じ参加者へ学習効果の強いcaseを連続させません。操作方法を教えず、詰まった時刻と発話を記録します。

## 6. 結果ファイル

実行ごとに次を作ります。

```text
docs/test-results/<yyyyMMdd-HHmmss>/
  00-environment.txt
  01-seed.txt
  junit/
  integration/
  playwright/
  security/
  non-functional/
  user-test/
  release-result.md
```

`release-result.md` には全161件の結果、未実行理由、FAIL一覧、再試験結果、P0/P1残件、リリース可否を記録します。集計の分母からBLOCKEDやNOT_RUNを外しません。

## 7. 失敗時の戻り先

|症状|最初の確認|戻り先|
|---|---|---|
|JUnit assertion|期待値、固定Clock、mock副作用|service/validation担当|
|Spring起動失敗|最初のcause、DB名、Bean constructor|構成・結合担当|
|401/403差|主体、mail確認、status、CSRF|Security→Controller|
|500|request IDでserver log、transaction rollback|Controller→Service→DB|
|表示だけ違う|API payload、console、computed style|UI担当|
|DBだけ違う|transaction境界、FK/unique/check、同時要求|Service→JPA/DB|
|画像残骸|保存順、rollback、物理path|ImageStorage/呼出service|
|MCP操作不能|公式MCP接続とbrowser起動|環境BLOCKED。別ツール代替不可|

## 8. 終了判定

`release-test-plan.md` のリリース判定基準を適用します。2026-09-15時点でJUnit・結合・業務マトリクスはPASSですが、公式Playwright最新UI回帰、ユーザーテスト、NFT、同時投稿のDB一意性が未完了です。P0/P1のFAILまたは未解消BLOCKEDが1件でも残る間は「リリース不可」です。`r`n
