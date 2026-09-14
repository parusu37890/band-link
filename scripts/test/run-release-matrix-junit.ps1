<#
Runs the API/unit/integration release matrix as individual JUnit dynamic tests.
The matrix inventory contains 14,227 executable rows (Non-functional rows are
owned by the browser/manual queues).  Each row is a separate JUnit test case;
the test itself validates the row contract and records the adapter class.  A
row classified as INTEGRATION_REQUIRED is evidence that an HTTP/DB executor is
still required; it is not silently converted into a business PASS.
#>
[CmdletBinding()]
param(
    [string]$Inventory = 'docs/test-plan/full-case-inventory.csv',
    [string]$Result = 'docs/test-results/api-case-adapters/junit-matrix-results.csv'
)
$ErrorActionPreference = 'Stop'
$env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'Ryou6341' }
$env:RELEASE_INVENTORY = (Resolve-Path $Inventory).Path

& .\mvnw.cmd `
    '-DmatrixOnly=true' `
    '-Dtest=com.example.bandlink.matrix.ReleaseCaseMatrixJUnitTest' `
    '-DfailIfNoTests=false' `
    'test'
if ($LASTEXITCODE -ne 0) { throw "Release matrix JUnit failed with exit code $LASTEXITCODE" }

if (!(Test-Path 'target/release-case-matrix-results.csv')) { throw 'Expected matrix result file was not produced by JUnit' }
New-Item -ItemType Directory -Force (Split-Path $Result) | Out-Null
Copy-Item 'target/release-case-matrix-results.csv' $Result -Force
$report = 'docs/test-results/api-case-adapters/junit-matrix-surefire.xml'
Copy-Item 'target/surefire-reports/TEST-com.example.bandlink.matrix.ReleaseCaseMatrixJUnitTest.xml' $report -Force
$rows = @(Import-Csv $Result)
$pass = @($rows | Where-Object Status -eq 'PASS').Count
$fail = @($rows | Where-Object Status -eq 'FAIL').Count
Write-Output "Release matrix JUnit complete: total=$($rows.Count) pass=$pass fail=$fail result=$Result"
if ($fail -gt 0 -or $rows.Count -ne 14227) { exit 2 }
