cd ~
echo "Cloning Repository"
git clone http://git.umitproject.org/ns-mobile.git
echo "Updating Project"
android update project --name ns-mobile --target android-8 --path ns-mobile
echo "Building Native"
ndk-build -C ns-mobile
cp ~/ns-mobile/libs/armeabi/scanner ~/ns-mobile/res/raw/archived/scanner
cd ~/ns-mobile/res/raw/archived
zip -r ../archive *
mv ~/ns-mobile/res/raw/archive.zip ~/ns-mobile/res/raw/archive
cd ~/ns-mobile
ant debug