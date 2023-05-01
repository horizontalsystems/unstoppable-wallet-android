# Unstoppable Wallet Release

This document describes the release process for `Unstoppable` app.

### 1. Prepare dependent libraries

#### 1.1. Update Checkpoints

* `BitcoinKit`
* `BitcoinCashKit`
* `LitecoinKit`
* `DashKit`

#### 1.2. Update coins dump in `MarketKit`

Initial coins dump `json` file should be updated to latest state of backend.

### 2. Update URL for Guides and FAQ

* In case there are changes in Guides and FAQ repositories, update their URL's by new tags.

### 3. Update README file

* Check and update 'Supported Android Versions' section if needed

### 4. Transfer Code to Production Branch

Merge `version` branch into `master` branch.

### 5. Prepare New Development Branch

* Create new `version` branch.

```
$ git branch version/0.1
```

* Increment version code.
* Increase version name.

### 6. Set repository tag

* Create tag for current version.

### 7. Build apk file

#### 7.1 Build apk file via Docker.

You will find a bash script located at `[Wallet-Project-Path]/docker/build-apk.sh`
1. Create and go to temporary folder for APK output
2. Run command:
```
./build-apk.sh [REPO-TAG] [FULL_PATH_TO_KEYSTORE] [KEYSTORE_PASSWORD]
```
Where:<br>
REPO-TAG: Repository tag from which you want to build APK<br>
FULL_PATH_TO_KEYSTORE: Full Location of the keystore file<br>
KEYSTORE_PASSWORD: Keystore password<br>

Example:
```
./build-apk.sh 0.18.0 ~/Documents/Keystore/Apk_HorSys/horsys Keystore_Psw
```

#### 7.2 Verify apk file is reproducible from source code

Run command:

```
./test.sh [APK-FILE-NAME]
```
Where:<br>
`test.sh` bash script located at `[Wallet-Project-Path]/docker`<br>
APK-FILE-NAME: Name of the apk file<br>

### 8. Upload Build to Google Play

* Upload apk to `Google Play Console`.

### 9. Create Release in GitHub Repository

* Create new `Release`, add changelog and upload apk file. Make note in changelog if the 'Supported Android Versions' was changed
* Compute the sha256 and md5 checksums of the apk file and attach them as separate files, named unstoppable-wallet-[version].apk.sha256 and unstoppable-wallet-[version].apk.md5 respectively, under the 'Assets' section.

### 10. Make sure Unstoppable Wallet is 'Reproducible' in [WalletScrutiny](https://walletscrutiny.com/android/io.horizontalsystems.bankwallet/)

* After apk is uploaded to Google Play make sure that new version of Unstoppable Wallet is 'Reproducible' in WalletScrutiny.