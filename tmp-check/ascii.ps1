$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap((Resolve-Path 'tmp-check/after.png').Path)
$cardL = 1085; $cardR = 1295

$sep = '=' * 70
Write-Output "ASCII map of right card (x=$cardL..$cardR), 1 char = 1 row, 1 char = 4 px horizontally"
Write-Output $sep

for ($y = 125; $y -lt 760; $y += 2) {
    $line = New-Object System.Text.StringBuilder
    $avg = 0.0; $cnt = 0
    for ($x = $cardL; $x -le $cardR; $x += 4) {
        # max luminance within this 4px block so thin outlines stay visible
        $mx = 0.0
        for ($k = 0; $k -lt 4; $k++) {
            $xx = $x + $k
            if ($xx -gt $cardR) { break }
            $c = $bmp.GetPixel($xx, $y)
            $lum = 0.2126 * $c.R + 0.7152 * $c.G + 0.0722 * $c.B
            if ($lum -gt $mx) { $mx = $lum }
        }
        $avg += $mx; $cnt++
        if ($mx -gt 120) { [void]$line.Append('#') }
        elseif ($mx -gt 75) { [void]$line.Append('+') }
        elseif ($mx -gt 45) { [void]$line.Append('.') }
        else { [void]$line.Append(' ') }
    }
    $avgLum = $avg / $cnt
    Write-Output ("{0,4} [{1,5}] {2}" -f $y, [Math]::Round($avgLum,0), $line.ToString())
}
$bmp.Dispose()
