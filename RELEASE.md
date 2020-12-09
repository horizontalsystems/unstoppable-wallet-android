# Unstoppable Wallet Release

This document describes the release process for `Unstoppable` app.

### 1. Update Checkpoints in Kits

* `BitcoinKit`
* `BitcoinCashKit`
* `LitecoinKit`
* `DashKit`

### 2. Transfer Code to Production Branch

Merge `version` branch into `master` branch.

### 3. Prepare New Development Branch

* Create new `version` branch.

```
$ git branch version/0.1
```

* Increment version code.
* Increase version name.

### 4. Set repository tag

* Create tag for current version.

### 5. Build apk file

* Build apk file via Docker.

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

### 6. Upload Build to Google Play

* Upload apk to `Google Play Console`.

### 7. Create Release in GitHub Repository

* Create new `Release`, add changelog and upload apk file.
