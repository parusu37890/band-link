# Release test harness

`scripts/test/run-release-harness.ps1` runs the full JUnit Maven suite and the Spring Boot release integration suite, captures Maven output under `target/release-harness`, and writes a per-case status projection to `release-harness-results.csv`.

The harness deliberately does not mark a generated combination as passed merely because a related feature suite passed. `COVERED_BY_AUTOMATION` means the feature has a passing focused suite; it does not mean every row's user/input/data/operation combination was executed. Rows without a corresponding executable suite remain `NOT_AUTOMATED`.

## Usage

```powershell
.\scripts\test\run-release-harness.ps1 -Suite all
```

Use `-Suite unit`, `-Suite integration`, or `-Suite inventory -SkipMaven` for a projection-only run. The output is regenerated each run and is suitable for Runa to consume. Playwright system tests remain a separate MCP execution because browser interaction cannot be performed by this PowerShell process.

The integration command sets `QA_RELEASE_IT=true`, `QA_RELEASE_PASSWORD=BandLink-QA-2026!`, and `QA_RELEASE_DB_PASSWORD` for the process only. Maven skipped tests remain skipped in Surefire and are never counted as passes by this harness.
