param(
    [string]$AssetsDir = "app/src/main/assets/avatar",
    [string]$OutputDir = "app/src/main/assets/avatar/generated_pose_variants_v1"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function New-RenderBitmap {
    param([int]$Width, [int]$Height)
    return New-Object System.Drawing.Bitmap($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Save-Png {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [string]$Path
    )
    $Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function Compose-Front {
    param(
        [System.Drawing.Image]$Body,
        [System.Drawing.Image]$Hair,
        [System.Drawing.Image]$Outfit
    )
    $w = $Body.Width
    $h = $Body.Height
    $bmp = New-RenderBitmap -Width $w -Height $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $g.DrawImage($Body, 0, 0, $w, $h)
        $g.DrawImage($Outfit, 0, 0, $w, $h)
        $g.DrawImage($Hair, 0, 0, $w, $h)
    } finally {
        $g.Dispose()
    }
    return $bmp
}

function New-ProfileVariant {
    param([System.Drawing.Bitmap]$Front)
    $w = $Front.Width
    $h = $Front.Height
    $bmp = New-RenderBitmap -Width $w -Height $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

        # Compress width and shift to suggest side profile.
        $profileScaleX = 0.68
        $dstW = [int]([Math]::Round($w * $profileScaleX))
        $dstH = $h
        $dstX = [int]([Math]::Round(($w - $dstW) / 2.0 + 10))
        $dstY = 0
        $g.DrawImage($Front, $dstX, $dstY, $dstW, $dstH)

        # Subtle dark strip to hint depth separation.
        $shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(34, 0, 0, 0))
        try {
            $g.FillRectangle($shadowBrush, [int]($dstX + $dstW * 0.62), [int]($h * 0.15), [int]($dstW * 0.18), [int]($h * 0.72))
        } finally {
            $shadowBrush.Dispose()
        }
    } finally {
        $g.Dispose()
    }
    return $bmp
}

function New-BackVariant {
    param([System.Drawing.Bitmap]$Front)
    $w = $Front.Width
    $h = $Front.Height
    $bmp = New-RenderBitmap -Width $w -Height $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

        # Mirror horizontally and darken to suggest rear view.
        $attributes = New-Object System.Drawing.Imaging.ImageAttributes
        $matrix = New-Object System.Drawing.Imaging.ColorMatrix
        $matrix.Matrix00 = 0.78
        $matrix.Matrix11 = 0.78
        $matrix.Matrix22 = 0.78
        $matrix.Matrix33 = 1.0
        $matrix.Matrix44 = 1.0
        $attributes.SetColorMatrix($matrix)

        $g.TranslateTransform($w, 0)
        $g.ScaleTransform(-1, 1)
        $srcRect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
        $dstRect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
        $g.DrawImage($Front, $dstRect, 0, 0, $w, $h, [System.Drawing.GraphicsUnit]::Pixel, $attributes)
        $g.ResetTransform()

        # Slight top shadow to make "back of head" more readable.
        $shade = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(22, 0, 0, 0))
        try {
            $g.FillRectangle($shade, [int]($w * 0.2), [int]($h * 0.05), [int]($w * 0.6), [int]($h * 0.22))
        } finally {
            $shade.Dispose()
        }
        $attributes.Dispose()
    } finally {
        $g.Dispose()
    }
    return $bmp
}

$root = (Resolve-Path ".").Path
$assetsPath = Join-Path $root $AssetsDir
$outputPath = Join-Path $root $OutputDir
$frontDir = Join-Path $outputPath "front"
$profileDir = Join-Path $outputPath "profile"
$backDir = Join-Path $outputPath "back"

New-Item -ItemType Directory -Path $frontDir -Force | Out-Null
New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
New-Item -ItemType Directory -Path $backDir -Force | Out-Null

$bodyFiles = @(
    "base_body_skin_01.png",
    "base_body_skin_03.png",
    "base_body_skin_05.png"
) | ForEach-Object { Join-Path $assetsPath $_ }

$hairFiles = @(
    "hair_boy_01_c0.png",
    "hair_boy_01_c1.png",
    "hair_boy_01_c2.png",
    "hair_boy_02_c0.png",
    "hair_boy_02_c1.png",
    "hair_boy_02_c2.png",
    "hair_boy_03_c0.png",
    "hair_boy_03_c1.png",
    "hair_boy_03_c2.png"
) | ForEach-Object { Join-Path $assetsPath $_ }

$outfitFiles = 1..7 | ForEach-Object {
    Join-Path $assetsPath ("outfit_boy_{0:D2}.png" -f $_)
}

foreach ($path in ($bodyFiles + $hairFiles + $outfitFiles)) {
    if (-not (Test-Path $path)) {
        throw "Falta asset requerido: $path"
    }
}

$bodyImages = @{}
$hairImages = @{}
$outfitImages = @{}
try {
    foreach ($path in $bodyFiles) { $bodyImages[$path] = [System.Drawing.Image]::FromFile($path) }
    foreach ($path in $hairFiles) { $hairImages[$path] = [System.Drawing.Image]::FromFile($path) }
    foreach ($path in $outfitFiles) { $outfitImages[$path] = [System.Drawing.Image]::FromFile($path) }

    $count = 0
    for ($bi = 0; $bi -lt $bodyFiles.Count; $bi++) {
        $bodyPath = $bodyFiles[$bi]
        $body = $bodyImages[$bodyPath]
        for ($hi = 0; $hi -lt $hairFiles.Count; $hi++) {
            $hairPath = $hairFiles[$hi]
            $hair = $hairImages[$hairPath]
            for ($oi = 0; $oi -lt $outfitFiles.Count; $oi++) {
                $outfitPath = $outfitFiles[$oi]
                $outfit = $outfitImages[$outfitPath]
                $slug = "b{0:D2}_h{1:D2}_o{2:D2}" -f ($bi + 1), ($hi + 1), ($oi + 1)

                $front = Compose-Front -Body $body -Hair $hair -Outfit $outfit
                $profile = New-ProfileVariant -Front $front
                $back = New-BackVariant -Front $front
                try {
                    Save-Png -Bitmap $front -Path (Join-Path $frontDir "$slug.png")
                    Save-Png -Bitmap $profile -Path (Join-Path $profileDir "$slug.png")
                    Save-Png -Bitmap $back -Path (Join-Path $backDir "$slug.png")
                } finally {
                    $front.Dispose()
                    $profile.Dispose()
                    $back.Dispose()
                }

                $count++
            }
        }
    }

    Write-Output "Combinaciones generadas: $count"
    Write-Output "Frontales: $frontDir"
    Write-Output "Perfiles: $profileDir"
    Write-Output "Espaldas: $backDir"
} finally {
    foreach ($img in $bodyImages.Values) { $img.Dispose() }
    foreach ($img in $hairImages.Values) { $img.Dispose() }
    foreach ($img in $outfitImages.Values) { $img.Dispose() }
}
