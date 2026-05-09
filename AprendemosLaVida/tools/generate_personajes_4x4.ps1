param(
    [string]$InputDir = "app/src/main/assets/avatar/personajes",
    [string]$OutputDir = "app/src/main/assets/avatar/personajes_4x4"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function New-ArgbBitmap {
    param([int]$Width, [int]$Height)
    return New-Object System.Drawing.Bitmap($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Get-QuarterRects {
    param([int]$Width, [int]$Height)
    $base = [int][Math]::Floor($Width / 4)
    $extra = $Width - ($base * 4)
    $rects = @()
    $x = 0
    for ($i = 0; $i -lt 4; $i++) {
        $w = $base + $(if ($i -eq 3) { $extra } else { 0 })
        $rects += (New-Object System.Drawing.Rectangle($x, 0, $w, $Height))
        $x += $w
    }
    return $rects
}

function Inset-Rect {
    param(
        [System.Drawing.Rectangle]$Rect,
        [int]$Inset
    )
    $x = $Rect.X + $Inset
    $y = $Rect.Y + $Inset
    $w = [Math]::Max(1, $Rect.Width - ($Inset * 2))
    $h = [Math]::Max(1, $Rect.Height - ($Inset * 2))
    return [System.Drawing.Rectangle]::new($x, $y, $w, $h)
}

function DrawFrame {
    param(
        [System.Drawing.Graphics]$Graphics,
        [System.Drawing.Bitmap]$Source,
        [System.Drawing.Rectangle]$SourceRect,
        [System.Drawing.Rectangle]$CellRect,
        [double]$Scale,
        [double]$OffsetX,
        [double]$OffsetY
    )

    $drawW = [int][Math]::Round($CellRect.Width * $Scale)
    $drawH = [int][Math]::Round($CellRect.Height * $Scale)
    $left = $CellRect.X + [int][Math]::Round(($CellRect.Width - $drawW) / 2.0 + $OffsetX)
    $top = $CellRect.Y + [int][Math]::Round(($CellRect.Height - $drawH) / 2.0 + $OffsetY)
    $dst = New-Object System.Drawing.Rectangle($left, $top, $drawW, $drawH)

    $Graphics.DrawImage(
        $Source,
        $dst,
        $SourceRect.X,
        $SourceRect.Y,
        $SourceRect.Width,
        $SourceRect.Height,
        [System.Drawing.GraphicsUnit]::Pixel
    )
}

$root = (Resolve-Path ".").Path
$inPath = Join-Path $root $InputDir
$outPath = Join-Path $root $OutputDir

if (-not (Test-Path $inPath)) {
    throw "No existe la carpeta de entrada: $inPath"
}
New-Item -ItemType Directory -Path $outPath -Force | Out-Null

$files = Get-ChildItem -Path $inPath -File -Filter "*.png" | Sort-Object Name
if ($files.Count -eq 0) {
    throw "No hay PNG en $inPath"
}

# Frame pattern to simulate walk cycle.
$offsetY = @(0, -8, 0, 8)
$offsetXFrontBack = @(0, 2, 0, -2)
$offsetXSide = @(0, 3, 0, -3)
$scalePattern = @(1.00, 0.992, 1.00, 0.992)

$generated = 0
foreach ($file in $files) {
    $src = [System.Drawing.Bitmap]::FromFile($file.FullName)
    try {
        $quarterRects = Get-QuarterRects -Width $src.Width -Height $src.Height

        # Input order (from your source tilesets): front, back, right, left.
        $frontRect = Inset-Rect -Rect ([System.Drawing.Rectangle]$quarterRects[0]) -Inset 4
        $backRect = Inset-Rect -Rect ([System.Drawing.Rectangle]$quarterRects[1]) -Inset 4
        $rightRect = Inset-Rect -Rect ([System.Drawing.Rectangle]$quarterRects[2]) -Inset 4
        $leftRect = Inset-Rect -Rect ([System.Drawing.Rectangle]$quarterRects[3]) -Inset 4

        $tileW = [int]$frontRect.Width
        $tileH = [int]$frontRect.Height
        $sheet = New-ArgbBitmap -Width ($tileW * 4) -Height ($tileH * 4)
        $g = [System.Drawing.Graphics]::FromImage($sheet)
        try {
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
            $g.Clear([System.Drawing.Color]::Transparent)

            for ($f = 0; $f -lt 4; $f++) {
                # Row order chosen for typical game runtime: up, right, down, left.
                $xCell = [int]$f * [int]$tileW
                $upCell = [System.Drawing.Rectangle]::new([int]$xCell, [int](0 * $tileH), [int]$tileW, [int]$tileH)
                $rightCell = [System.Drawing.Rectangle]::new([int]$xCell, [int](1 * $tileH), [int]$tileW, [int]$tileH)
                $downCell = [System.Drawing.Rectangle]::new([int]$xCell, [int](2 * $tileH), [int]$tileW, [int]$tileH)
                $leftCell = [System.Drawing.Rectangle]::new([int]$xCell, [int](3 * $tileH), [int]$tileW, [int]$tileH)

                DrawFrame -Graphics $g -Source $src -SourceRect $backRect -CellRect $upCell -Scale $scalePattern[$f] -OffsetX $offsetXFrontBack[$f] -OffsetY $offsetY[$f]
                DrawFrame -Graphics $g -Source $src -SourceRect $rightRect -CellRect $rightCell -Scale $scalePattern[$f] -OffsetX $offsetXSide[$f] -OffsetY $offsetY[$f]
                DrawFrame -Graphics $g -Source $src -SourceRect $frontRect -CellRect $downCell -Scale $scalePattern[$f] -OffsetX $offsetXFrontBack[$f] -OffsetY $offsetY[$f]
                DrawFrame -Graphics $g -Source $src -SourceRect $leftRect -CellRect $leftCell -Scale $scalePattern[$f] -OffsetX (-$offsetXSide[$f]) -OffsetY $offsetY[$f]
            }
        } finally {
            $g.Dispose()
        }

        try {
            $outName = [System.IO.Path]::GetFileNameWithoutExtension($file.Name) + "_4x4.png"
            $outFile = Join-Path $outPath $outName
            $sheet.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
            $generated++
        } finally {
            $sheet.Dispose()
        }
    } finally {
        $src.Dispose()
    }
}

Write-Output "Tilesets 4x4 generados: $generated"
Write-Output "Salida: $outPath"
