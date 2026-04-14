# Fix remaining Phase 2 items
$ErrorActionPreference = "Stop"
$root = "F:\Quantum\repositories\quantum-wallet-android"

# 1. Move remaining test files under app/src/test/java/io/horizontalsystems/
Write-Host "=== Moving remaining test directories ==="
$testBase = "app/src/test/java/io/horizontalsystems"
$testBaseFull = Join-Path $root $testBase
if (Test-Path $testBaseFull) {
    $dirs = Get-ChildItem $testBaseFull -Directory
    foreach ($d in $dirs) {
        # These are NOT external kits - they're app test files organized differently
        $src = "$testBase/$($d.Name)"
        $dst = "app/src/test/java/com/quantum/wallet/$($d.Name)"
        $dstFull = Join-Path $root $dst
        $dstParent = Split-Path $dstFull -Parent
        if (-not (Test-Path $dstParent)) {
            New-Item -ItemType Directory -Path $dstParent -Force | Out-Null
        }
        Write-Host "  Moving $src -> $dst"
        git mv (Join-Path $root $src) $dstFull
    }
    # Also move any loose files
    $looseFiles = Get-ChildItem $testBaseFull -File
    foreach ($f in $looseFiles) {
        $src = "$testBase/$($f.Name)"
        $dst = "app/src/test/java/com/quantum/wallet/$($f.Name)"
        Write-Host "  Moving $src -> $dst"
        git mv (Join-Path $root $src) (Join-Path $root $dst)
    }
}

# 2. Fix fully-qualified type references in .kt files (e.g., io.horizontalsystems.bankwallet.entities.Address)
Write-Host "`n=== Fixing fully-qualified type references ==="
$ktFiles = Get-ChildItem -Path $root -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch "\\build\\" }

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

$modifiedCount = 0
foreach ($file in $ktFiles) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    $original = $content
    
    # Replace ALL occurrences of internal packages (not just package/import lines)
    # Sort by longest first to avoid partial matches
    $sorted = $packageReplacements.Keys | Sort-Object { $_.Length } -Descending
    foreach ($old in $sorted) {
        $new = $packageReplacements[$old]
        $content = $content -replace [regex]::Escape($old), $new
    }
    
    if ($content -ne $original) {
        [System.IO.File]::WriteAllText($file.FullName, $content)
        $modifiedCount++
    }
}
Write-Host "  Modified $modifiedCount .kt files (pass 2)"

# 3. Clean up empty io/horizontalsystems dirs
Write-Host "`n=== Cleaning empty dirs ==="
$oldDirs = Get-ChildItem -Path $root -Recurse -Directory -Filter "horizontalsystems" | Where-Object {
    $_.FullName -match "\\io\\horizontalsystems$"
}
foreach ($d in $oldDirs) {
    $ioDir = Split-Path $d.FullName -Parent
    $items = Get-ChildItem $d.FullName -Recurse -File
    if ($items.Count -eq 0) {
        Write-Host "  Removing empty: $($d.FullName.Substring($root.Length + 1))"
        [System.IO.Directory]::Delete($d.FullName, $true)
        $ioItems = Get-ChildItem $ioDir -Recurse -File
        if ($ioItems.Count -eq 0) {
            Write-Host "  Removing empty: $($ioDir.Substring($root.Length + 1))"
            [System.IO.Directory]::Delete($ioDir, $true)
        }
    }
}

Write-Host "`nDone!"
