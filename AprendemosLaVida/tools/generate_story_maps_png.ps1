Add-Type -AssemblyName System.Drawing

$width = 22
$height = 16
$tile = 48
$outputDir = "C:\PROYECTOS\naturais\AprendemosLaVida\tools\map_previews"

if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

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
$START = Color "#4A90E2"
$HIDDEN_OVERLAY = [System.Drawing.Color]::FromArgb(105, 128, 128, 128)
$SECRET_BORDER = Color "#FF6B6B"
$ACCESS_BORDER = Color "#6ECF68"

function New-Set {
    return New-Object "System.Collections.Generic.HashSet[string]"
}

function Add-Path([System.Collections.Generic.HashSet[string]]$set, [int]$x, [int]$y) {
    [void]$set.Add("$x,$y")
}

function Add-Horizontal([System.Collections.Generic.HashSet[string]]$set, [int]$xStart, [int]$y, [int]$xEnd) {
    $from = [Math]::Min($xStart, $xEnd)
    $to = [Math]::Max($xStart, $xEnd)
    for ($x = $from; $x -le $to; $x++) { Add-Path $set $x $y }
}

function Add-Vertical([System.Collections.Generic.HashSet[string]]$set, [int]$x, [int]$yStart, [int]$yEnd) {
    $from = [Math]::Min($yStart, $yEnd)
    $to = [Math]::Max($yStart, $yEnd)
    for ($y = $from; $y -le $to; $y++) { Add-Path $set $x $y }
}

function Rect-Tiles([int]$xFrom, [int]$yFrom, [int]$xTo, [int]$yTo) {
    $list = New-Object System.Collections.ArrayList
    for ($y = $yFrom; $y -le $yTo; $y++) {
        for ($x = $xFrom; $x -le $xTo; $x++) {
            [void]$list.Add(@($x, $y))
        }
    }
    return $list
}

function Build-MapDef(
    [string]$name,
    [scriptblock]$buildMainPath,
    [array]$secretPath,
    [array]$secretEntrances,
    [array]$hiddenZoneTiles,
    [array]$hiddenAccess,
    [array]$rocks,
    [int]$startX,
    [int]$startY,
    [int]$exitX,
    [int]$exitY
) {
    $path = New-Set
    & $buildMainPath $path
    foreach ($p in $secretPath) { Add-Path $path $p[0] $p[1] }
    foreach ($p in $hiddenZoneTiles) { Add-Path $path $p[0] $p[1] }
    foreach ($p in $hiddenAccess) { Add-Path $path $p[0] $p[1] }

    return @{
        Name = $name
        Path = $path
        SecretPath = $secretPath
        SecretEntrances = $secretEntrances
        HiddenZoneTiles = $hiddenZoneTiles
        HiddenAccess = $hiddenAccess
        Rocks = $rocks
        Start = @($startX, $startY)
        Exit = @($exitX, $exitY)
    }
}

function Draw-Map($map, [string]$outFile) {
    $bmp = New-Object System.Drawing.Bitmap ($width * $tile), ($height * $tile)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

    $types = @{}
    for ($y = 0; $y -lt $height; $y++) {
        for ($x = 0; $x -lt $width; $x++) {
            $types["$x,$y"] = "GRASS"
        }
    }

    foreach ($k in $map.Path) { $types[$k] = "DIRT" }

    foreach ($k in $map.Path) {
        $keyText = [string]$k
        if ($keyText -notmatch '^\d+,\d+$') { continue }
        $parts = $keyText.Split(',')
        if ($parts.Length -lt 2) { continue }
        $xParsed = 0
        $yParsed = 0
        $xOk = [int]::TryParse($parts[0], [ref]$xParsed)
        $yOk = [int]::TryParse($parts[1], [ref]$yParsed)
        if (-not $xOk -or -not $yOk) { continue }
        [int]$x = $xParsed
        [int]$y = $yParsed
        $neighbors = @(
            @($x - 1, $y),
            @($x + 1, $y),
            @($x, $y - 1),
            @($x, $y + 1)
        )
        foreach ($n in $neighbors) {
            [int]$nx = $n[0]
            [int]$ny = $n[1]
            if ($nx -ge 0 -and $ny -ge 0 -and $nx -lt $width -and $ny -lt $height) {
                $key = "$nx,$ny"
                if (-not $map.Path.Contains($key)) {
                    $types[$key] = "TREE"
                }
            }
        }
    }

    foreach ($r in $map.Rocks) {
        $types["$($r[0]),$($r[1])"] = "ROCK"
    }
    foreach ($e in $map.SecretEntrances) {
        $types["$($e[0]),$($e[1])"] = "TREE"
    }
    foreach ($a in $map.HiddenAccess) {
        $types["$($a[0]),$($a[1])"] = "TREE"
    }

    for ($y = 0; $y -lt $height; $y++) {
        for ($x = 0; $x -lt $width; $x++) {
            $rx = $x * $tile
            $ry = $y * $tile
            $key = "$x,$y"
            $type = $types[$key]

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

    # Hidden-zone overlay for preview purposes.
    foreach ($h in $map.HiddenZoneTiles) {
        $g.FillRectangle(
            (New-Object System.Drawing.SolidBrush $HIDDEN_OVERLAY),
            $h[0] * $tile,
            $h[1] * $tile,
            $tile,
            $tile
        )
    }

    # Mark secret entrance candidates (red border).
    foreach ($e in $map.SecretEntrances) {
        $pen = New-Object System.Drawing.Pen($SECRET_BORDER, 3)
        $g.DrawRectangle($pen, $e[0] * $tile + 2, $e[1] * $tile + 2, $tile - 4, $tile - 4)
    }

    # Mark hidden access (green border).
    foreach ($a in $map.HiddenAccess) {
        $pen = New-Object System.Drawing.Pen($ACCESS_BORDER, 3)
        $g.DrawRectangle($pen, $a[0] * $tile + 4, $a[1] * $tile + 4, $tile - 8, $tile - 8)
    }

    # Start and exit.
    $g.FillRectangle((New-Object System.Drawing.SolidBrush $START), $map.Start[0] * $tile, $map.Start[1] * $tile, $tile, $tile)
    $g.FillRectangle((New-Object System.Drawing.SolidBrush $EXIT), $map.Exit[0] * $tile, $map.Exit[1] * $tile, $tile, $tile)

    # Title strip
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(170, 255, 255, 255))), 0, 0, $width * $tile, 34)
    $font = New-Object System.Drawing.Font("Arial", 14, [System.Drawing.FontStyle]::Bold)
    $g.DrawString($map.Name, $font, [System.Drawing.Brushes]::Black, 8, 6)

    $bmp.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
}

$maps = @(
    (Build-MapDef -name "Map 0 - Default" -buildMainPath {
        param($p)
        Add-Horizontal $p 1 1 18
        Add-Vertical $p 18 1 4
        Add-Horizontal $p 4 4 18
        Add-Vertical $p 4 4 8
        Add-Horizontal $p 4 8 15
        Add-Vertical $p 15 8 11
        Add-Horizontal $p 2 11 15
    } -secretPath @(@(6,6),@(7,6),@(8,6),@(9,6),@(10,6)) -secretEntrances @(@(5,6),@(6,5),@(10,5)) -hiddenZoneTiles (Rect-Tiles 10 13 17 14) -hiddenAccess @(@(16,11),@(16,12)) -rocks @(@(6,1),@(9,4),@(12,8)) -startX 1 -startY 1 -exitX 2 -exitY 11),

    (Build-MapDef -name "Map 1 - Variant 1" -buildMainPath {
        param($p)
        Add-Horizontal $p 1 1 19
        Add-Vertical $p 19 1 5
        Add-Horizontal $p 3 5 19
        Add-Vertical $p 3 5 9
        Add-Horizontal $p 3 9 16
        Add-Vertical $p 16 9 12
        Add-Horizontal $p 2 12 16
    } -secretPath @(@(7,7),@(8,7),@(9,7),@(10,7)) -secretEntrances @(@(6,7),@(7,6),@(10,6)) -hiddenZoneTiles (Rect-Tiles 11 14 18 15) -hiddenAccess @(@(15,13),@(15,14)) -rocks @(@(6,1),@(11,5),@(9,9)) -startX 1 -startY 1 -exitX 2 -exitY 12),

    (Build-MapDef -name "Map 2 - Variant 2" -buildMainPath {
        param($p)
        Add-Vertical $p 2 1 12
        Add-Horizontal $p 2 12 18
        Add-Vertical $p 18 3 12
        Add-Horizontal $p 5 4 18
        Add-Vertical $p 4 5 9
        Add-Horizontal $p 4 9 15
        Add-Vertical $p 15 9 13
    } -secretPath @(@(9,4),@(9,5),@(9,6),@(10,6)) -secretEntrances @(@(8,5),@(10,5),@(10,6)) -hiddenZoneTiles (Rect-Tiles 16 13 20 15) -hiddenAccess @(@(16,13),@(17,13)) -rocks @(@(2,7),@(12,12),@(13,9)) -startX 2 -startY 1 -exitX 15 -exitY 13),

    (Build-MapDef -name "Map 3 - Variant 3 (Laberinto)" -buildMainPath {
        param($p)
        Add-Horizontal $p 1 1 19
        Add-Horizontal $p 1 5 17
        Add-Horizontal $p 3 9 19
        Add-Horizontal $p 2 13 20
        Add-Vertical $p 3 1 13
        Add-Vertical $p 7 1 9
        Add-Vertical $p 11 5 13
        Add-Vertical $p 15 1 13
        Add-Vertical $p 18 9 13
        Add-Vertical $p 20 12 13
    } -secretPath @(@(8,6),@(9,6),@(10,6),@(11,6)) -secretEntrances @(@(7,6),@(8,5),@(11,5)) -hiddenZoneTiles (Rect-Tiles 4 14 8 15) -hiddenAccess @(@(8,13),@(8,14)) -rocks @(@(5,1),@(12,9),@(16,5),@(19,13)) -startX 1 -startY 1 -exitX 20 -exitY 13),

    (Build-MapDef -name "Map 4 - Variant 4" -buildMainPath {
        param($p)
        Add-Horizontal $p 1 1 19
        Add-Vertical $p 19 1 7
        Add-Horizontal $p 4 7 19
        Add-Vertical $p 19 7 11
        Add-Horizontal $p 17 8 19
        Add-Vertical $p 17 8 11
        Add-Horizontal $p 17 11 19
        Add-Vertical $p 6 7 14
        Add-Horizontal $p 6 11 17
        Add-Vertical $p 16 11 14
        Add-Horizontal $p 6 14 16
    } -secretPath @(@(8,3),@(9,3),@(10,3),@(11,3),@(12,3),@(13,3),@(14,3),@(15,3),@(11,4),@(11,5),@(10,5),@(12,5)) -secretEntrances @(@(10,6),@(11,6),@(12,6)) -hiddenZoneTiles (Rect-Tiles 1 12 5 14) -hiddenAccess @(@(6,13),@(6,14)) -rocks @(@(8,1),@(14,7),@(11,11)) -startX 1 -startY 1 -exitX 4 -exitY 7)
)

$index = 0
foreach ($map in $maps) {
    $file = Join-Path $outputDir ("story_map_{0}_{1}.png" -f $index, ($map.Name.ToLower().Replace(" ", "_").Replace("(", "").Replace(")", "")))
    Draw-Map $map $file
    Write-Output "CREATED:$file"
    $index++
}
