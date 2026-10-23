#!/usr/bin/env bash
# Guarded taps on the test phone: no call going on, the expected app in front. Usage: tap.sh <pkg> x y
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
pkg=$1; shift
c=$("$ADB" shell dumpsys telephony.registry | grep -m1 mCallState | tr -d ' \r')
f=$("$ADB" shell dumpsys window | grep -m1 mCurrentFocus)
case "$f" in *"$pkg"*) ;; *) echo "ABORT: focus is $f"; exit 1;; esac
[ "$c" = "mCallState=0" ] || { echo "ABORT: call state $c"; exit 1; }
"$ADB" shell input tap "$1" "$2"
