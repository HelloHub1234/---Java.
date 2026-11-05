# Локальная сборка Clicker-windows.zip — то же, что делает CI (.github/workflows/build.yml),
# один в один, включая иконку .exe. Запуск из корня проекта:  pwsh -File tools/build-clicker.ps1
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

$icon = 'assets/clicker.ico'
if (-not (Test-Path $icon)) {
    throw "Нет $icon — сгенерируй его: tools/generate-icon.ps1"
}

Remove-Item -Recurse -Force classes, dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path classes, dist | Out-Null

Write-Host '== Компиляция' -ForegroundColor Cyan
javac -encoding UTF-8 -d classes (Get-ChildItem src/WindowApps/*.java) (Get-ChildItem src/WindowApps/clicker/*.java)
Copy-Item src/WindowApps/messages*.properties classes/WindowApps/

Write-Host '== jar' -ForegroundColor Cyan
jar --create --file dist/clicker.jar --main-class WindowApps.clicker.Clicker -C classes WindowApps

Write-Host '== jpackage' -ForegroundColor Cyan
# --main-jar ищется внутри --input (classes), поэтому jar кладём туда
Copy-Item dist/clicker.jar classes/clicker.jar
jpackage --type app-image --name Clicker --input classes --main-jar clicker.jar `
    --main-class WindowApps.clicker.Clicker --icon $icon `
    --dest dist/Clicker --java-options "-Dfile.encoding=UTF-8"
Compress-Archive -Path dist/Clicker -DestinationPath dist/Clicker-windows.zip -Force

Write-Host "Готово: dist/Clicker-windows.zip и dist/clicker.jar" -ForegroundColor Green
