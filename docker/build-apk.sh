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
DOCKER_IMAGE="hsdao/android-release-build:java-17-update"
GIT_REPO="https://github.com/horizontalsystems/unstoppable-wallet-android"
WORK_DIR="$(mktemp -d)"
TAG=$1

KEYSTORE=$2
KEYSTORE_FILENAME="$(basename -- $KEYSTORE)"
KEYSTORE_PASSWORD=$3
BUILT_APK_FILE="app/build/outputs/apk/base/release/app-base-release.apk"
BUILT_APK_FILE_FDROID="app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk"
####################################

function init() {
    echo "DOCKER_IMAGE="${DOCKER_IMAGE}""
    echo "GIT_REPO="${GIT_REPO}""
    echo "WORK_DIR="${WORK_DIR}""
    echo "TAG="${TAG}""
    echo "BUILT_APK_FILE="${BUILT_APK_FILE}""
    echo "BUILT_APK_FILE_FDROID="${BUILT_APK_FILE_FDROID}""
}

function gitClone () {
    echo "Trying to clone version ${TAG} ..."
    git clone --quiet --branch "${TAG}" --depth 1 ${GIT_REPO} "${WORK_DIR}" || exit 1
}

function buildApk () {
    echo "Starting docker container  ${DOCKER_IMAGE} ..."
    echo "Building apk file ..."

    docker run -it --volume "${WORK_DIR}:/mnt" --workdir /mnt ${DOCKER_IMAGE} bash -x -c \
      './gradlew :app:assembleBaseRelease --no-daemon'
    docker run -it --volume "${WORK_DIR}:/mnt" --workdir /mnt ${DOCKER_IMAGE} bash -x -c \
      './gradlew :app:assembleFdroidRelease --no-daemon'
}

function signApk () {
    echo "Signing apk with  ${KEYSTORE}  ... "

    cp $KEYSTORE "${WORK_DIR}"

    response=$(docker run -it --volume "${WORK_DIR}:/mnt" --workdir /mnt ${DOCKER_IMAGE} bash -x -c \
      "apksigner sign --ks ${KEYSTORE_FILENAME} --ks-pass pass:${KEYSTORE_PASSWORD} ${BUILT_APK_FILE}")

    echo $response

    response2=$(docker run -it --volume "${WORK_DIR}:/mnt" --workdir /mnt ${DOCKER_IMAGE} bash -x -c \
      "apksigner sign --ks ${KEYSTORE_FILENAME} --ks-pass pass:${KEYSTORE_PASSWORD} ${BUILT_APK_FILE_FDROID}")

    echo $response2

    rm "${WORK_DIR}/${KEYSTORE_FILENAME}"
}

function end () {
    cp "${WORK_DIR}/${BUILT_APK_FILE}" "${PWD}/unstoppable_wallet_google_play_${TAG}.apk"
    cp "${WORK_DIR}/${BUILT_APK_FILE_FDROID}" "${PWD}/unstoppable_wallet_github_${TAG}.apk"
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
