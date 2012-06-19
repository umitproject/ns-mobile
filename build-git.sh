echo "Updating Project"
android update project --name ns-mobile --target android-8 --path .

echo "Building Native"
ndk-build

echo "Arranging binaries"
cp ./libs/armeabi/scanner ./res/raw/archived/scanner
cd ./res/raw/archived
zip -r archive *
mv archive.zip ../archive
cd ../../../

#echo "Building Android Project"
#ant debug
