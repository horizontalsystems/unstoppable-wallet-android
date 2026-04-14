# Rebrand fastlane metadata files and documentation
$root = "f:\Quantum\repositories\quantum-wallet-android"

# Fix title.txt files
$titles = Get-ChildItem -Path "$root\fastlane\metadata" -Filter "title.txt" -Recurse
foreach ($t in $titles) {
    $c = [System.IO.File]::ReadAllText($t.FullName)
    $o = $c
    $c = $c -replace 'Unstoppable Crypto Wallet', 'Quantum Crypto Wallet'
    $c = $c -replace 'Carteira Unstoppable', 'Quantum Wallet'
    $c = $c -replace 'Unstoppable Wallet', 'Quantum Wallet'
    $c = $c -replace 'Unstoppable', 'Quantum Wallet'
    if ($c -ne $o) {
        [System.IO.File]::WriteAllText($t.FullName, $c)
        Write-Host "Title: $($t.FullName)"
    }
}

# Fix full_description.txt and short_description.txt files
$descs = Get-ChildItem -Path "$root\fastlane\metadata" -Filter "*.txt" -Recurse | Where-Object { $_.Name -match 'description' }
foreach ($d in $descs) {
    $c = [System.IO.File]::ReadAllText($d.FullName)
    $o = $c
    $c = $c -replace 'Wallet Unstoppable', 'Quantum Wallet'
    $c = $c -replace 'Carteira Unstoppable', 'Quantum Wallet'
    $c = $c -replace 'Unstoppable wallet', 'Quantum Wallet'
    $c = $c -replace 'Unstoppable Wallet', 'Quantum Wallet'
    $c = $c -replace 'Unstoppable cüzdan', 'Quantum Wallet'
    $c = $c -replace 'Be Unstoppable!', 'Be Quantum!'
    $c = $c -replace 'Sei Unstoppable!', 'Quantum Wallet!'
    $c = $c -replace 'Seja Unstoppable!', 'Quantum Wallet!'
    $c = $c -replace 'Unstoppable', 'Quantum Wallet'
    if ($c -ne $o) {
        [System.IO.File]::WriteAllText($d.FullName, $c)
        Write-Host "Desc: $($d.FullName)"
    }
}

Write-Host "Done"
