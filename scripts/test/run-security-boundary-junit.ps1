[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$jdkCandidates = @(
    $env:BANDLINK_TEST_JAVA_HOME,
    $env:JAVA_HOME,
    'C:\Program Files\Java\jdk-26.0.2',
    'C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')) }
$jdkHome = $jdkCandidates | Select-Object -First 1
if (-not $jdkHome) { throw 'No JDK runtime was found. Set BANDLINK_TEST_JAVA_HOME.' }
$java = Join-Path $jdkHome 'bin\java.exe'
$javac = Join-Path $jdkHome 'bin\javac.exe'
$jarTool = Join-Path $jdkHome 'bin\jar.exe'

Push-Location $root
try {
    $previousJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = $jdkHome
    cmd.exe /d /c "mvnw.cmd -q -Dmaven.compiler.release=25 -DskipTests compile"
    if ($LASTEXITCODE -ne 0) { throw "main compile failed: $LASTEXITCODE" }
    cmd.exe /d /c "mvnw.cmd -q dependency:build-classpath -Dmdep.outputFile=target/security-boundary-cp.txt"
    if ($LASTEXITCODE -ne 0) { throw "dependency classpath failed: $LASTEXITCODE" }

    $mainJar = Join-Path $root 'target\security-boundary-main.jar'
    Remove-Item -LiteralPath $mainJar -Force -ErrorAction SilentlyContinue
    & $jarTool cf $mainJar -C (Join-Path $root 'target\classes') .
    if ($LASTEXITCODE -ne 0) { throw "main jar failed: $LASTEXITCODE" }
    $cp = $mainJar + ';' + (Get-Content 'target\security-boundary-cp.txt' -Raw).Trim()
    if ($cp -notmatch 'junit-platform-launcher') { throw 'JUnit Platform Launcher is missing' }

    $out = Join-Path $root 'target\security-boundary-test-classes'
    New-Item -ItemType Directory -Force $out | Out-Null
    $test = Join-Path $root 'src\test\java\com\example\bandlink\config\SecurityBoundaryFilterTest.java'
    & $javac --release 25 -cp $cp -d $out $test
    if ($LASTEXITCODE -ne 0) { throw "security boundary test compile failed: $LASTEXITCODE" }

    $runner = Join-Path $root 'target\RunSecurityBoundaryJUnit.java'
    @'
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public class RunSecurityBoundaryJUnit {
  public static void main(String[] args) {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
      .selectors(DiscoverySelectors.selectClass("com.example.bandlink.config.SecurityBoundaryFilterTest")).build();
    Launcher launcher = LauncherFactory.create();
    SummaryGeneratingListener summary = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(summary);
    launcher.execute(request);
    summary.getSummary().printTo(new java.io.PrintWriter(System.out));
    summary.getSummary().getFailures().forEach(failure -> {
      System.out.println("FAIL " + failure.getTestIdentifier().getDisplayName());
      failure.getException().printStackTrace(System.out);
    });
    if (!summary.getSummary().getFailures().isEmpty()) System.exit(1);
  }
}
'@ | Set-Content -LiteralPath $runner -Encoding ascii
    & $javac --release 25 -cp $cp -d $out $runner
    if ($LASTEXITCODE -ne 0) { throw "JUnit runner compile failed: $LASTEXITCODE" }
    & $java -cp ($out + ';' + $cp) RunSecurityBoundaryJUnit
    if ($LASTEXITCODE -ne 0) { throw "security boundary JUnit failed: $LASTEXITCODE" }
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    Pop-Location
}
