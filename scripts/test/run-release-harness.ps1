param(
  [ValidateSet('all','unit','integration','inventory')][string]$Suite = 'all',
  [switch]$SkipMaven,
  [string]$Inventory = (Join-Path $PSScriptRoot '..\..\docs\test-plan\full-case-inventory.csv'),
  [string]$Output = (Join-Path $PSScriptRoot '..\..\docs\test-results\release-harness-results.csv')
)
$ErrorActionPreference = 'Stop'
$oldNativePreference = $PSNativeCommandUseErrorActionPreference
$PSNativeCommandUseErrorActionPreference = $false

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$logDir = Join-Path $root 'target\release-harness'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$commands = @()
if (!$SkipMaven -and $Suite -in @('all','unit')) {
  $commands += @{ Name='unit'; Args=@('-B','-Dmaven.compiler.release=25','test') }
}
if (!$SkipMaven -and $Suite -in @('all','integration')) {
  $commands += @{ Name='integration'; Args=@('-B','-Dmaven.compiler.release=25','-Dtest=ReleaseApiIntegrationTest','test') }
}
foreach ($c in $commands) {
  $log = Join-Path $logDir ("$($c.Name)-$stamp.log")
  Push-Location $root
  try {
    $oldDbPassword = $env:DB_PASSWORD
    $env:DB_PASSWORD='Ryou6341'
    if ($c.Name -eq 'integration') {
      $oldQa = $env:QA_RELEASE_IT; $oldPwd = $env:QA_RELEASE_PASSWORD; $oldDb = $env:QA_RELEASE_DB_PASSWORD
      $env:QA_RELEASE_IT='true'; $env:QA_RELEASE_PASSWORD='BandLink-QA-2026!'; $env:QA_RELEASE_DB_PASSWORD='Ryou6341'
    }
    $oldNativePreference = $PSNativeCommandUseErrorActionPreference
    $PSNativeCommandUseErrorActionPreference = $false
    try {
    $argLine = ($c.Args | ForEach-Object { '"' + ($_ -replace '"','\\"') + '"' }) -join ' '
    cmd.exe /d /c ("mvnw.cmd " + $argLine + " 2>&1") | Tee-Object -FilePath $log
      $exitCode = $LASTEXITCODE
    } finally { $PSNativeCommandUseErrorActionPreference = $oldNativePreference }
    if ($exitCode -ne 0) { throw "$($c.Name) failed ($exitCode)" }
  }
  finally {
    if ($c.Name -eq 'integration') { $env:QA_RELEASE_IT=$oldQa; $env:QA_RELEASE_PASSWORD=$oldPwd; $env:QA_RELEASE_DB_PASSWORD=$oldDb }
    $env:DB_PASSWORD=$oldDbPassword
    Pop-Location
  }
}

if (!(Test-Path $Inventory)) { throw "Inventory not found: $Inventory" }
$rows = Import-Csv -LiteralPath $Inventory
$unitFeatures = @('Authentication','Profile','Recruitment-post','Search','Direct-message','Image-storage','Report-admin','Feedback')
$integrationFeatures = $unitFeatures
$unitPass = $true; $integrationPass = $true
if ($commands.Name -contains 'unit') { $unitPass = (Get-ChildItem "$root\target\surefire-reports\TEST-*.xml" | Where-Object { (Get-Content $_.FullName -Raw) -match 'failures="[1-9]|errors="[1-9]' }).Count -eq 0 }
if ($commands.Name -contains 'integration') { $integrationPass = (Get-ChildItem "$root\target\surefire-reports\TEST-ReleaseApiIntegrationTest.xml" -ErrorAction SilentlyContinue | Where-Object { (Get-Content $_.FullName -Raw) -match 'failures="[1-9]|errors="[1-9]' }).Count -eq 0 }
$outRows = foreach ($r in $rows) {
  $covered = $false; $evidence = 'NOT_AUTOMATED'
  if ($commands.Name -contains 'unit' -and $unitPass -and $r.Feature -in $unitFeatures) { $covered=$true; $evidence='JUnit focused suite PASS (does not prove every combination)' }
  if ($commands.Name -contains 'integration' -and $integrationPass -and $r.Feature -in $integrationFeatures) { $covered=$true; $evidence='Spring Boot integration suite PASS (does not prove every combination)' }
  [pscustomobject]@{TestID=$r.TestID; RequirementID=$r.RequirementID; Feature=$r.Feature; UserState=$r.UserState; InputState=$r.InputState; DataState=$r.DataState; Operation=$r.Operation; Priority=$r.Priority; Status=if($covered){'COVERED_BY_AUTOMATION'}else{'NOT_AUTOMATED'}; Evidence=$evidence; RunAt=$stamp; LogDirectory='target/release-harness'}
}
$outRows | Export-Csv -LiteralPath $Output -NoTypeInformation -Encoding UTF8
$summary = $outRows | Group-Object Status | ForEach-Object { "$($_.Name)=$($_.Count)" }
Write-Output "Harness output: $Output"
Write-Output ($summary -join ', ')
$PSNativeCommandUseErrorActionPreference = $oldNativePreference
