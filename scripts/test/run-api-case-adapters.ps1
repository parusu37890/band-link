[CmdletBinding()]
param(
 [ValidateSet('prepare','run','record','report')][string]$Mode='prepare',
 [string]$Inventory='',
 [string]$OutputDirectory='',
 [string]$TestId,[ValidateSet('PASS','FAIL','BLOCKED','NOT_RUN','ADAPTER_MISSING')][string]$Result,[string]$Evidence='',[string]$Notes=''
)
$ErrorActionPreference='Stop';if([string]::IsNullOrWhiteSpace($Inventory)){$Inventory=Join-Path $PSScriptRoot '..\..\docs\test-plan\full-case-inventory.csv'};if([string]::IsNullOrWhiteSpace($OutputDirectory)){$OutputDirectory=Join-Path $PSScriptRoot '..\..\docs\test-results\api-case-adapters'};New-Item -ItemType Directory -Force -Path $OutputDirectory|Out-Null
$manifest=Join-Path $OutputDirectory 'manifest.csv';$logDir=Join-Path $OutputDirectory 'logs';New-Item -ItemType Directory -Force -Path $logDir|Out-Null
$registry=@{
 'Authentication'=@{Class='com.example.bandlink.controller.AuthSessionTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.controller.AuthSessionTest test'}
 'Profile'=@{Class='com.example.bandlink.dto.ReleaseValidationUnitTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.dto.ReleaseValidationUnitTest test'}
 'Recruitment-post'=@{Class='com.example.bandlink.service.PostServiceTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.service.PostServiceTest test'}
 'Search'=@{Class='com.example.bandlink.integration.ReleaseApiIntegrationTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.integration.ReleaseApiIntegrationTest test'}
 'Direct-message'=@{Class='com.example.bandlink.service.ReleaseMessageUnitTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.service.ReleaseMessageUnitTest test'}
 'Image-storage'=@{Class='com.example.bandlink.service.ReleaseImageUnitTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.service.ReleaseImageUnitTest test'}
 'Report-admin'=@{Class='com.example.bandlink.integration.ReleaseApiIntegrationTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.integration.ReleaseApiIntegrationTest test'}
 'Feedback'=@{Class='com.example.bandlink.service.ReleaseFeedbackUnitTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.service.ReleaseFeedbackUnitTest test'}
 'Security'=@{Class='com.example.bandlink.config.EmailVerificationGateFilterTest';Command='mvnw.cmd -B -Dmaven.compiler.release=25 -Dtest=com.example.bandlink.config.EmailVerificationGateFilterTest test'}
}
if($Mode -eq 'prepare'){
 $rows=Import-Csv $Inventory|Where-Object {$_.Feature -ne 'Non-functional'}
 $out=foreach($r in $rows){$a=$registry[$r.Feature];[pscustomobject]@{TestID=$r.TestID;RequirementID=$r.RequirementID;Feature=$r.Feature;Priority=$r.Priority;UserState=$r.UserState;InputState=$r.InputState;DataState=$r.DataState;Operation=$r.Operation;AdapterClass=if($a){$a.Class}else{''};Command=if($a){$a.Command}else{''};Status='NOT_RUN';Evidence='';Notes='';RunAt=''}}
 $out|Export-Csv $manifest -NoTypeInformation -Encoding UTF8
}
elseif($Mode -eq 'run'){
 if(!(Test-Path $manifest)){throw 'Run prepare first.'};$rows=Import-Csv $manifest;$groups=$rows|Group-Object AdapterClass
 foreach($g in $groups){if([string]::IsNullOrWhiteSpace($g.Name)){foreach($r in $g.Group){$r.Status='ADAPTER_MISSING';$r.Notes='No adapter registered for feature'};continue};$safe=($g.Name -replace '[^A-Za-z0-9.]','_');$log=Join-Path $logDir "$safe.log";Push-Location (Resolve-Path (Join-Path $PSScriptRoot '..\..'));try{$env:DB_PASSWORD='Ryou6341';$env:QA_RELEASE_IT='true';$env:QA_RELEASE_DB_PASSWORD='Ryou6341';$env:QA_RELEASE_PASSWORD='BandLink-QA-ST-2026-PWBE!';cmd.exe /d /c ($g.Group[0].Command+' 2>&1')|Tee-Object $log;if($LASTEXITCODE -eq 0){foreach($r in $g.Group){$r.Status='ADAPTER_EXECUTED';$r.Evidence=$log;$r.Notes='Adapter suite passed; per-case business assertions still require parameterized adapter';$r.RunAt=(Get-Date -Format o)}}else{foreach($r in $g.Group){$r.Status='FAIL';$r.Evidence=$log;$r.Notes='Adapter suite failed';$r.RunAt=(Get-Date -Format o)}}}finally{Pop-Location}}
 $rows|Export-Csv $manifest -NoTypeInformation -Encoding UTF8
}
elseif($Mode -eq 'record'){
 if(!(Test-Path $manifest)){throw 'Run prepare first.'};$rows=Import-Csv $manifest;$m=@($rows|Where-Object TestID -eq $TestId);if($m.Count-ne 1){throw 'TestId not found or duplicated'};$m[0].Status=$Result;$m[0].Evidence=$Evidence;$m[0].Notes=$Notes;$m[0].RunAt=(Get-Date -Format o);$rows|Export-Csv $manifest -NoTypeInformation -Encoding UTF8
}
else{$rows=Import-Csv $manifest;$lines=@('# API case adapter report','',"Generated: $(Get-Date -Format o)","Total: $($rows.Count)",'');foreach($g in $rows|Group-Object Status){$lines+="- $($g.Name): $($g.Count)"};$lines+='','ADAPTER_EXECUTED is suite-level evidence and is not a per-case PASS. Only explicit record PASS satisfies a case-level release gate.';Set-Content (Join-Path $OutputDirectory 'summary.md') $lines -Encoding UTF8;Get-Content (Join-Path $OutputDirectory 'summary.md')}
Write-Output "API adapter $Mode complete: $OutputDirectory"
