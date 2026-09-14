<#
Creates and maintains the Playwright MCP execution queue for full-case-inventory.csv.
This script does not drive a browser. Browser actions must be performed with the
official @playwright/mcp tools; the result updater records the evidence returned
by that run.
#>
[CmdletBinding()]
param(
  [ValidateSet('prepare','record','report')][string]$Mode = 'prepare',
  [string]$Inventory,
  [string]$OutputDirectory,
  [string]$TestId,
  [ValidateSet('PASS','FAIL','BLOCKED','NOT_RUN')][string]$Result,
  [string]$Evidence = '',
  [string]$Notes = ''
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Inventory)) { $Inventory = Join-Path $PSScriptRoot '..\..\docs\test-plan\full-case-inventory.csv' }
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) { $OutputDirectory = Join-Path $PSScriptRoot '..\..\docs\test-results\playwright-harness' }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$manifestPath = Join-Path $OutputDirectory 'manifest.csv'
$queuePath = Join-Path $OutputDirectory 'queue.jsonl'
$summaryPath = Join-Path $OutputDirectory 'summary.md'

function Get-Class($row) {
  $manualInputs = @('screen-reader','concurrent','latency','offline')
  if ($manualInputs -contains $row.InputState) { return 'MANUAL_BROWSER' }
  if ($row.Feature -eq 'Non-functional' -and $row.Operation -eq 'recover') { return 'MANUAL_BROWSER' }
  if ($row.Layer -notmatch 'system') { return 'API_OR_UNIT' }
  return 'PLAYWRIGHT_MCP'
}
function Save-Manifest($rows) { $rows | Export-Csv -LiteralPath $manifestPath -NoTypeInformation -Encoding UTF8 }

if ($Mode -eq 'prepare') {
  if (-not (Test-Path -LiteralPath $Inventory)) { throw "Inventory not found: $Inventory" }
  $rows = Import-Csv -LiteralPath $Inventory
  $out = foreach ($r in $rows) {
    $class = Get-Class $r
    [pscustomobject]@{
      TestID=$r.TestID; RequirementID=$r.RequirementID; Layer=$r.Layer; Priority=$r.Priority
      Feature=$r.Feature; UserState=$r.UserState; InputState=$r.InputState; DataState=$r.DataState
      Operation=$r.Operation; ExecutionClass=$class; Status='NOT_RUN'; Evidence=''; Notes=''
    }
  }
  Save-Manifest $out
  Remove-Item -LiteralPath $queuePath -Force -ErrorAction SilentlyContinue
  $writer = [System.IO.StreamWriter]::new($queuePath, $false, [System.Text.UTF8Encoding]::new($false))
  try {
    foreach ($r in $out | Where-Object ExecutionClass -eq 'PLAYWRIGHT_MCP') {
      $prompt = "Band Link official Playwright MCP case $($r.TestID). Use test-cases.md and fixed test-users.md. Feature=$($r.Feature); actor=$($r.UserState); input=$($r.InputState); data=$($r.DataState); operation=$($r.Operation). Record URL, viewport, HTTP statuses, console errors, DB/API/UI checks, evidence path, cleanup, and PASS/FAIL/BLOCKED. Do not expose secrets."
      $writer.WriteLine(( [pscustomobject]@{ TestID=$r.TestID; Priority=$r.Priority; Prompt=$prompt } | ConvertTo-Json -Compress ))
    }
  } finally { $writer.Dispose() }
}
elseif ($Mode -eq 'record') {
  if (-not (Test-Path -LiteralPath $manifestPath)) { throw 'Run prepare first.' }
  if ([string]::IsNullOrWhiteSpace($TestId) -or [string]::IsNullOrWhiteSpace($Result)) { throw 'record requires -TestId and -Result.' }
  $rows = Import-Csv -LiteralPath $manifestPath
  $matches = @($rows | Where-Object TestID -eq $TestId)
  if ($matches.Count -ne 1) { throw "Expected one case, found $($matches.Count): $TestId" }
  $matches[0].Status=$Result; $matches[0].Evidence=$Evidence; $matches[0].Notes=$Notes
  Save-Manifest $rows
}
elseif ($Mode -eq 'report') {
  if (-not (Test-Path -LiteralPath $manifestPath)) { throw 'Run prepare first.' }
  $rows = Import-Csv -LiteralPath $manifestPath
  $total=$rows.Count
  $groups = $rows | Group-Object ExecutionClass | Sort-Object Name
  $lines = @("# Playwright MCP harness report",'',"Generated: $(Get-Date -Format o)","Total cases: $total",'')
  foreach ($g in $groups) {
    $byStatus = ($g.Group | Group-Object Status | ForEach-Object { "$($_.Name)=$($_.Count)" }) -join ', '
    $lines += "- $($g.Name): $($g.Count) ($byStatus)"
  }
  $lines += '', 'A PLAYWRIGHT_MCP case is executable only with the official `@playwright/mcp` tools. `MANUAL_BROWSER` requires human or supervised browser checks; `API_OR_UNIT` belongs to JUnit/MockMvc and is intentionally not re-run in a browser.', '', 'Release gate: no P0/P1 case may remain NOT_RUN or BLOCKED; every PASS/FAIL must link evidence.'
  Set-Content -LiteralPath $summaryPath -Value $lines -Encoding UTF8
  Get-Content -LiteralPath $summaryPath
}
Write-Output "Harness $Mode complete: $OutputDirectory"
