# Full case harness report

Generated: 2026-09-14T02:38:50.9951463+09:00
Total: 14659

- API_OR_UNIT: 14227 (NOT_RUN=14227)
- MANUAL_BROWSER: 192 (NOT_RUN=192)
- PLAYWRIGHT_MCP: 240 (NOT_RUN=240)

A row is PASS only after its assigned executor records evidence. NOT_RUN, BLOCKED, and NOT_AUTOMATED are release blockers for P0/P1.

The API_OR_UNIT subset has since been executed by `ReleaseCaseMatrixJUnitTest`
as 14,227 independent JUnit DynamicTests. The result is recorded separately in
`docs/test-results/api-case-adapters/junit-matrix-results.csv`; 1,750 rows are UNIT_CONTRACT and
12,477 rows remain explicitly classified INTEGRATION_REQUIRED for business
side-effect verification.
