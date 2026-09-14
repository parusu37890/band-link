[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Push-Location $root
try {
    # JDK 26 on Windows intermittently cannot resolve target/classes while
    # compiling tests.  A disposable main jar keeps this run deterministic.
    cmd.exe /d /c "mvnw.cmd -q -Dmaven.compiler.release=25 -DskipTests compile"
    if ($LASTEXITCODE -ne 0) { throw "main compile failed: $LASTEXITCODE" }
    cmd.exe /d /c "mvnw.cmd -q dependency:build-classpath -Dmdep.outputFile=target/state-cp.txt"
    if ($LASTEXITCODE -ne 0) { throw "dependency classpath failed: $LASTEXITCODE" }

    $jar = Join-Path $root 'target\state-main.jar'
    Remove-Item $jar -Force -ErrorAction SilentlyContinue
    & 'C:\Program Files\Java\jdk-26.0.2\bin\jar.exe' cf $jar -C (Join-Path $root 'target\classes') .
    $cp = $jar + ';' + (Get-Content 'target\state-cp.txt' -Raw).Trim()
    $out = 'target\state-test-classes'
    New-Item -ItemType Directory -Force $out | Out-Null
    $test = Join-Path $root 'scripts\test\java\com\example\bandlink\service\ReleasePostStateTransitionUnitTest.java'
    & 'C:\Program Files\Java\jdk-26.0.2\bin\javac.exe' --release 25 -cp $cp -d $out $test
    if ($LASTEXITCODE -ne 0) { throw "state test compile failed: $LASTEXITCODE" }

    $runner = Join-Path $root 'target\RunStateJUnit.java'
    @'
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public class RunStateJUnit {
  public static void main(String[] args) {
    LauncherDiscoveryRequest r = LauncherDiscoveryRequestBuilder.request()
      .selectors(DiscoverySelectors.selectClass("com.example.bandlink.service.ReleasePostStateTransitionUnitTest")).build();
    Launcher l = LauncherFactory.create();
    SummaryGeneratingListener s = new SummaryGeneratingListener();
    l.registerTestExecutionListeners(s); l.execute(r);
    s.getSummary().printTo(new java.io.PrintWriter(System.out));
    if (!s.getSummary().getFailures().isEmpty()) System.exit(1);
  }
}
'@ | Set-Content -LiteralPath $runner -Encoding ascii
    $launcher = (Get-ChildItem 'C:\Users\CodexSandboxOffline\.m2\repository\org\junit\platform\junit-platform-launcher\6.0.3' -Filter '*.jar').FullName
    $cp = $cp + ';' + $launcher
    & 'C:\Program Files\Java\jdk-26.0.2\bin\javac.exe' --release 25 -cp $cp -d $out $runner
    if ($LASTEXITCODE -ne 0) { throw "JUnit runner compile failed: $LASTEXITCODE" }
    & 'C:\Program Files\Java\jdk-26.0.2\bin\java.exe' -cp ($out + ';' + $cp) RunStateJUnit
    if ($LASTEXITCODE -ne 0) { throw "state transition JUnit failed: $LASTEXITCODE" }
}
finally { Pop-Location }

