echo "Updating Project"
android update project --name ns-mobile --target android-8 --path .

echo "Building Native"
ndk-build

echo "Arranging binaries"
cp ./libs/armeabi/scanner ./res/raw/archived/scanner
zip -r ./res/raw/archive ./res/raw/archived/*
mv ./res/raw/archive.zip ./res/raw/archive

echo "Building Android Project"
ant debug
