[CmdletBinding()]
param(
    [string]$Inventory = '',
    [string]$ResultsDirectory = '',
    [switch]$SkipCompile
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$Inventory = if ([string]::IsNullOrWhiteSpace($Inventory)) { Join-Path $repo 'docs\test-plan\full-case-inventory.csv' } else { $Inventory }
$ResultsDirectory = if ([string]::IsNullOrWhiteSpace($ResultsDirectory)) { Join-Path $repo 'docs\test-results\full-case-harness' } else { $ResultsDirectory }
$inventoryPath = (Resolve-Path $Inventory).Path
New-Item -ItemType Directory -Force -Path $ResultsDirectory | Out-Null
$compileLog = Join-Path $repo 'target\release-case-junit-compile.log'
$runLog = Join-Path $repo 'target\release-case-junit.log'

Push-Location $repo
try {
    # Do not invoke clean on Windows: a running dev server or antivirus can
    # retain a handle under target. Disabling Maven's incremental decision
    # still recompiles changed test sources without deleting target.
    if (-not $SkipCompile) {
        & .\mvnw.cmd -B '-Dmaven.compiler.release=25' '-Dmaven.compiler.useIncrementalCompilation=false' -DskipTests test-compile *> $compileLog
        if ($LASTEXITCODE -ne 0) { throw "JUnit matrix compilation failed. See $compileLog" }
    }
    & .\mvnw.cmd -B '-Dmaven.compiler.release=25' '-Dmaven.compiler.useIncrementalCompilation=false' "-Drelease.inventory=$inventoryPath" '-Dtest=com.example.bandlink.matrix.ReleaseCaseMatrixJUnitTest' test *> $runLog
    $exitCode = $LASTEXITCODE
    $source = Join-Path $repo 'target\release-case-matrix-results.csv'
    if (Test-Path $source) {
        Copy-Item -LiteralPath $source -Destination (Join-Path $ResultsDirectory 'junit-matrix-results.csv') -Force
    }
    Copy-Item -LiteralPath $runLog -Destination (Join-Path $ResultsDirectory 'junit-matrix.log') -Force
    $summary = Join-Path $ResultsDirectory 'junit-matrix-summary.md'
    $rows = if (Test-Path $source) { Import-Csv $source } else { @() }
    $lines = @('# JUnit release case matrix', '', "Inventory: $inventoryPath", "Generated: $(Get-Date -Format o)", "Cases executed: $($rows.Count)", '')
    foreach ($group in ($rows | Group-Object Classification)) { $lines += "- $($group.Name): $($group.Count)" }
    $lines += '', 'Each inventory row is a separate JUnit dynamic test. UNIT_CONTRACT rows validate the executable unit contract; INTEGRATION_REQUIRED rows validate the case schema and explicitly require the corresponding HTTP/DB adapter.'
    Set-Content -LiteralPath $summary -Value $lines -Encoding UTF8
    if ($exitCode -ne 0) { throw "JUnit matrix failed. See $runLog" }
    Write-Output "JUnit case matrix complete: $($rows.Count) cases"
}
finally { Pop-Location }
