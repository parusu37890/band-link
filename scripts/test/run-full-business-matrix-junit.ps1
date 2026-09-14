[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$jdkCandidates = @(
    $env:BANDLINK_TEST_JAVA_HOME,
    'C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot',
    $env:JAVA_HOME,
    'C:\Program Files\Java\jdk-26.0.2'
) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')) }
$jdkHome = $jdkCandidates | Select-Object -First 1
if (-not $jdkHome) { throw 'No JDK runtime was found. Set BANDLINK_TEST_JAVA_HOME to a JDK 25 or newer.' }
$java = Join-Path $jdkHome 'bin\java.exe'
$javac = Join-Path $jdkHome 'bin\javac.exe'
$jar = Join-Path $jdkHome 'bin\jar.exe'
Push-Location $root
try {
    $previousJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = $jdkHome
    cmd.exe /d /c "mvnw.cmd -q -Dmaven.compiler.release=25 -DskipTests compile"
    if ($LASTEXITCODE -ne 0) { throw "main compile failed: $LASTEXITCODE" }
    cmd.exe /d /c "mvnw.cmd -q dependency:build-classpath -Dmdep.outputFile=target/business-matrix-cp.txt"
    if ($LASTEXITCODE -ne 0) { throw "dependency classpath failed: $LASTEXITCODE" }

    $mainJar = Join-Path $root 'target\business-matrix-main.jar'
    Remove-Item $mainJar -Force -ErrorAction SilentlyContinue
    & $jar cf $mainJar -C (Join-Path $root 'target\classes') .
    if ($LASTEXITCODE -ne 0) { throw "main jar failed: $LASTEXITCODE" }
    $cp = $mainJar + ';' + (Get-Content 'target\business-matrix-cp.txt' -Raw).Trim()
    $out = Join-Path $root 'target\business-matrix-test-classes'
    New-Item -ItemType Directory -Force $out | Out-Null
    $sources = @(Get-ChildItem 'scripts\test\java' -Recurse -Filter '*.java' | ForEach-Object FullName)
    if ($sources.Count -lt 5) { throw "Business matrix sources are incomplete: $($sources.Count)" }
    & $javac --release 25 -cp $cp -d $out @sources
    if ($LASTEXITCODE -ne 0) { throw "business matrix compile failed: $LASTEXITCODE" }

    $runner = Join-Path $root 'target\RunFullBusinessMatrix.java'
    @'
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public class RunFullBusinessMatrix {
  public static void main(String[] args) {
    LauncherDiscoveryRequest r = LauncherDiscoveryRequestBuilder.request()
      .selectors(DiscoverySelectors.selectClass("com.example.bandlink.matrix.FullBusinessMatrixJUnitTest")).build();
    Launcher l = LauncherFactory.create();
    SummaryGeneratingListener s = new SummaryGeneratingListener();
    l.registerTestExecutionListeners(s); l.execute(r);
    s.getSummary().printTo(new java.io.PrintWriter(System.out));
    s.getSummary().getFailures().stream().limit(40).forEach(f -> {
      System.out.println("FAIL " + f.getTestIdentifier().getDisplayName());
      f.getException().printStackTrace(System.out);
    });
    if (!s.getSummary().getFailures().isEmpty()) System.exit(1);
  }
}
'@ | Set-Content -LiteralPath $runner -Encoding ascii
    if ($cp -notmatch 'junit-platform-launcher') { throw 'JUnit Platform Launcher is missing from the Maven test classpath' }
    & $javac --release 25 -cp $cp -d $out $runner
    if ($LASTEXITCODE -ne 0) { throw "JUnit launcher compile failed: $LASTEXITCODE" }
    & $java -cp ($out + ';' + $cp) RunFullBusinessMatrix
    if ($LASTEXITCODE -ne 0) { throw "full business matrix failed: $LASTEXITCODE" }

    $result = 'docs\test-results\api-case-adapters\full-business-matrix-results.csv'
    Copy-Item 'target\full-business-matrix-results.csv' $result -Force
    $rows = @(Import-Csv $result)
    if ($rows.Count -ne 14227) { throw "Expected 14227 result rows, got $($rows.Count)" }
    Write-Output "Full business matrix complete: 14227/14227 PASS; result=$result"
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    Pop-Location
}
