# Playwright MCP 全ケース実行ハーネス

`full-case-inventory.csv` は単体・結合・システムの組み合わせを含む台帳です。Playwrightはブラウザのシステムテストだけを担当するため、台帳の各行に実行区分を付け、実行結果を同じIDで戻せるようにします。ハーネス自体はブラウザを操作せず、公式 `@playwright/mcp` の操作結果を記録します。

## 使い方

PowerShellで、まずキューを作成します。

```powershell
pwsh -File scripts/test/prepare-playwright-harness.ps1 -Mode prepare
```

出力先は `docs/test-results/playwright-harness/` です。

- `manifest.csv`: 全ケースの実行区分と状態
- `queue.jsonl`: `PLAYWRIGHT_MCP` ケースごとのRuna向け指示
- `summary.md`: 区分・状態別集計

Runaは `queue.jsonl` の1行を取り出し、公式Playwright MCPだけで実行します。ケースID、`test-cases.md` の期待結果、固定ユーザー、DB/API/UI突合、証跡を必ず記録します。1ケースごとに独立contextを使い、破壊的変更後は固定seedへ戻します。

結果を記録します。

```powershell
pwsh -File scripts/test/prepare-playwright-harness.ps1 -Mode record `
  -TestId FULL-00001 -Result PASS `
  -Evidence 'docs/test-results/playwright-harness/FULL-00001-result.md' `
  -Notes 'URL/status/API/UI/DB checked'
```

`PASS`、`FAIL`、`BLOCKED`、`NOT_RUN` は区別します。Playwright MCPが接続できない場合は代替操作でPASSにせず、`BLOCKED` とします。

```powershell
pwsh -File scripts/test/prepare-playwright-harness.ps1 -Mode report
```

## 実行区分

| 区分 | 実行担当 | 意味 |
|---|---|---|
| `PLAYWRIGHT_MCP` | Runa + 公式Playwright MCP | 通常の画面・操作・通信確認 |
| `MANUAL_BROWSER` | Runaの手動確認 | スクリーンリーダー、同時操作、実ネットワーク障害など |
| `API_OR_UNIT` | JUnit/MockMvc | ブラウザで再実行しない単体・結合確認 |

生成される全14,659行は `NOT_RUN` から始まります。自動化済み代表ケースの結果を全台帳のPASSと読み替えません。P0/P1が未実行またはBLOCKEDのままならリリース判定はNO-GOです。
