# API case adapter report

Generated: 2026-09-14T02:45:26.0108704+09:00
Total: 14227

- ADAPTER_EXECUTED: 14227

ADAPTER_EXECUTED is suite-level evidence and is not a per-case PASS. Only explicit record PASS satisfies a case-level release gate.

## Individual JUnit matrix run

`ReleaseCaseMatrixJUnitTest` subsequently executed all 14,227 API/unit/integration
rows as independent JUnit DynamicTests: PASS 14,227, FAIL 0, ERROR 0, SKIP 0.
The result file is `docs/test-results/api-case-adapters/junit-matrix-results.csv`. Classification is
UNIT_CONTRACT 1,750 and INTEGRATION_REQUIRED 12,477. The latter means that the
row contract ran, while a live HTTP/DB business assertion remains required; it
is not inferred feature-suite PASS.
