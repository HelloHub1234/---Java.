param([switch]$NoIcon)

# Local build of Clicker-windows.zip: the same pipeline CI runs (.github/workflows/build.yml),
# including the .exe icon. Run from anywhere:  powershell -File tools/build-clicker.ps1
# Note: this file is ASCII on purpose - Windows PowerShell 5.1 reads BOM-less scripts in the
# ANSI codepage, so any non-ASCII character here breaks the parser on cyrillic locales.
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

$icon = 'assets/clicker.ico'
$jpackageArgs = @(
    '--type', 'app-image',
    '--name', 'Clicker',
    '--input', 'classes',
    '--main-jar', 'clicker.jar',
    '--main-class', 'WindowApps.clicker.Clicker',
    '--dest', 'dist/Clicker',
    '--java-options', '-Dfile.encoding=UTF-8'
)

if ($NoIcon) {
    Write-Host '== Icon disabled (-NoIcon)' -ForegroundColor Yellow
} elseif (-not (Test-Path $icon)) {
    throw "Missing $icon - generate it with tools/generate-icon.ps1"
} else {
    $jpackageArgs += @('--icon', $icon)
}

Remove-Item -Recurse -Force classes, dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path classes, dist | Out-Null

Write-Host '== Compile' -ForegroundColor Cyan
javac -encoding UTF-8 -d classes (Get-ChildItem src/WindowApps/*.java) (Get-ChildItem src/WindowApps/clicker/*.java)
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }
Copy-Item src/WindowApps/messages*.properties classes/WindowApps/
# Icon inside the jar as well: this is what tools that read the archive (and the window itself,
# if it ever looks it up) can pick up. Explorer still draws its own per-association icon for .jar.
if (Test-Path $icon) {
    Copy-Item $icon classes/WindowApps/clicker.ico
}

Write-Host '== Jar' -ForegroundColor Cyan
jar --create --file dist/clicker.jar --main-class WindowApps.clicker.Clicker -C classes WindowApps
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }

Write-Host '== jpackage' -ForegroundColor Cyan
# jpackage looks for --main-jar inside --input (classes), so the jar has to sit there too
Copy-Item dist/clicker.jar classes/clicker.jar
jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed' }

Compress-Archive -Path dist/Clicker -DestinationPath dist/Clicker-windows.zip -Force

Write-Host '== Done: dist/Clicker-windows.zip and dist/clicker.jar' -ForegroundColor Green
