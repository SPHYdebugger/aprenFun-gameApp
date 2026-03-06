Add-Type -AssemblyName System.Drawing

$width = 22
$height = 16
$tile = 48
$outPath = "C:\PROYECTOS\naturais\AprendemosLaVida\story_map_actual.png"

function Color([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }

$DIRT = Color "#C6A26E"
$DIRT_DOT = Color "#B28C5C"
$GRASS = Color "#5FAE68"
$GRASS_DOT = Color "#4A9A55"
$TREE = Color "#2E6A35"
$TREE_DARK = Color "#244F2A"
$TREE_TRUNK = Color "#8B5A2B"
$ROCK = Color "#8E9399"
$ROCK_HI = Color "#A7ADB3"
$EXIT = Color "#F2C94C"
$TROPHY = Color "#F2C94C"

$bmp = New-Object System.Drawing.Bitmap ($width * $tile), ($height * $tile)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

$path = New-Object "System.Collections.Generic.HashSet[string]"
function Add-Path([int]$x, [int]$y) { [void]$path.Add("$x,$y") }

for ($x = 1; $x -le 18; $x++) { Add-Path $x 1 }
for ($y = 1; $y -le 4; $y++) { Add-Path 18 $y }
for ($x = 4; $x -le 18; $x++) { Add-Path $x 4 }
for ($y = 4; $y -le 8; $y++) { Add-Path 4 $y }
for ($x = 4; $x -le 15; $x++) { Add-Path $x 8 }
for ($y = 8; $y -le 11; $y++) { Add-Path 15 $y }
for ($x = 2; $x -le 15; $x++) { Add-Path $x 11 }

$types = @{}
for ($y = 0; $y -lt $height; $y++) {
    for ($x = 0; $x -lt $width; $x++) {
        $types["$x,$y"] = "GRASS"
    }
}

foreach ($k in $path) { $types[$k] = "DIRT" }

# Trees next to the main path.
$mainPath = New-Object System.Collections.ArrayList
for ($x = 1; $x -le 18; $x++) { [void]$mainPath.Add(@($x, 1)) }
for ($y = 1; $y -le 4; $y++) { [void]$mainPath.Add(@(18, $y)) }
for ($x = 4; $x -le 18; $x++) { [void]$mainPath.Add(@($x, 4)) }
for ($y = 4; $y -le 8; $y++) { [void]$mainPath.Add(@(4, $y)) }
for ($x = 4; $x -le 15; $x++) { [void]$mainPath.Add(@($x, 8)) }
for ($y = 8; $y -le 11; $y++) { [void]$mainPath.Add(@(15, $y)) }
for ($x = 2; $x -le 15; $x++) { [void]$mainPath.Add(@($x, 11)) }

foreach ($p in $mainPath) {
    [int]$x = $p[0]
    [int]$y = $p[1]
    $neighbors = @(
        @([int]($x - 1), [int]$y),
        @([int]($x + 1), [int]$y),
        @([int]$x, [int]($y - 1)),
        @([int]$x, [int]($y + 1))
    )
    foreach ($n in $neighbors) {
        [int]$nx = $n[0]
        [int]$ny = $n[1]
        if ($nx -ge 0 -and $ny -ge 0 -and $nx -lt $width -and $ny -lt $height) {
            if (-not $path.Contains("$nx,$ny")) {
                $types["$nx,$ny"] = "TREE"
            }
        }
    }
}

foreach ($r in @(@(6, 1), @(9, 4), @(12, 8))) {
    $types["$($r[0]),$($r[1])"] = "ROCK"
}

# Existing secret corridor with random entrance.
foreach ($p in @(@(6, 6), @(7, 6), @(8, 6), @(9, 6), @(10, 6))) {
    Add-Path $p[0] $p[1]
    $types["$($p[0]),$($p[1])"] = "DIRT"
}
$secretEntrances = @(@(5, 6), @(6, 5), @(10, 5))
foreach ($e in $secretEntrances) { $types["$($e[0]),$($e[1])"] = "TREE" }
$chosen = $secretEntrances | Get-Random
Add-Path $chosen[0] $chosen[1]

# Hidden zone exists but is hidden by default.
$hidden = New-Object "System.Collections.Generic.HashSet[string]"
for ($y = 7; $y -le 10; $y++) {
    for ($x = 16; $x -le 20; $x++) {
        Add-Path $x $y
        $types["$x,$y"] = "DIRT"
        [void]$hidden.Add("$x,$y")
    }
}
$hiddenEntrance = @(16, 8)
Add-Path $hiddenEntrance[0] $hiddenEntrance[1]
$types["$($hiddenEntrance[0]),$($hiddenEntrance[1])"] = "TREE"
[void]$hidden.Remove("$($hiddenEntrance[0]),$($hiddenEntrance[1])")

for ($y = 0; $y -lt $height; $y++) {
    for ($x = 0; $x -lt $width; $x++) {
        $rx = $x * $tile
        $ry = $y * $tile
        $key = "$x,$y"
        $type = $types[$key]
        if ($hidden.Contains($key)) { $type = "TREE" }

        if ($type -eq "DIRT") {
            $g.FillRectangle((New-Object System.Drawing.SolidBrush $DIRT), $rx, $ry, $tile, $tile)
            $dot = New-Object System.Drawing.SolidBrush $DIRT_DOT
            for ($i = 0; $i -lt 10; $i++) {
                $dx = $rx + (($i * 7) % $tile)
                $dy = $ry + (($i * 13) % $tile)
                $g.FillRectangle($dot, $dx, $dy, 2, 2)
            }
            continue
        }

        if ($type -eq "TREE") {
            $g.FillRectangle((New-Object System.Drawing.SolidBrush $TREE), $rx, $ry, $tile, $tile)
            $g.FillRectangle((New-Object System.Drawing.SolidBrush $TREE_DARK), $rx + [int]($tile * 0.1), $ry + [int]($tile * 0.1), [int]($tile * 0.8), [int]($tile * 0.55))
            $g.FillRectangle((New-Object System.Drawing.SolidBrush $TREE_TRUNK), $rx + [int]($tile * 0.42), $ry + [int]($tile * 0.55), [int]($tile * 0.16), [int]($tile * 0.35))
            continue
        }

        if ($type -eq "ROCK") {
            $g.FillRectangle((New-Object System.Drawing.SolidBrush $DIRT), $rx, $ry, $tile, $tile)
            $g.FillEllipse((New-Object System.Drawing.SolidBrush $ROCK), $rx + [int]($tile * 0.25), $ry + [int]($tile * 0.45), [int]($tile * 0.55), [int]($tile * 0.4))
            $g.FillEllipse((New-Object System.Drawing.SolidBrush $ROCK_HI), $rx + [int]($tile * 0.35), $ry + [int]($tile * 0.55), [int]($tile * 0.3), [int]($tile * 0.2))
            continue
        }

        $g.FillRectangle((New-Object System.Drawing.SolidBrush $GRASS), $rx, $ry, $tile, $tile)
        $dot = New-Object System.Drawing.SolidBrush $GRASS_DOT
        for ($i = 0; $i -lt 12; $i++) {
            $dx = $rx + (($i * 5) % $tile)
            $dy = $ry + (($i * 11) % $tile)
            $g.FillRectangle($dot, $dx, $dy, 1, 3)
        }
    }
}

# Exit.
$g.FillRectangle((New-Object System.Drawing.SolidBrush $EXIT), 2 * $tile, 11 * $tile, $tile, $tile)

# Trophy markers (hidden-zone trophy not visible while hidden).
$gates = @(@(6, 1), @(13, 1), @(18, 3), @(8, 4), @(4, 7), @(9, 6), @(16, 4), @(10, 8), @(7, 11), @(19, 9))
foreach ($gate in $gates) {
    $gx = $gate[0]
    $gy = $gate[1]
    if ($hidden.Contains("$gx,$gy")) { continue }
    $cx = $gx * $tile + ($tile / 2)
    $cy = $gy * $tile + ($tile / 2)
    $r = [int]($tile * 0.18)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush $TROPHY), $cx - $r, $cy - $r, $r * 2, $r * 2)
}

$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Output "CREATED:$outPath"
