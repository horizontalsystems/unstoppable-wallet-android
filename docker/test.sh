#!/bin/bash

downloadedApp="$1"
# make sure path is absolute
if ! [[ $downloadedApp =~ ^/.* ]]; then
  downloadedApp="$PWD/$downloadedApp"
fi
wsDocker="walletscrutiny/android:5"

set -x

dockerApktool() {
  targetFolder=$1
  app=$2
  targetFolderParent=$(dirname "$targetFolder")
  targetFolderBase=$(basename "$targetFolder")
  appFolder=$(dirname "$app")
  appFile=$(basename "$app")
  # Run apktool in a docker container so apktool doesn't need to be installed.
  # The folder with the apk file is mounted read only and only the output folder
  # is mounted with write permission.
  # If docker is running as root, change the owner of the output to the current
  # user. If that fails, ignore it.
  docker run \
    --rm \
    --volume $targetFolderParent:/tfp \
    --volume $appFolder:/af:ro \
    $wsDocker \
    sh -c "apktool d -o \"/tfp/$targetFolderBase\" \"/af/$appFile\"; chown $(id -u):$(id -g) -R /tfp/ || true"
  return $?
}

getSigner() {
  DIR=$(dirname "$1")
  BASE=$(basename "$1")
  s=$(
    docker run \
      --rm \
      --volume $DIR:/mnt:ro \
      --workdir /mnt \
      $wsDocker \
      apksigner verify --print-certs "$BASE" | grep "Signer #1 certificate SHA-256"  | awk '{print $6}' )
  echo $s
}

usage() {
  echo 'NAME
       test.sh - test if apk can be built from source

SYNOPSIS
       test.sh downloadedApp

DESCRIPTION
       This command tries to verify builds of apps that we verified before.

       downloadedApp  The apk file we want to test.'
}

if [ ! -f "$downloadedApp" ]; then
  echo "APK file not found!"
  echo
  usage
  exit 1
fi

apkHash=$(sha256sum "$downloadedApp" | awk '{print $1;}')
fromPlayFolder=/tmp/fromPlay$apkHash
rm -rf $fromPlayFolder
signer=$( getSigner "$downloadedApp" )
echo "Extracting APK content ..."
dockerApktool $fromPlayFolder "$downloadedApp" || exit 1
appId=$( cat $fromPlayFolder/AndroidManifest.xml | head -n 1 | sed 's/.*package=\"//g' | sed 's/\".*//g' )
versionName=$( cat $fromPlayFolder/apktool.yml | grep versionName | sed 's/.*\: //g' | sed "s/'//g" )
versionCode=$( cat $fromPlayFolder/apktool.yml | grep versionCode | sed 's/.*\: //g' | sed "s/'//g" )
fromPlayUnpacked=/tmp/fromPlay_"$appId"_"$versionCode"
workDir="/tmp/test$appId"
rm -rf $fromPlayUnpacked
mv $fromPlayFolder $fromPlayUnpacked

if [ -z $appId ]; then
  echo "appId could not be tetermined"
  exit 1
fi

if [ -z $versionName ]; then
  echo "versionName could not be determined"
  exit 1
fi

if [ -z $versionCode ]; then
  echo "versionCode could not be determined"
  exit 1
fi

echo
echo "Testing \"$downloadedApp\" ($appId version $versionName)"
echo

prepare() {
  echo "Testing $appId from $repo revision $tag ..."
  # cleanup
  sudo rm -rf /tmp/test$appId || exit 1
  # get uinque folder
  mkdir $workDir
  cd $workDir
  # clone
  echo "Trying to clone version $tag ..."
  git clone --quiet --branch "$tag" --depth 1 $repo app || exit 1
  cd app
}

result() {
  # collect results
  fromBuildUnpacked="/tmp/fromBuild_${appId}_$versionCode"
  rm -rf $fromBuildUnpacked
  dockerApktool $fromBuildUnpacked "$builtApk" || exit 1
  echo "Results:
appId:          $appId
signer:         $signer
apkVersionName: $versionName
apkVersionCode: $versionCode
apkHash:        $apkHash

Diff:
$( diff --brief --recursive $fromPlayUnpacked $fromBuildUnpacked )

Revision, tag (and its signature):
$( git tag -v "$tag" )

Run a full
diff --recursive $fromPlayUnpacked $fromBuildUnpacked
meld $fromPlayUnpacked $fromBuildUnpacked
for more details."
}

testUnstoppable() {
  repo=https://github.com/horizontalsystems/unstoppable-wallet-android
  tag=$versionName
  builtApk=$workDir/app/app/build/outputs/apk/release/app-release-unsigned.apk

  prepare

  # build
  docker run -it --volume $PWD:/mnt --workdir /mnt --rm $wsDocker bash -x -c \
      'apt update && DEBIAN_FRONTEND=noninteractive apt install openjdk-11-jdk --yes && ./gradlew clean :app:assembleRelease'

  # collect results
  result
}

case "$appId" in
  "io.horizontalsystems.bankwallet")
    testUnstoppable
    ;;
  *)
    echo "Unknown appId $appId"
    ;;
esac
