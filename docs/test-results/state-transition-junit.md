# Service state transition JUnit

Executed 2026-09-14 with `scripts/test/run-state-transition-junit.ps1`, using the dedicated JUnit Platform Launcher path used for
the Windows classpath workaround.

- Class: `com.example.bandlink.service.ReleasePostStateTransitionUnitTest`
- Tests found: 3
- Tests started: 3
- Successful: 3
- Failed: 0
- Aborted/skipped: 0

Covered transitions are manual close→reopen, owner-only mutation, and rejection
of reopening an administrator-deleted post. Existing block and message service
unit tests remain separate and are included in the normal unit suite.
