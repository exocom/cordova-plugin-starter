#!/usr/bin/env bash

CORDOVA_PLATFORM="android"

cordova platform rm android && cordova platform add $CORDOVA_PLATFORM && cordova build
# TODO add git reset on platforms/android/.idea