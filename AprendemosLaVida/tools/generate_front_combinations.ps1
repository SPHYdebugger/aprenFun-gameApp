param(
    [string]$AssetsDir = "app/src/main/assets/avatar",
    [string]$FrontDir = "app/src/main/assets/avatar/generated_pose_variants_v1/front",
    [switch]$IncludeBoy = $true,
    [switch]$IncludeGirl = $true
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Compose-Front {
    param(
        [System.Drawing.Image]$Body,
        [System.Drawing.Image]$Hair,
        [System.Drawing.Image]$Outfit
    )
    $w = $Body.Width
    $h = $Body.Height
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
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

$root = (Resolve-Path ".").Path
$assetsPath = Join-Path $root $AssetsDir
$frontPath = Join-Path $root $FrontDir
New-Item -ItemType Directory -Path $frontPath -Force | Out-Null

# Clean previous files in front folder.
Get-ChildItem -Path $frontPath -File -Filter "*.png" | Remove-Item -Force

$bodyFiles = @(
    "base_body_skin_01.png",
    "base_body_skin_03.png",
    "base_body_skin_05.png"
) | ForEach-Object { Join-Path $assetsPath $_ }

if (-not $IncludeBoy -and -not $IncludeGirl) {
    throw "Debes activar al menos uno: -IncludeBoy o -IncludeGirl."
}

$hairBoyFiles = @(
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

$hairGirlFiles = @(
    "hair_girl_01_c0.png",
    "hair_girl_01_c1.png",
    "hair_girl_01_c2.png",
    "hair_girl_02_c0.png",
    "hair_girl_02_c1.png",
    "hair_girl_02_c2.png",
    "hair_girl_03_c0.png",
    "hair_girl_03_c1.png",
    "hair_girl_03_c2.png"
) | ForEach-Object { Join-Path $assetsPath $_ }

$outfitBoyFiles = 1..7 | ForEach-Object {
    Join-Path $assetsPath ("outfit_boy_{0:D2}.png" -f $_)
}

$outfitGirlFiles = 1..7 | ForEach-Object {
    Join-Path $assetsPath ("outfit_girl_{0:D2}.png" -f $_)
}

foreach ($path in ($bodyFiles + $hairBoyFiles + $hairGirlFiles + $outfitBoyFiles + $outfitGirlFiles)) {
    if (-not (Test-Path $path)) {
        throw "Falta asset requerido: $path"
    }
}

$bodyImages = @{}
$hairImages = @{} # cached by full path
$outfitImages = @{} # cached by full path
try {
    foreach ($path in $bodyFiles) { $bodyImages[$path] = [System.Drawing.Image]::FromFile($path) }
    foreach ($path in $hairBoyFiles) { $hairImages[$path] = [System.Drawing.Image]::FromFile($path) }
    foreach ($path in $hairGirlFiles) { $hairImages[$path] = [System.Drawing.Image]::FromFile($path) }
    foreach ($path in $outfitBoyFiles) { $outfitImages[$path] = [System.Drawing.Image]::FromFile($path) }
    foreach ($path in $outfitGirlFiles) { $outfitImages[$path] = [System.Drawing.Image]::FromFile($path) }

    $count = 0
    $genders = @()
    if ($IncludeBoy) {
        $genders += [PSCustomObject]@{
            Prefix = "boy"
            HairFiles = $hairBoyFiles
            OutfitFiles = $outfitBoyFiles
        }
    }
    if ($IncludeGirl) {
        $genders += [PSCustomObject]@{
            Prefix = "girl"
            HairFiles = $hairGirlFiles
            OutfitFiles = $outfitGirlFiles
        }
    }

    foreach ($gender in $genders) {
        for ($bi = 0; $bi -lt $bodyFiles.Count; $bi++) {
            $bodyPath = $bodyFiles[$bi]
            $body = $bodyImages[$bodyPath]
            for ($hi = 0; $hi -lt $gender.HairFiles.Count; $hi++) {
                $hairPath = $gender.HairFiles[$hi]
                $hair = $hairImages[$hairPath]
                for ($oi = 0; $oi -lt $gender.OutfitFiles.Count; $oi++) {
                    $outfitPath = $gender.OutfitFiles[$oi]
                    $outfit = $outfitImages[$outfitPath]
                    $slug = "{0}_b{1:D2}_h{2:D2}_o{3:D2}" -f $gender.Prefix, ($bi + 1), ($hi + 1), ($oi + 1)

                    $front = Compose-Front -Body $body -Hair $hair -Outfit $outfit
                    try {
                        $front.Save((Join-Path $frontPath "$slug.png"), [System.Drawing.Imaging.ImageFormat]::Png)
                        $count++
                    } finally {
                        $front.Dispose()
                    }
                }
            }
        }
    }

    $expected = (($IncludeBoy ? 1 : 0) + ($IncludeGirl ? 1 : 0)) * 189
    if ($count -ne $expected) {
        throw "Se generaron $count archivos y deben ser $expected."
    }

    # Exact duplicate validation by SHA-256.
    $dupeGroups = @(Get-ChildItem -Path $frontPath -File -Filter "*.png" |
        Get-FileHash -Algorithm SHA256 |
        Group-Object Hash |
        Where-Object { $_.Count -gt 1 })
    if ($dupeGroups.Count -gt 0) {
        $summary = ($dupeGroups | ForEach-Object { "$($_.Count)x$($_.Name.Substring(0, 10))..." }) -join ", "
        throw "Hay PNG repetidos por hash: $summary"
    }

    Write-Output "Frontales generados: $count"
    Write-Output "Sin duplicados por hash SHA-256."
    Write-Output "Destino: $frontPath"
} finally {
    foreach ($img in $bodyImages.Values) { $img.Dispose() }
    foreach ($img in $hairImages.Values) { $img.Dispose() }
    foreach ($img in $outfitImages.Values) { $img.Dispose() }
}
