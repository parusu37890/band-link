param(
    [Parameter(Mandatory = $true)]
    [string]$Destination
)

$ErrorActionPreference = 'Stop'
$target = [System.IO.Path]::GetFullPath($Destination)
if ([string]::IsNullOrWhiteSpace($target) -or $target.Length -lt 8) {
    throw '専用の画像出力先を指定してください。'
}
if (Test-Path -LiteralPath $target) {
    if ((Get-ChildItem -LiteralPath $target -Force | Measure-Object).Count -ne 0) {
        throw "既存ファイルを守るため、空ではない出力先を拒否しました: $target"
    }
} else {
    New-Item -ItemType Directory -Path $target | Out-Null
}

$png = [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=')
$jpeg = [Convert]::FromBase64String('/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAEf/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABBQJ//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPwF//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAgEBPwF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQAGPwJ//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPyF//9oADAMBAAIAAwAAABD/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/EF//xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/EF//xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/EF//2Q==')
$webp = [Convert]::FromBase64String('UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEAAUAmJaQAA3AA/v89WAAAAA==')

for ($index = 1; $index -le 9; $index++) {
    $name = '97000000-0000-4000-8000-{0}.png' -f $index.ToString('000000000000')
    [IO.File]::WriteAllBytes((Join-Path $target $name), $png)
}
$privateTarget = Join-Path $target 'messages'
New-Item -ItemType Directory -Path $privateTarget | Out-Null
[IO.File]::WriteAllBytes((Join-Path $privateTarget '97000000-0000-4000-8000-000000000007.png'), $png)
$feedbackTarget = Join-Path $target 'feedback'
New-Item -ItemType Directory -Path $feedbackTarget | Out-Null
[IO.File]::WriteAllBytes((Join-Path $feedbackTarget '97000000-0000-4000-8000-000000000009.png'), $png)
[IO.File]::WriteAllBytes((Join-Path $target 'qa-valid.png'), $png)
[IO.File]::WriteAllBytes((Join-Path $target 'qa-valid.jpg'), $jpeg)
[IO.File]::WriteAllBytes((Join-Path $target 'qa-valid.webp'), $webp)
[IO.File]::WriteAllBytes((Join-Path $target 'qa-empty.png'), [byte[]]::new(0))
[IO.File]::WriteAllText((Join-Path $target 'qa-script.svg'), '<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>')
[IO.File]::WriteAllBytes((Join-Path $target 'qa-png-named-jpg.jpg'), $png)
[IO.File]::WriteAllBytes((Join-Path $target 'qa-truncated.png'), [byte[]](137,80,78,71,13,10,26,10))

$atLimit = [byte[]]::new(5 * 1024 * 1024)
[Array]::Copy($png, $atLimit, $png.Length)
[IO.File]::WriteAllBytes((Join-Path $target 'qa-exactly-5mib.png'), $atLimit)
$overLimit = [byte[]]::new(5 * 1024 * 1024 + 1)
[Array]::Copy($png, $overLimit, $png.Length)
[IO.File]::WriteAllBytes((Join-Path $target 'qa-over-5mib.png'), $overLimit)

$manifest = @(
    'fixture_version=1'
    'generated_utc=' + [DateTime]::UtcNow.ToString('O')
    'destination=' + $target
    'normal=png,jpg,webp,exactly-5mib'
    'invalid=empty,svg,mime-mismatch,truncated,over-5mib'
)
[IO.File]::WriteAllLines((Join-Path $target 'QA_RELEASE_MANIFEST.txt'), $manifest)
Write-Output "QA release image fixtures created in $target"
