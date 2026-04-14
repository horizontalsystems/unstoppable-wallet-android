# Rebrand XML files: Update io.horizontalsystems references in XML files
$ErrorActionPreference = "Stop"
$root = "F:\Quantum\repositories\quantum-wallet-android"

# Only replace internal package references in XML
$xmlReplacements = @{
    "io.horizontalsystems.bankwallet" = "com.quantum.wallet.bankwallet"
    "io.horizontalsystems.core" = "com.quantum.wallet.core"
    "io.horizontalsystems.chartview" = "com.quantum.wallet.chartview"
    "io.horizontalsystems.icons" = "com.quantum.wallet.icons"
    "io.horizontalsystems.subscriptions.core" = "com.quantum.wallet.subscriptions.core"
    "io.horizontalsystems.subscriptions.dev" = "com.quantum.wallet.subscriptions.dev"
    "io.horizontalsystems.subscriptions.fdroid" = "com.quantum.wallet.subscriptions.fdroid"
    "io.horizontalsystems.subscriptions.googleplay" = "com.quantum.wallet.subscriptions.googleplay"
}

# Process XML files
$xmlFiles = Get-ChildItem -Path $root -Recurse -Include "*.xml" | Where-Object { $_.FullName -notmatch "\\build\\" }
$modifiedCount = 0

foreach ($file in $xmlFiles) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    $original = $content
    
    foreach ($old in $xmlReplacements.Keys) {
        $content = $content -replace [regex]::Escape($old), $xmlReplacements[$old]
    }
    
    if ($content -ne $original) {
        [System.IO.File]::WriteAllText($file.FullName, $content)
        $modifiedCount++
        Write-Host "  Updated: $($file.FullName.Substring($root.Length + 1))"
    }
}

Write-Host "`nModified $modifiedCount XML files"
