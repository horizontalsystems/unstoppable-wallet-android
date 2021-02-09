#!/bin/bash

function usage() {
    echo "Usage: ./build-apk.sh [REPO-TAG]  [FULL_PATH_TO_KEYSTORE] [KEYSTORE_PASSWORD]"
}

####################################

if [ "$#" -ne 3 ]; then
    echo "Error! Illegal number of args."
    usage
    exit 1
fi

if [ -f "$2" ]; then
    echo "Using keystore file: $2"
else
    echo "Error ! Keystore: $2 not exists."
    exit 1
fi

####################################
DOCKER_IMAGE="horizontalsystems/android-release-build"
GIT_REPO="https://github.com/horizontalsystems/unstoppable-wallet-android"
WORK_DIR="$PWD/app"
TAG=$1

KEYSTORE=$2
KEYSTORE_FILENAME="$(basename -- $KEYSTORE)"
KEYSTORE_PASSWORD=$3
BUILT_APK_FILE="app/app/build/outputs/apk/release/app-release-unsigned.apk"
####################################

function init() {
    echo "Removing folder:${WORK_DIR}  ... "

    rm -Rf $WORK_DIR
}

function gitClone () {
    echo "Trying to clone version ${TAG} ..."
    git clone --quiet --branch "${TAG}" --depth 1 ${GIT_REPO} app || exit 1
}

function buildApk () {
    echo "Starting docker container  ${DOCKER_IMAGE} ..."
    echo "Building apk file ..."

    docker run -it --volume $PWD/app:/mnt --workdir /mnt ${DOCKER_IMAGE} bash -x -c \
      './gradlew clean :app:assembleRelease'

}

function signApk () {
    echo "Signing apk with  ${KEYSTORE}  ... "

    cp $KEYSTORE $PWD

    response=$(docker run -it --volume $PWD:/mnt --workdir /mnt ${DOCKER_IMAGE} bash -x -c \
      "apksigner sign --ks ${KEYSTORE_FILENAME} --ks-pass pass:${KEYSTORE_PASSWORD} ${BUILT_APK_FILE}")

    echo $response
    rm "${PWD}/${KEYSTORE_FILENAME}"
}

function end () {
    cp $BUILT_APK_FILE "${PWD}/app-release-signed.apk"
}

#############################################
echo "Starting build revision:${TAG}  ... "

echo "--------------------"
init

echo "--------------------"
gitClone

echo "--------------------"
buildApk

echo "--------------------"
signApk

echo "--------------------"
end

echo "--------------------"
echo "Successfully created apk !!! "
echo "--------END --------"



