$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$distRoot = Join-Path $root "dist\windows"
$buildRoot = Join-Path $root "dist\build"

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found in PATH."
    }
}

function Resolve-JPackage {
    $cmd = Get-Command "jpackage" -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    $javaCmd = Get-Command "java" -ErrorAction SilentlyContinue
    if (-not $javaCmd) {
        throw "java command was not found in PATH."
    }

    $javaDir = Split-Path -Parent $javaCmd.Source
    $candidate = Join-Path $javaDir "jpackage.exe"
    if (Test-Path $candidate) {
        return $candidate
    }

    if ($env:JAVA_HOME) {
        $javaHomeCandidate = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
        if (Test-Path $javaHomeCandidate) {
            return $javaHomeCandidate
        }
    }

    $jdkRoots = @(
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Eclipse Adoptium"
    )

    foreach ($rootDir in $jdkRoots) {
        if (-not (Test-Path $rootDir)) { continue }
        $found = Get-ChildItem -Path $rootDir -Recurse -Filter "jpackage.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            return $found.FullName
        }
    }

    throw "jpackage was not found. Install a full JDK (21+) and set JAVA_HOME."
}

function Resolve-PythonRunner {
    $pyCmd = Get-Command "py" -ErrorAction SilentlyContinue
    if ($pyCmd) {
        return @($pyCmd.Source, "-3")
    }

    $pythonCmd = Get-Command "python" -ErrorAction SilentlyContinue
    if ($pythonCmd) {
        return @($pythonCmd.Source)
    }

    throw "Neither 'py' nor 'python' command is available."
}

function Invoke-PythonModule {
    param(
        [string[]]$Runner,
        [string[]]$Args
    )

    if ($Runner.Length -gt 1) {
        & $Runner[0] $Runner[1..($Runner.Length - 1)] @Args
    } else {
        & $Runner[0] @Args
    }
}

Write-Host "Checking required tools..."
Require-Command "java"
Require-Command "npm"
$jpackageExe = Resolve-JPackage
$pythonRunner = Resolve-PythonRunner

Write-Host "Cleaning output folders..."
if (Test-Path $distRoot) { Remove-Item -Recurse -Force $distRoot }
if (Test-Path $buildRoot) { Remove-Item -Recurse -Force $buildRoot }
New-Item -ItemType Directory -Path $distRoot | Out-Null
New-Item -ItemType Directory -Path $buildRoot | Out-Null

Write-Host "Building UI..."
Push-Location (Join-Path $root "anticheat-ui")
npm run build
Pop-Location

Write-Host "Copying UI build into Spring static resources..."
Copy-Item `
    -Path (Join-Path $root "anticheat-ui\dist\*") `
    -Destination (Join-Path $root "engine\src\main\resources\static") `
    -Recurse -Force

Write-Host "Building engine jar..."
Push-Location (Join-Path $root "engine")
.\mvnw -DskipTests clean package
Pop-Location

$engineJar = Join-Path $root "engine\target\engine-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $engineJar)) {
    throw "Engine jar not found at $engineJar"
}

Write-Host "Packaging Master Control executable (app-image)..."
& $jpackageExe `
  --type app-image `
  --name "AEGISMasterControl" `
  --input (Join-Path $root "engine\target") `
  --main-jar "engine-0.0.1-SNAPSHOT.jar" `
  --dest $distRoot `
  --vendor "AEGIS AI" `
  --description "AEGIS AI Master Control" `
  --win-console

Write-Host "Installing/updating PyInstaller..."
Invoke-PythonModule -Runner $pythonRunner -Args @("-m", "pip", "install", "--upgrade", "pyinstaller")
Invoke-PythonModule -Runner $pythonRunner -Args @("-m", "pip", "install", "-r", (Join-Path $root "anticheat-agent-python\requirements.txt"))

$pyDist = Join-Path $distRoot "agent"
$pyWork = Join-Path $buildRoot "pyinstaller\work"
$pySpec = Join-Path $buildRoot "pyinstaller\spec"
New-Item -ItemType Directory -Path $pyDist -Force | Out-Null
New-Item -ItemType Directory -Path $pyWork -Force | Out-Null
New-Item -ItemType Directory -Path $pySpec -Force | Out-Null

Write-Host "Packaging AntiCheat Agent executable..."
Invoke-PythonModule -Runner $pythonRunner -Args @(
    "-m", "PyInstaller",
    "--noconfirm",
    "--onefile",
    "--noconsole",
    "--name", "AEGISAntiCheatAgent",
    "--distpath", $pyDist,
    "--workpath", $pyWork,
    "--specpath", $pySpec,
    (Join-Path $root "anticheat-agent-python\tray_launcher.py")
)

$runBoth = @"
@echo off
setlocal
cd /d %~dp0
start "" ".\AEGISMasterControl\AEGISMasterControl.exe"
timeout /t 3 > nul
start "" ".\agent\AEGISAntiCheatAgent.exe"
echo Started Master Control + AntiCheat Agent.
"@

$runAgent = @"
@echo off
setlocal
cd /d %~dp0
start "" ".\agent\AEGISAntiCheatAgent.exe"
echo AntiCheat Agent started in background.
"@

$stopAgent = @"
@echo off
taskkill /IM AEGISAntiCheatAgent.exe /F > nul 2>&1
echo AntiCheat Agent stopped.
"@

Set-Content -Path (Join-Path $distRoot "run-all.bat") -Value $runBoth -Encoding ASCII
Set-Content -Path (Join-Path $distRoot "run-agent-only.bat") -Value $runAgent -Encoding ASCII
Set-Content -Path (Join-Path $distRoot "stop-agent.bat") -Value $stopAgent -Encoding ASCII

Write-Host ""
Write-Host "Packaging complete."
Write-Host "Output: $distRoot"
Write-Host "Master Control EXE: $distRoot\AEGISMasterControl\AEGISMasterControl.exe"
Write-Host "Agent EXE:          $distRoot\agent\AEGISAntiCheatAgent.exe"
