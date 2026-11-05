$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap((Resolve-Path 'tmp-check/after.png').Path)
$W = $bmp.Width; $H = $bmp.Height

# The right card sits around x=1076..1306; sample strictly inside it.
$cardL = 1085; $cardR = 1295

# For each row compute mean luminance across the card interior.
$rowLum = New-Object double[] $H
for ($y = 130; $y -lt 760; $y++) {
    $sum = 0.0; $cnt = 0
    for ($x = $cardL; $x -le $cardR; $x++) {
        $c = $bmp.GetPixel($x, $y)
        $sum += 0.2126 * $c.R + 0.7152 * $c.G + 0.0722 * $c.B
        $cnt++
    }
    $rowLum[$y] = $sum / $cnt
}

# Strict local maxima with prominence >= 8 above the local median baseline.
$candidates = @()
for ($y = 135; $y -lt 755; $y++) {
    $v = $rowLum[$y]
    if ($v -gt ($rowLum[$y-1] + 4) -and $v -ge ($rowLum[$y+1] - 0)) {
        # local baseline: minimum in +/- 25 rows
        $base = $v
        for ($k = [Math]::Max(130, $y - 25); $k -le [Math]::Min(759, $y + 25); $k++) {
            if ($rowLum[$k] -lt $base) { $base = $rowLum[$k] }
        }
        if (($v - $base) -ge 8) {
            $candidates += [pscustomobject]@{ Y = $y; Lum = [Math]::Round($v,1); Base = [Math]::Round($base,1); Prom = [Math]::Round($v - $base,1) }
        }
    }
}

Write-Output "=== outline row candidates (card interior x=$cardL..$cardR) ==="
foreach ($c in $candidates) { Write-Output ("  y={0} lum={1} base={2} prominence={3}" -f $c.Y, $c.Lum, $c.Base, $c.Prom) }

# group rows closer than 4px into single outline line
$lines = @()
foreach ($c in $candidates) {
    $last = if ($lines.Count -gt 0) { $lines[-1] } else { $null }
    if ($null -ne $last -and ($c.Y - $last.start) -le 3) {
        if ($c.Lum -gt $last.lum) { $last.y = $c.Y; $last.lum = $c.Lum }
        $last.start = $c.Y
    } else {
        $lines += [pscustomobject]@{ y = $c.Y; lum = $c.Lum; start = $c.Y }
    }
}

Write-Output ""
Write-Output "=== grouped outline lines ==="
foreach ($l in $lines) { Write-Output ("  line y={0} lum={1}" -f $l.y, $l.lum) }

Write-Output ""
Write-Output "=== buttons (pairs of lines) ==="
$i = 0
$n = 1
while ($i + 1 -lt $lines.Count) {
    $top = $lines[$i].y
    $bot = $lines[$i+1].y
    $h = $bot - $top + 1
    if ($h -lt 15) { $i++; continue }

    # width from the top outline row
    $left = -1; $right = -1
    for ($x = $cardL - 12; $x -le $cardR + 12; $x++) {
        $c = $bmp.GetPixel($x, $top)
        $lum = 0.2126 * $c.R + 0.7152 * $c.G + 0.0722 * $c.B
        if ($lum -gt 60) { if ($left -lt 0) { $left = $x }; $right = $x }
    }
    Write-Output ("  btn{0}: top={1} bottom={2} height={3}px  width={4}px (x {5}..{6})" -f $n, $top, $bot, $h, ($right - $left + 1), $left, $right)
    $n++
    $i += 2
}
$bmp.Dispose()
