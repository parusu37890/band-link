<# Unified per-case harness for full-case-inventory.csv.
Every inventory row receives an executor, a queue entry, and an independent status.
This script never infers PASS from a feature-level suite; an executor must record it.
#>
[CmdletBinding()]
param(
 [ValidateSet('prepare','record','report')][string]$Mode='prepare',
 [string]$Inventory='',
 [string]$OutputDirectory='',
 [string]$TestId, [ValidateSet('PASS','FAIL','BLOCKED','NOT_RUN','NOT_AUTOMATED')][string]$Result,
 [string]$Evidence='', [string]$Notes=''
)
$ErrorActionPreference='Stop'
if([string]::IsNullOrWhiteSpace($Inventory)){$Inventory=Join-Path $PSScriptRoot '..\..\docs\test-plan\full-case-inventory.csv'}
if([string]::IsNullOrWhiteSpace($OutputDirectory)){$OutputDirectory=Join-Path $PSScriptRoot '..\..\docs\test-results\full-case-harness'}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$manifest=Join-Path $OutputDirectory 'manifest.csv'; $queue=Join-Path $OutputDirectory 'queue.jsonl'; $summary=Join-Path $OutputDirectory 'summary.md'
function Classify($r) {
 if($r.Feature -eq 'Non-functional' -and $r.InputState -in @('screen-reader','concurrent','latency','offline')) { return 'MANUAL_BROWSER' }
 if($r.Feature -in @('Authentication','Profile','Recruitment-post','Search','Direct-message','Image-storage','Report-admin','Feedback','Security')) { return 'API_OR_UNIT' }
 return 'PLAYWRIGHT_MCP'
}
function Executor($class) { switch($class) { 'API_OR_UNIT' {'JUnit/MockMvc adapter'} 'PLAYWRIGHT_MCP' {'official @playwright/mcp'} default {'supervised manual browser'} } }
if($Mode -eq 'prepare') {
 if(!(Test-Path $Inventory)){throw "Inventory not found: $Inventory"}; $rows=Import-Csv $Inventory
 $out=@(); foreach($r in $rows){$class=Classify $r; $out += [pscustomobject]@{TestID=$r.TestID;RequirementID=$r.RequirementID;Feature=$r.Feature;Layer=$r.Layer;Priority=$r.Priority;UserState=$r.UserState;InputState=$r.InputState;DataState=$r.DataState;Operation=$r.Operation;ExecutionClass=$class;Executor=(Executor $class);Status='NOT_RUN';Evidence='';Notes='';RunAt=''}}
 $out|Export-Csv $manifest -NoTypeInformation -Encoding UTF8
 $sw=[IO.StreamWriter]::new($queue,$false,[Text.UTF8Encoding]::new($false)); try { foreach($r in $out){$prompt="Case $($r.TestID): feature=$($r.Feature), actor=$($r.UserState), input=$($r.InputState), data=$($r.DataState), operation=$($r.Operation). Use fixed seed. Verify DB/API/UI and attach evidence. Record only PASS/FAIL/BLOCKED; never infer from another case."; $sw.WriteLine(([pscustomobject]@{TestID=$r.TestID;ExecutionClass=$r.ExecutionClass;Executor=$r.Executor;Priority=$r.Priority;Prompt=$prompt}|ConvertTo-Json -Compress))} } finally {$sw.Dispose()}
}
elseif($Mode -eq 'record') {
 if(!(Test-Path $manifest)){throw 'Run -Mode prepare first.'}; if([string]::IsNullOrWhiteSpace($TestId)-or [string]::IsNullOrWhiteSpace($Result)){throw 'record requires -TestId and -Result.'}; $rows=Import-Csv $manifest; $m=@($rows|Where-Object TestID -eq $TestId); if($m.Count -ne 1){throw "Expected one case, found $($m.Count): $TestId"}; $m[0].Status=$Result;$m[0].Evidence=$Evidence;$m[0].Notes=$Notes;$m[0].RunAt=(Get-Date -Format o);$rows|Export-Csv $manifest -NoTypeInformation -Encoding UTF8
}
else {
 if(!(Test-Path $manifest)){throw 'Run -Mode prepare first.'}; $rows=Import-Csv $manifest; $lines=@('# Full case harness report','',"Generated: $(Get-Date -Format o)","Total: $($rows.Count)",''); foreach($g in ($rows|Group-Object ExecutionClass|Sort-Object Name)){ $s=($g.Group|Group-Object Status|ForEach-Object{"$($_.Name)=$($_.Count)"})-join ', ';$lines+="- $($g.Name): $($g.Count) ($s)" };$lines+='','A row is PASS only after its assigned executor records evidence. NOT_RUN, BLOCKED, and NOT_AUTOMATED are release blockers for P0/P1.';Set-Content $summary $lines -Encoding UTF8;Get-Content $summary
}
Write-Output "Full case harness $Mode complete: $OutputDirectory"
