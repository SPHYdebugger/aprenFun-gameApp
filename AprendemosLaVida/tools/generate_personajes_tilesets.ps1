param(
    [string]$FrontDir = "app/src/main/assets/avatar/generated_pose_variants_v4/front",
    [string]$OutputDir = "app/src/main/assets/avatar/generated_pose_variants_v4/personajes"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function New-Canvas {
    param([int]$W, [int]$H)
    return New-Object System.Drawing.Bitmap($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Draw-CheckerBackground {
    param(
        [System.Drawing.Graphics]$G,
        [int]$W,
        [int]$H,
        [int]$Cell = 44
    )
    $c1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 237, 237, 239))
    $c2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 228, 228, 230))
    try {
        for ($y = 0; $y -lt $H; $y += $Cell) {
            for ($x = 0; $x -lt $W; $x += $Cell) {
                $isEven = (([int]($x / $Cell) + [int]($y / $Cell)) % 2) -eq 0
                $brush = if ($isEven) { $c1 } else { $c2 }
                $G.FillRectangle($brush, $x, $y, $Cell, $Cell)
            }
        }
    } finally {
        $c1.Dispose()
        $c2.Dispose()
    }
}

function Get-ColorSample {
    param(
        [System.Drawing.Bitmap]$Bmp,
        [double]$XNorm,
        [double]$YNorm,
        [System.Drawing.Color]$Fallback
    )
    $x = [int]([Math]::Round(($Bmp.Width - 1) * $XNorm))
    $y = [int]([Math]::Round(($Bmp.Height - 1) * $YNorm))
    if ($x -lt 0 -or $y -lt 0 -or $x -ge $Bmp.Width -or $y -ge $Bmp.Height) { return $Fallback }
    $c = $Bmp.GetPixel($x, $y)
    if ($c.A -lt 16) { return $Fallback }
    return $c
}

function Create-BackPose {
    param([System.Drawing.Bitmap]$Front)
    $w = $Front.Width
    $h = $Front.Height
    $bmp = New-Canvas -W $w -H $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

        # Base: mirrored + slightly darker.
        $attrs = New-Object System.Drawing.Imaging.ImageAttributes
        $cm = New-Object System.Drawing.Imaging.ColorMatrix
        $cm.Matrix00 = 0.84
        $cm.Matrix11 = 0.84
        $cm.Matrix22 = 0.84
        $cm.Matrix33 = 1.0
        $cm.Matrix44 = 1.0
        $attrs.SetColorMatrix($cm)

        $g.TranslateTransform($w, 0)
        $g.ScaleTransform(-1, 1)
        $dstRect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
        $g.DrawImage($Front, $dstRect, 0, 0, $w, $h, [System.Drawing.GraphicsUnit]::Pixel, $attrs)
        $g.ResetTransform()
        $attrs.Dispose()

        # Keep it clean: no extra paint artifacts, just mirrored + darkened.
    } finally {
        $g.Dispose()
    }
    return $bmp
}

function Create-ProfileRightPose {
    param([System.Drawing.Bitmap]$Front)
    $w = $Front.Width
    $h = $Front.Height
    $bmp = New-Canvas -W $w -H $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

        # Faux side view: compress X and shift to the right.
        $scaleX = 0.72
        $drawW = [int]([Math]::Round($w * $scaleX))
        $drawX = [int]([Math]::Round(($w - $drawW) / 2.0 + ($w * 0.07)))
        $g.DrawImage($Front, $drawX, 0, $drawW, $h)

        # Clean pseudo-profile from front without paint overlays.
    } finally {
        $g.Dispose()
    }
    return $bmp
}

function Mirror-Horizontal {
    param([System.Drawing.Bitmap]$Source)
    $w = $Source.Width
    $h = $Source.Height
    $bmp = New-Canvas -W $w -H $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.TranslateTransform($w, 0)
        $g.ScaleTransform(-1, 1)
        $g.DrawImage($Source, 0, 0, $w, $h)
        $g.ResetTransform()
    } finally {
        $g.Dispose()
    }
    return $bmp
}

function Compose-Tileset {
    param(
        [System.Drawing.Bitmap]$Front,
        [System.Drawing.Bitmap]$Back,
        [System.Drawing.Bitmap]$ProfileRight,
        [System.Drawing.Bitmap]$ProfileLeft
    )
    $w = $Front.Width
    $h = $Front.Height
    $tileset = New-Canvas -W ($w * 4) -H $h
    $g = [System.Drawing.Graphics]::FromImage($tileset)
    try {
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        Draw-CheckerBackground -G $g -W ($w * 4) -H $h
        $g.DrawImage($Front, 0, 0, $w, $h)
        $g.DrawImage($Back, $w, 0, $w, $h)
        $g.DrawImage($ProfileRight, $w * 2, 0, $w, $h)
        $g.DrawImage($ProfileLeft, $w * 3, 0, $w, $h)
    } finally {
        $g.Dispose()
    }
    return $tileset
}

$root = (Resolve-Path ".").Path
$frontPath = Join-Path $root $FrontDir
$outputPath = Join-Path $root $OutputDir

if (-not (Test-Path $frontPath)) {
    throw "No existe la carpeta front: $frontPath"
}
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

$files = Get-ChildItem -Path $frontPath -File -Filter "*.png" | Sort-Object Name
if ($files.Count -eq 0) {
    throw "No hay PNG en $frontPath"
}

$count = 0
foreach ($file in $files) {
    $front = [System.Drawing.Bitmap]::FromFile($file.FullName)
    try {
        $back = Create-BackPose -Front $front
        $profileRight = Create-ProfileRightPose -Front $front
        $profileLeft = Mirror-Horizontal -Source $profileRight
        try {
            $tileset = Compose-Tileset -Front $front -Back $back -ProfileRight $profileRight -ProfileLeft $profileLeft
            try {
                $outFile = Join-Path $outputPath $file.Name
                $tileset.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
                $count++
            } finally {
                $tileset.Dispose()
            }
        } finally {
            $back.Dispose()
            $profileRight.Dispose()
            $profileLeft.Dispose()
        }
    } finally {
        $front.Dispose()
    }
}

Write-Output "Tilesets generados: $count"
Write-Output "Destino: $outputPath"
