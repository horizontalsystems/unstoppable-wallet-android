# Rebrand script: Move directories and update package/import declarations
# Phase 2 - Packages

$ErrorActionPreference = "Stop"
$root = "F:\Quantum\repositories\quantum-wallet-android"

# Define directory moves: [source_relative, dest_relative]
$moves = @(
    @("app\src\main\java\io\horizontalsystems\bankwallet", "app\src\main\java\com\quantum\wallet\bankwallet"),
    @("app\src\test\java\io\horizontalsystems\bankwallet", "app\src\test\java\com\quantum\wallet\bankwallet"),
    @("app\src\test\java\io\horizontalsystems\chartview", "app\src\test\java\com\quantum\wallet\chartview"),
    @("app\src\androidTest\java\io\horizontalsystems\bankwallet", "app\src\androidTest\java\com\quantum\wallet\bankwallet"),
    @("core\src\main\java\io\horizontalsystems\core", "core\src\main\java\com\quantum\wallet\core"),
    @("core\src\test\java\io\horizontalsystems\core", "core\src\test\java\com\quantum\wallet\core"),
    @("components\chartview\src\main\java\io\horizontalsystems\chartview", "components\chartview\src\main\java\com\quantum\wallet\chartview"),
    @("components\icons\src\main\java\io\horizontalsystems\icons", "components\icons\src\main\java\com\quantum\wallet\icons"),
    @("subscriptions-core\src\main\java\io\horizontalsystems\subscriptions\core", "subscriptions-core\src\main\java\com\quantum\wallet\subscriptions\core"),
    @("subscriptions-core\src\test\java\io\horizontalsystems\subscriptions\core", "subscriptions-core\src\test\java\com\quantum\wallet\subscriptions\core"),
    @("subscriptions-core\src\androidTest\java\io\horizontalsystems\subscriptions\core", "subscriptions-core\src\androidTest\java\com\quantum\wallet\subscriptions\core"),
    @("subscriptions-dev\src\main\java\io\horizontalsystems\subscriptions\dev", "subscriptions-dev\src\main\java\com\quantum\wallet\subscriptions\dev"),
    @("subscriptions-dev\src\test\java\io\horizontalsystems\subscriptions\dev", "subscriptions-dev\src\test\java\com\quantum\wallet\subscriptions\dev"),
    @("subscriptions-dev\src\androidTest\java\io\horizontalsystems\subscriptions\dev", "subscriptions-dev\src\androidTest\java\com\quantum\wallet\subscriptions\dev"),
    @("subscriptions-fdroid\src\main\java\io\horizontalsystems\subscriptions\fdroid", "subscriptions-fdroid\src\main\java\com\quantum\wallet\subscriptions\fdroid"),
    @("subscriptions-fdroid\src\test\java\io\horizontalsystems\subscriptions\fdroid", "subscriptions-fdroid\src\test\java\com\quantum\wallet\subscriptions\fdroid"),
    @("subscriptions-fdroid\src\androidTest\java\io\horizontalsystems\subscriptions\fdroid", "subscriptions-fdroid\src\androidTest\java\com\quantum\wallet\subscriptions\fdroid"),
    @("subscriptions-google-play\src\main\java\io\horizontalsystems\subscriptions\googleplay", "subscriptions-google-play\src\main\java\com\quantum\wallet\subscriptions\googleplay"),
    @("subscriptions-google-play\src\test\java\io\horizontalsystems\subscriptions\googleplay", "subscriptions-google-play\src\test\java\com\quantum\wallet\subscriptions\googleplay"),
    @("subscriptions-google-play\src\androidTest\java\io\horizontalsystems\subscriptions\googleplay", "subscriptions-google-play\src\androidTest\java\com\quantum\wallet\subscriptions\googleplay")
)

Write-Host "=== Step 1: Moving directories ==="
foreach ($move in $moves) {
    $src = Join-Path $root $move[0]
    $dst = Join-Path $root $move[1]
    if (Test-Path $src) {
        $dstParent = Split-Path $dst -Parent
        if (-not (Test-Path $dstParent)) {
            New-Item -ItemType Directory -Path $dstParent -Force | Out-Null
        }
        # Use git mv for proper tracking
        $gitSrc = $move[0] -replace '\\', '/'
        $gitDst = $move[1] -replace '\\', '/'
        Write-Host "  Moving $gitSrc -> $gitDst"
        git mv $src $dst
    } else {
        Write-Host "  SKIP (not found): $($move[0])"
    }
}

# Clean up empty io/horizontalsystems directories
Write-Host "`n=== Step 2: Cleaning empty old directories ==="
$oldDirs = Get-ChildItem -Path $root -Recurse -Directory -Filter "horizontalsystems" | Where-Object {
    $_.FullName -match "\\io\\horizontalsystems$"
}
foreach ($d in $oldDirs) {
    $ioDir = Split-Path $d.FullName -Parent
    # Check if horizontalsystems dir is empty
    $items = Get-ChildItem $d.FullName -Recurse -File
    if ($items.Count -eq 0) {
        Write-Host "  Removing empty: $($d.FullName)"
        [System.IO.Directory]::Delete($d.FullName, $true)
        # Also remove io dir if empty
        $ioItems = Get-ChildItem $ioDir -Recurse -File
        if ($ioItems.Count -eq 0) {
            Write-Host "  Removing empty: $ioDir"
            [System.IO.Directory]::Delete($ioDir, $true)
        }
    }
}

Write-Host "`n=== Step 3: Updating package declarations and imports in .kt files ==="

# Package replacement map (only internal packages)
$packageReplacements = @{
    "io.horizontalsystems.bankwallet" = "com.quantum.wallet.bankwallet"
    "io.horizontalsystems.core" = "com.quantum.wallet.core"
    "io.horizontalsystems.chartview" = "com.quantum.wallet.chartview"
    "io.horizontalsystems.icons" = "com.quantum.wallet.icons"
    "io.horizontalsystems.subscriptions.core" = "com.quantum.wallet.subscriptions.core"
    "io.horizontalsystems.subscriptions.dev" = "com.quantum.wallet.subscriptions.dev"
    "io.horizontalsystems.subscriptions.fdroid" = "com.quantum.wallet.subscriptions.fdroid"
    "io.horizontalsystems.subscriptions.googleplay" = "com.quantum.wallet.subscriptions.googleplay"
}

# External packages that must NOT be renamed
$externalPrefixes = @(
    "io.horizontalsystems.bitcoinkit",
    "io.horizontalsystems.bitcoincash",
    "io.horizontalsystems.litecoinkit",
    "io.horizontalsystems.dashkit",
    "io.horizontalsystems.ethereumkit",
    "io.horizontalsystems.erc20kit",
    "io.horizontalsystems.uniswapkit",
    "io.horizontalsystems.oneinchkit",
    "io.horizontalsystems.nftkit",
    "io.horizontalsystems.solanakit",
    "io.horizontalsystems.tronkit",
    "io.horizontalsystems.tonkit",
    "io.horizontalsystems.zcashbinancekt",
    "io.horizontalsystems.monerokit",
    "io.horizontalsystems.stellarkit",
    "io.horizontalsystems.marketkit",
    "io.horizontalsystems.feeratekit",
    "io.horizontalsystems.hdwalletkit",
    "io.horizontalsystems.pin"
)

function Is-ExternalImport {
    param([string]$importLine)
    foreach ($prefix in $externalPrefixes) {
        if ($importLine -match [regex]::Escape($prefix)) {
            return $true
        }
    }
    return $false
}

# Process all .kt files
$ktFiles = Get-ChildItem -Path $root -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch "\\build\\" }
$modifiedCount = 0

foreach ($file in $ktFiles) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    $original = $content
    
    foreach ($old in $packageReplacements.Keys) {
        $new = $packageReplacements[$old]
        $escapedOld = [regex]::Escape($old)
        
        # Replace package declarations: "package io.horizontalsystems.X" -> "package com.quantum.wallet.X"
        $content = $content -replace "(?m)^(package\s+)$escapedOld\b", "`${1}$new"
        
        # Replace import statements - but only for internal packages
        # We need to be careful: io.horizontalsystems.core should match but io.horizontalsystems.bitcoinkit should not
        $content = $content -replace "(?m)^(import\s+)$escapedOld\b", "`${1}$new"
    }
    
    if ($content -ne $original) {
        [System.IO.File]::WriteAllText($file.FullName, $content)
        $modifiedCount++
    }
}

Write-Host "  Modified $modifiedCount .kt files"

Write-Host "`nDone!"
