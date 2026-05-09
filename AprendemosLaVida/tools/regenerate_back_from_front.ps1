param(
    [string]$FrontDir = "app/src/main/assets/avatar/generated_pose_variants_v1/front",
    [string]$BackDir = "app/src/main/assets/avatar/generated_pose_variants_v1/back"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Get-SafePixel {
    param(
        [System.Drawing.Bitmap]$Bmp,
        [int]$X,
        [int]$Y,
        [System.Drawing.Color]$Fallback
    )
    if ($X -lt 0 -or $Y -lt 0 -or $X -ge $Bmp.Width -or $Y -ge $Bmp.Height) { return $Fallback }
    $c = $Bmp.GetPixel($X, $Y)
    if ($c.A -eq 0) { return $Fallback }
    return $c
}

function Get-RegionAverage {
    param(
        [System.Drawing.Bitmap]$Bmp,
        [int]$X0,
        [int]$Y0,
        [int]$X1,
        [int]$Y1,
        [System.Drawing.Color]$Fallback
    )
    $sumR = 0; $sumG = 0; $sumB = 0; $count = 0
    for ($y = $Y0; $y -le $Y1; $y++) {
        for ($x = $X0; $x -le $X1; $x++) {
            if ($x -lt 0 -or $y -lt 0 -or $x -ge $Bmp.Width -or $y -ge $Bmp.Height) { continue }
            $c = $Bmp.GetPixel($x, $y)
            if ($c.A -lt 24) { continue }
            $sumR += $c.R; $sumG += $c.G; $sumB += $c.B
            $count++
        }
    }
    if ($count -eq 0) { return $Fallback }
    return [System.Drawing.Color]::FromArgb(255, [int]($sumR / $count), [int]($sumG / $count), [int]($sumB / $count))
}

$root = (Resolve-Path ".").Path
$frontPath = Join-Path $root $FrontDir
$backPath = Join-Path $root $BackDir

if (-not (Test-Path $frontPath)) { throw "No existe FrontDir: $frontPath" }
New-Item -ItemType Directory -Path $backPath -Force | Out-Null

$files = Get-ChildItem -Path $frontPath -File -Filter "*.png" | Sort-Object Name
if ($files.Count -eq 0) { throw "No hay PNG en $frontPath" }

$count = 0
foreach ($file in $files) {
    $front = [System.Drawing.Bitmap]::FromFile($file.FullName)
    try {
        $w = $front.Width
        $h = $front.Height
        $back = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($back)
        try {
            $g.Clear([System.Drawing.Color]::Transparent)
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

            # Base back look: mirrored + darkened.
            $attrs = New-Object System.Drawing.Imaging.ImageAttributes
            $cm = New-Object System.Drawing.Imaging.ColorMatrix
            $cm.Matrix00 = 0.82
            $cm.Matrix11 = 0.82
            $cm.Matrix22 = 0.82
            $cm.Matrix33 = 1.0
            $cm.Matrix44 = 1.0
            $attrs.SetColorMatrix($cm)

            $g.TranslateTransform($w, 0)
            $g.ScaleTransform(-1, 1)
            $dstRect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
            $g.DrawImage($front, $dstRect, 0, 0, $w, $h, [System.Drawing.GraphicsUnit]::Pixel, $attrs)
            $g.ResetTransform()

            # Infer key tones from source.
            $hairBase = Get-SafePixel -Bmp $front -X ([int]($w * 0.50)) -Y ([int]($h * 0.09)) -Fallback ([System.Drawing.Color]::FromArgb(255, 35, 28, 22))
            $skinBase = Get-SafePixel -Bmp $front -X ([int]($w * 0.36)) -Y ([int]($h * 0.28)) -Fallback ([System.Drawing.Color]::FromArgb(255, 170, 120, 85))
            $torsoBlend = Get-RegionAverage `
                -Bmp $back `
                -X0 ([int]($w * 0.34)) `
                -Y0 ([int]($h * 0.40)) `
                -X1 ([int]($w * 0.66)) `
                -Y1 ([int]($h * 0.70)) `
                -Fallback ([System.Drawing.Color]::FromArgb(255, 170, 170, 170))

            # Hair mass to hide frontal face details.
            $hairBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, [int]($hairBase.R * 0.88), [int]($hairBase.G * 0.88), [int]($hairBase.B * 0.88)))
            $hairShadow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(120, 0, 0, 0))
            $neckBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230, [int]($skinBase.R * 0.78), [int]($skinBase.G * 0.78), [int]($skinBase.B * 0.78)))
            $torsoBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(245, [int]($torsoBlend.R * 0.94), [int]($torsoBlend.G * 0.94), [int]($torsoBlend.B * 0.94)))
            $torsoShade = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(70, 0, 0, 0))
            try {
                $g.FillEllipse($hairBrush, [int]($w * 0.26), [int]($h * 0.04), [int]($w * 0.48), [int]($h * 0.36))
                $g.FillEllipse($hairShadow, [int]($w * 0.30), [int]($h * 0.18), [int]($w * 0.40), [int]($h * 0.20))
                $g.FillRectangle($neckBrush, [int]($w * 0.45), [int]($h * 0.33), [int]($w * 0.10), [int]($h * 0.06))
                # Flatten chest/front symbols with rear cloth block.
                $g.FillEllipse($torsoBrush, [int]($w * 0.30), [int]($h * 0.34), [int]($w * 0.40), [int]($h * 0.18))
                $g.FillRectangle($torsoBrush, [int]($w * 0.34), [int]($h * 0.42), [int]($w * 0.32), [int]($h * 0.30))
                $g.FillRectangle($torsoShade, [int]($w * 0.48), [int]($h * 0.42), [int]($w * 0.04), [int]($h * 0.30))
            } finally {
                $hairBrush.Dispose()
                $hairShadow.Dispose()
                $neckBrush.Dispose()
                $torsoBrush.Dispose()
                $torsoShade.Dispose()
                $attrs.Dispose()
            }
        } finally {
            $g.Dispose()
        }

        try {
            $outFile = Join-Path $backPath $file.Name
            $back.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
            $count++
        } finally {
            $back.Dispose()
        }
    } finally {
        $front.Dispose()
    }
}

Write-Output "Back regenerados: $count"
Write-Output "Destino: $backPath"
