android update project --name ns-mobile --target android-10 --path .
echo "Building Native"
ndk-build
cp ./libs/armeabi/scanner ./res/raw/archived/scanner
cd ./res/raw/archived
rm archive.zip
zip -r archive *
mv archive.zip ../archive
cd ../../../
echo "Building Android Project"
ant debug
