# Перегенерирует assets/clicker.ico из кода отрисовки иконки (src/WindowApps/AppIconPainter.java).
# Запуск из корня проекта:  pwsh -File tools/generate-icon.ps1
# Нужен только после правок AppIconPainter — сам .ico лежит в репозитории.
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

javac -encoding UTF-8 -d .icon-build src/WindowApps/Theme.java src/WindowApps/AppIconPainter.java src/tools/IconExport.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

java "-Djava.awt.headless=true" -cp .icon-build tools.IconExport assets/clicker.ico
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Remove-Item -Recurse -Force .icon-build -ErrorAction SilentlyContinue
