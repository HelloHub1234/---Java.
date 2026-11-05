$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap((Resolve-Path 'tmp-check/after.png').Path)
$W = $bmp.Width; $H = $bmp.Height
Write-Output "image ${W}x${H}"

# locate the right-hand card: saturated accent-blue pixels in the right part of the screen
$minX = $W; $maxX = -1; $minY = $H; $maxY = -1
for ($x = 900; $x -lt $W; $x++) {
    for ($y = 0; $y -lt $H; $y++) {
        $c = $bmp.GetPixel($x, $y)
        if ($c.B -gt 120 -and $c.B -gt ($c.R + 40) -and $c.B -gt ($c.G + 20)) {
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
Write-Output "accent bbox right: L=$minX R=$maxX T=$minY B=$maxY"

# per-row mean luminance across the card width
$x0 = [Math]::Max(0, $minX - 4)
$x1 = [Math]::Min($W - 1, $maxX + 4)
$rowLum = New-Object double[] $H
for ($y = 0; $y -lt $H; $y++) {
    $sum = 0.0; $cnt = 0
    for ($x = $x0; $x -le $x1; $x++) {
        $c = $bmp.GetPixel($x, $y)
        $sum += 0.2126 * $c.R + 0.7152 * $c.G + 0.0722 * $c.B
        $cnt++
    }
    if ($cnt -gt 0) { $rowLum[$y] = $sum / $cnt }
}

# detect bright outline rows (local maxima above threshold)
$bright = @()
for ($y = 1; $y -lt ($H - 1); $y++) {
    if ($rowLum[$y] -gt 65 -and $rowLum[$y] -ge $rowLum[$y-1] -and $rowLum[$y] -ge $rowLum[$y+1]) {
        $bright += $y
    }
}
Write-Output ("outline rows: " + ($bright -join ','))
Write-Output ("row luminance detail:")
foreach ($y in $bright) {
    Write-Output ("  y=$y lum=" + [Math]::Round($rowLum[$y],1))
}

# horizontal extent of the button outline: scan a row that is an outline row
if ($bright.Count -gt 0) {
    $ry = $bright[0]
    $left = -1; $right = -1
    for ($x = $x0; $x -le $x1; $x++) {
        $c = $bmp.GetPixel($x, $ry)
        $lum = 0.2126 * $c.R + 0.7152 * $c.G + 0.0722 * $c.B
        if ($lum -gt 65) { if ($left -lt 0) { $left = $x }; $right = $x }
    }
    Write-Output "first outline row y=$ry spans x=$left..$right (width $($right - $left + 1))"
}
$bmp.Dispose()
