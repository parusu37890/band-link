# JUnit unit and PostgreSQL integration verification

実行日時: 2026-09-15 01:05 JST  
基準コミット: `e104b78f939ffd8ba9adcafa467fb61c7eeec298`（未コミット変更を含む現行作業ツリー）

## 最終結果

| 区分 | 検出 | 実行 | 成功 | 失敗 | スキップ |
|---|---:|---:|---:|---:|---:|
| 通常単体・Springコンテキスト | 117 | 98 | 98 | 0 | 19 |
| `ReleaseApiIntegrationTest`（PostgreSQL） | 19 | 19 | 19 | 0 | 0 |

単体側の19件は、同じテスト出力ディレクトリに含まれるリリース結合テストを通常単体ランチャーから除外した件数である。結合テストは次の行で全19件を別途実行した。

募集投稿・ダイレクトメッセージの状態マトリクス6,552件も全件成功した。全業務マトリクスを統合した結果は `docs/test-results/full-business-matrix-junit.md`、ケース別結果は `docs/test-results/api-case-adapters/full-business-matrix-results.csv` に記録している。

## 実行条件

- Java: Oracle JDK 26.0.2
- PostgreSQL: Windowsサービス `postgresql-x64-18`、PostgreSQL 18.4
- 結合試験DB: `band_link_release_test`
- ガード用マーカー: `qa_release_fixture = Band Link disposable release QA`
- PostgreSQL readiness: `accepting connections`
- fixture再投入後: users 84件、posts 74件

DB認証情報は環境変数 `DB_PASSWORD`、`QA_RELEASE_DATABASE_URL`、`QA_RELEASE_DATABASE_USERNAME`、`QA_RELEASE_DATABASE_PASSWORD`、`QA_RELEASE_PASSWORD` から与えた。

## 実行コマンド

通常のMaven経路を最初に確認した。

```powershell
cmd.exe /d /c "set DB_PASSWORD=<local-db-password>&& mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=*Test,!ReleaseApiIntegrationTest,!ReleaseCaseMatrixJUnitTest test"
```

JDK 26のWindowsファイルシステム問題により `testCompile` が既存の `target/classes` を正しく解決できなかったため、Mavenで依存classpathを作成し、main classをJAR化してJUnit Platform Launcherから実行した。

```powershell
.\mvnw.cmd -B dependency:build-classpath -Dmdep.outputFile=target/current-junit-cp.txt
jar --create --file target/current-junit-main.jar -C target/classes .
# src/test/java 配下25ファイルを target/current-unit-test-classes へjavacでコンパイル
java -cp "target/current-unit-test-classes;target/current-junit-main.jar;<dependency-classpath>" RunCurrentUnitJUnit
java -cp "target/current-unit-test-classes;target/current-junit-main.jar;<dependency-classpath>" RunCurrentIntegrationJUnit
```

fixtureは対象DB名とマーカーを検査してから再投入した。

```powershell
psql -d band_link_release_test -v ON_ERROR_STOP=1 -f scripts/test/seed-release-users.sql
```

## 失敗の切り分けと修正

1. Maven `testCompile` は、JDK 26上でディレクトリ形式の `target/classes` にある本番クラスを多数「存在しない」と報告した。main compile済みclassをJAR化すると同一テストソースをコンパイル・実行できたため、テストまたは本番ソースのコンパイルエラーではなくWindows/JDK経路の問題と判定した。
2. 最初の単体ランチャー実行は `DB_PASSWORD` 未設定によりSpringコンテキスト9件がDB認証で失敗した。環境変数を同じJavaプロセスへ設定すると解消した。
3. `PostServiceTest.editLockMessageNamesWhenEditingBecomesAvailableAgain` は、処理が編集ロックで早期終了するにもかかわらず投稿取得をstubしており、Mockitoの `UnnecessaryStubbingException` になった。未使用stubだけを削除した。
4. 最初の結合試験はfixtureの認証hashと検索期待値の古さにより2件失敗した。ガード済みの使い捨てDBをseedし直してログインを復旧した。現行要件ではkeyword検索が廃止され、legacy `area_sub` も検索対象外なので、IT-017を空結果の互換性確認へ更新した。
5. 並行作業中に更新された `SecurityConfig.class` よりmain JARが古くなり、SEC-011が200を返す偽失敗が1件発生した。現行 `target/classes` からJARを再生成後、期待する403/401/403/200となり全19件成功した。現行作業ツリーへ反映された認可・検索契約の変更を含む状態で再検証した。
6. JDK 26ではjavac終了時に入力JARを閉じる際の `AccessDeniedException` が表示されることがある。javac終了コード0、class生成、JUnit集計の三点を確認して判定した。

## 証跡

- 本記録: `docs/test-results/2026-09-15-junit-postgresql.md`
- 全14,227業務ケース集計: `docs/test-results/full-business-matrix-junit.md`
- 全14,227ケース別結果: `docs/test-results/api-case-adapters/full-business-matrix-results.csv`
- 実行時classpath: `target/current-junit-cp.txt`
- 実行時main JAR: `target/current-junit-main.jar`
- 実行時test class: `target/current-unit-test-classes`



最新の全スイート集計（全業務マトリクス、通常JUnit、結合、認可境界）は [2026-09-15-current-rerun.md](2026-09-15-current-rerun.md) を参照。

