$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap((Resolve-Path 'tmp-check/after.png').Path)
$cardL = 1079; $cardR = 1301

$outlineRows = @()
for ($y = 130; $y -lt 760; $y++) {
    $lit = 0; $total = 0
    for ($x = $cardL; $x -le $cardR; $x++) {
        $c = $bmp.GetPixel($x, $y)
        $lum = 0.2126 * $c.R + 0.7152 * $c.G + 0.0722 * $c.B
        $total++
        if ($lum -gt 55) { $lit++ }
    }
    $frac = $lit / $total
    if ($frac -ge 0.95) { $outlineRows += [pscustomobject]@{ Y = $y; Frac = [Math]::Round($frac,2) } }
}

Write-Output "=== rows lit across full card width (outline lines) ==="
foreach ($r in $outlineRows) { Write-Output ("  y={0} coverage={1}" -f $r.Y, $r.Frac) }

$lines = @()
foreach ($r in $outlineRows) {
    if ($lines.Count -gt 0 -and ($r.Y - $lines[-1].last) -le 2) {
        $lines[-1].last = $r.Y
        $lines[-1].y = [int](($lines[-1].y + $r.Y) / 2)
    } else {
        $lines += [pscustomobject]@{ y = $r.Y; last = $r.Y }
    }
}

Write-Output ""
Write-Output "=== distinct outline lines ==="
foreach ($l in $lines) { Write-Output ("  line y={0} (..{1})" -f $l.y, $l.last) }

Write-Output ""
Write-Output "=== buttons: pairs of outline lines ==="
$i = 0; $n = 1
while ($i + 1 -lt $lines.Count) {
    $top = $lines[$i].y
    $bot = $lines[$i+1].y
    $h = $bot - $top + 1
    if ($h -lt 20) { $i++; continue }
    Write-Output ("  button{0}: top={1} bottom={2}  HEIGHT={3}px  WIDTH={4}px" -f $n, $top, $bot, $h, ($cardR - $cardL + 1))
    $n++; $i += 2
}
$bmp.Dispose()
