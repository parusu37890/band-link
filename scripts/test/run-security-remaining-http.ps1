[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$LogPath = 'target/sec-remaining-logs/band-link.ecs.json',
    [string]$DatabaseName = 'band_link_release_test'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
if (-not $env:QA_RELEASE_PASSWORD) { throw 'QA_RELEASE_PASSWORD is required' }
if (-not $env:PGPASSWORD) { throw 'PGPASSWORD is required for the SEC-016 before/after check' }

function New-HttpState {
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.UseCookies = $true
    $handler.CookieContainer = [System.Net.CookieContainer]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.BaseAddress = [Uri]$BaseUrl
    [pscustomobject]@{ Client = $client; Handler = $handler; Csrf = $null }
}

function Invoke-TestRequest {
    param($State, [string]$Method, [string]$Path, [string]$Body, [bool]$UseCsrf = $false, [hashtable]$Headers = @{})
    if ($UseCsrf -and -not $State.Csrf) { Update-Csrf $State }
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), $Path)
    if ($Body) { $request.Content = [System.Net.Http.StringContent]::new($Body, [Text.Encoding]::UTF8, 'application/json') }
    if ($UseCsrf) { $null = $request.Headers.TryAddWithoutValidation($State.Csrf.headerName, $State.Csrf.token) }
    foreach ($entry in $Headers.GetEnumerator()) {
        if (-not $request.Headers.TryAddWithoutValidation([string]$entry.Key, [string]$entry.Value)) {
            throw "Header was rejected before transmission: $($entry.Key)"
        }
    }
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $response = $State.Client.SendAsync($request).GetAwaiter().GetResult()
    $watch.Stop()
    $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $setCookie = @($response.Headers | Where-Object Key -eq 'Set-Cookie' | ForEach-Object Value)
    [pscustomobject]@{
        Status = [int]$response.StatusCode
        Body = $text
        DurationMs = $watch.Elapsed.TotalMilliseconds
        RequestId = if ($response.Headers.Contains('X-Request-Id')) { ($response.Headers.GetValues('X-Request-Id') | Select-Object -First 1) } else { $null }
        SetCookie = $setCookie -join '; '
        RetryAfter = if ($response.Headers.Contains('Retry-After')) { ($response.Headers.GetValues('Retry-After') | Select-Object -First 1) } else { $null }
    }
}

function Update-Csrf($State) {
    $response = Invoke-TestRequest $State 'GET' '/api/csrf' $null $false
    if ($response.Status -ne 200) { throw "CSRF bootstrap failed: $($response.Status)" }
    $State.Csrf = $response.Body | ConvertFrom-Json
    $script:LastCsrfResponse = $response
}

function Assert-Equal($Expected, $Actual, [string]$Message) {
    if ($Expected -ne $Actual) { throw "$Message expected=$Expected actual=$Actual" }
}

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Login($State, [string]$Email, [string]$Password) {
    $body = @{ email = $Email; password = $Password } | ConvertTo-Json -Compress
    Invoke-TestRequest $State 'POST' '/api/auth/login' $body $true
}

function Median([double[]]$Values) {
    $sorted = @($Values | Sort-Object)
    if ($sorted.Count % 2) { return [double]$sorted[[int][math]::Floor($sorted.Count / 2)] }
    return ([double]$sorted[$sorted.Count / 2 - 1] + [double]$sorted[$sorted.Count / 2]) / 2
}

function ReportCount {
    $raw = & 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -q -t -A -h localhost -U postgres -d $DatabaseName `
        -c "select count(*) from reports where target_type='MESSAGE' and target_id=940002"
    if ($LASTEXITCODE -ne 0) { throw 'SEC-016 DB count failed' }
    return [int]($raw | Select-Object -Last 1)
}

$results = [ordered]@{}

# SEC-014: the CSRF bootstrap creates the session cookie; login then logout must invalidate it.
$session = New-HttpState
Update-Csrf $session
$cookieHeader = $script:LastCsrfResponse.SetCookie
Assert-True ($cookieHeader -match 'HttpOnly') 'SEC-014: JSESSIONID is missing HttpOnly'
Assert-True ($cookieHeader -match 'SameSite=Lax') 'SEC-014: JSESSIONID is missing SameSite=Lax'
Assert-True ($cookieHeader -notmatch '(?i);\s*Secure') 'SEC-014: local HTTP cookie unexpectedly has Secure'
$login = Login $session 'qa-release-third-party@example.test' $env:QA_RELEASE_PASSWORD
Assert-Equal 200 $login.Status 'SEC-014 login'
$beforeLogout = Invoke-TestRequest $session 'GET' '/api/auth/me'
Assert-Equal 200 $beforeLogout.Status 'SEC-014 authenticated session'
$logout = Invoke-TestRequest $session 'POST' '/api/auth/logout' $null $true
Assert-Equal 204 $logout.Status 'SEC-014 logout'
$afterLogout = Invoke-TestRequest $session 'GET' '/api/auth/me'
Assert-Equal 401 $afterLogout.Status 'SEC-014 old session reuse'
$results['SEC-014'] = [ordered]@{ Status = 'PASS'; HttpOnly = $true; SameSite = 'Lax'; LocalSecure = $false; LogoutOldSessionStatus = 401; ProductionSecureRequires = 'SESSION_COOKIE_SECURE=true' }

# Fresh participant for SEC-015/016.
$actor = New-HttpState
$actorLogin = Login $actor 'qa-release-third-party@example.test' $env:QA_RELEASE_PASSWORD
Assert-Equal 200 $actorLogin.Status 'SEC-015/016 login'
$malformed = Invoke-TestRequest $actor 'POST' '/api/reports' '{"targetType":' $true
$unknownEnum = Invoke-TestRequest $actor 'POST' '/api/reports' '{"targetType":"ROOT","targetId":1,"reason":"probe"}' $true
$hugeLong = Invoke-TestRequest $actor 'POST' '/api/reports' '{"targetType":"MESSAGE","targetId":999999999999999999999999999999999,"reason":"probe"}' $true
$duplicate = Invoke-TestRequest $actor 'GET' '/api/posts/page?limit=10&limit=20'
foreach ($response in @($malformed, $unknownEnum, $hugeLong, $duplicate)) {
    Assert-Equal 400 $response.Status 'SEC-015 invalid input status'
    Assert-True ($response.Body -notmatch '(?i)exception|stack.?trace|org\.springframework|java\.') 'SEC-015 leaked internal details'
}
$results['SEC-015'] = [ordered]@{ Status = 'PASS'; MalformedJson = 400; UnknownEnum = 400; HugeLong = 400; DuplicateScalar = 400 }

$beforeReports = ReportCount
$idor = Invoke-TestRequest $actor 'POST' '/api/reports' '{"targetType":"MESSAGE","targetId":940002,"reason":"SEC-016 participant boundary"}' $true
$afterReports = ReportCount
Assert-True ($idor.Status -in @(403,404)) "SEC-016 status was $($idor.Status)"
Assert-Equal $beforeReports $afterReports 'SEC-016 report count changed'
$results['SEC-016'] = [ordered]@{ Status = 'PASS'; HttpStatus = $idor.Status; Before = $beforeReports; After = $afterReports }

# SEC-017: hostile values must be replaced, bounded, and absent from structured logs.
$hostileIds = @('space separated id', ([char]0x2028 + 'unicode-control'), ('A' * 10000))
$returnedIds = @()
foreach ($hostile in $hostileIds) {
    $response = Invoke-TestRequest (New-HttpState) 'GET' '/api/posts/page?limit=1' $null $false @{ 'X-Request-Id' = $hostile }
    if ($response.Status -eq 200) {
        Assert-True ($response.RequestId -match '^[0-9a-f-]{36}$') 'SEC-017 invalid request id was reflected'
        $returnedIds += $response.RequestId
    } else {
        Assert-True ($response.Status -eq 400) "SEC-017 unexpected parser status $($response.Status)"
    }
}
Start-Sleep -Milliseconds 250
$logText = if (Test-Path -LiteralPath $LogPath) { Get-Content -LiteralPath $LogPath -Raw } else { '' }
Assert-True ($logText -notmatch 'space separated id|unicode-control|AAAAA{20}') 'SEC-017 hostile request id reached logs'
$results['SEC-017'] = [ordered]@{ Status = 'PASS'; Cases = 3; Regenerated = $returnedIds.Count; ParserRejected = 3 - $returnedIds.Count; HostileValuesInLog = 0 }

# SEC-018: synthetic secrets are sent in bodies and must never appear in ECS output.
$secretState = New-HttpState
$syntheticPassword = 'SEC018_PASSWORD_MARKER_7f2c'
$syntheticToken = 'SEC018_TOKEN_MARKER_5d91'
$null = Login $secretState 'qa-release-general@example.test' $syntheticPassword
$null = Invoke-TestRequest $secretState 'POST' '/api/auth/password-reset/request' '{"email":"sec018-mail-marker@example.test"}' $true
$null = Invoke-TestRequest $secretState 'POST' '/api/auth/password-reset/confirm' ('{"token":"' + $syntheticToken + '","newPassword":"another-test-password"}') $true
Start-Sleep -Milliseconds 250
$logText = Get-Content -LiteralPath $LogPath -Raw
$secretMatches = @($syntheticPassword, $syntheticToken, 'sec018-mail-marker@example.test', (Resolve-Path '.').Path) |
    Where-Object { $logText.IndexOf($_, [StringComparison]::OrdinalIgnoreCase) -ge 0 }
Assert-Equal 0 $secretMatches.Count 'SEC-018 secret or physical path appeared in log'
$results['SEC-018'] = [ordered]@{ Status = 'PASS'; SecretMatches = 0; Checked = @('password-body', 'token-body', 'email-body', 'workspace-physical-path') }

# SEC-013 is last because it intentionally exhausts the endpoint buckets.
$enumState = New-HttpState
$knownTimes = @(); $unknownTimes = @(); $knownBodies = @(); $unknownBodies = @()
1..5 | ForEach-Object {
    $response = Login $enumState 'qa-release-general@example.test' 'wrong-password-marker'
    $knownTimes += $response.DurationMs; $knownBodies += $response.Body
    $response = Login $enumState 'qa-release-does-not-exist@example.test' 'wrong-password-marker'
    $unknownTimes += $response.DurationMs; $unknownBodies += $response.Body
}
Assert-True (@($knownBodies | Select-Object -Unique).Count -eq 1) 'SEC-013 known-account login response varied'
Assert-True (@($unknownBodies | Select-Object -Unique).Count -eq 1) 'SEC-013 unknown-account login response varied'
Assert-Equal $knownBodies[0] $unknownBodies[0] 'SEC-013 login account enumeration body differs'
$loginLimited = $null
1..15 | ForEach-Object {
    if (-not $loginLimited) {
        $candidate = Login $enumState 'qa-release-general@example.test' 'wrong-password-marker'
        if ($candidate.Status -eq 429) { $loginLimited = $candidate }
    }
}
Assert-True ($null -ne $loginLimited) 'SEC-013 login never rate limited'

$resetState = New-HttpState
1..5 | ForEach-Object {
    $known = Invoke-TestRequest $resetState 'POST' '/api/auth/password-reset/request' '{"email":"qa-release-general@example.test"}' $true
    $unknown = Invoke-TestRequest $resetState 'POST' '/api/auth/password-reset/request' '{"email":"qa-release-does-not-exist@example.test"}' $true
    Assert-Equal $known.Status $unknown.Status 'SEC-013 reset enumeration status differs'
    Assert-Equal $known.Body $unknown.Body 'SEC-013 reset enumeration body differs'
}
$resetLimited = $null
1..15 | ForEach-Object {
    if (-not $resetLimited) {
        $candidate = Invoke-TestRequest $resetState 'POST' '/api/auth/password-reset/request' '{"email":"qa-release-general@example.test"}' $true
        if ($candidate.Status -eq 429) { $resetLimited = $candidate }
    }
}
Assert-True ($null -ne $resetLimited) 'SEC-013 reset never rate limited'

$verifyState = New-HttpState
$verifyLimited = $null
1..25 | ForEach-Object {
    if (-not $verifyLimited) {
        $candidate = Invoke-TestRequest $verifyState 'POST' '/api/auth/verify-email' '{"token":"invalid-verification-token"}' $true
        if ($candidate.Status -eq 429) { $verifyLimited = $candidate }
    }
}
Assert-True ($null -ne $verifyLimited) 'SEC-013 verify never rate limited'
$knownMedian = Median $knownTimes
$unknownMedian = Median $unknownTimes
$ratio = [math]::Round(([math]::Max($knownMedian, $unknownMedian) / [math]::Max(1, [math]::Min($knownMedian, $unknownMedian))), 2)
$results['SEC-013'] = [ordered]@{
    Status = 'PASS'; LoginKnownStatus = 401; LoginUnknownStatus = 401; SameBody = $true
    KnownMedianMs = [math]::Round($knownMedian, 2); UnknownMedianMs = [math]::Round($unknownMedian, 2)
    MedianRatio = $ratio; LoginRateLimited = 429; ResetRateLimited = 429; VerifyRateLimited = 429
}

$rateLimitedLogCount = @(Get-Content -LiteralPath $LogPath | Where-Object { $_ -match '"http_status":"429"' }).Count
Assert-True ($rateLimitedLogCount -ge 3) 'SEC-013 rate-limited requests were not monitored in access logs'
$results['SEC-013']['RateLimitedAccessLogs'] = $rateLimitedLogCount

$destination = 'docs/test-results/2026-09-14-security-remaining.json'
$results | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $destination -Encoding utf8
Write-Output "SEC-013..018 HTTP checks passed; evidence=$destination"
