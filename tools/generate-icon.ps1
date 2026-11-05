# Regenerates assets/clicker.ico from the icon drawing code (src/WindowApps/AppIconPainter.java).
# Needed only after AppIconPainter changes - the .ico itself is committed.
# Run:  powershell -File tools/generate-icon.ps1
# Note: this file is ASCII on purpose - Windows PowerShell 5.1 reads BOM-less scripts in the
# ANSI codepage, so any non-ASCII character here breaks the parser on cyrillic locales.
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

javac -encoding UTF-8 -d .icon-build src/WindowApps/Theme.java src/WindowApps/AppIconPainter.java src/tools/IconExport.java
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

java "-Djava.awt.headless=true" -cp .icon-build tools.IconExport assets/clicker.ico
if ($LASTEXITCODE -ne 0) { throw 'icon export failed' }

Remove-Item -Recurse -Force .icon-build -ErrorAction SilentlyContinue
Write-Host '== Done: assets/clicker.ico' -ForegroundColor Green
