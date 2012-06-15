rm -rf bin
ant debug
adb uninstall org.umit.ns.mobile
adb install -r ./bin/ns-mobile-debug.apk

