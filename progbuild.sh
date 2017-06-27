#!/bin/bash

export ANDROID_HOME="/run/media/vitya/11608a3a-59c8-4165-9684-d0b7f7b57d5e/android-sdk-linux"
bld="./gradlew --offline assembleDebug"
apk="./app/build/outputs/apk/app-debug.apk"

if [ "$1" = "upload" ]; then
	scp $apk ii-net.tk:/home/vitya/ii-net.tk/public_html/ii/files/
	exit
fi

if [ "$1" = "install-run" ]; then
	adb install -r $apk && adb shell am start vit01.idecmobile/vit01.idecmobile.MainActivity && adb logcat
	exit
fi

if [ "$1" = "build" ]; then
	$bld
	exit
fi

if [ "$1" = "rundebug" ]; then
	adb shell am start vit01.idecmobile/vit01.idecmobile.MainActivity && adb logcat
	exit
fi

$bld && adb install -r $apk && adb shell am start vit01.idecmobile/vit01.idecmobile.MainActivity
