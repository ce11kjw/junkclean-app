#!/bin/bash
set -e
SDK=/opt/android-sdk
BT=$SDK/build-tools/34.0.0
PLATFORM=$SDK/platforms/android-34/android.jar
cd /opt/junkclean-app

VER=$(grep -oP 'versionName="\K[^"]+' AndroidManifest.xml)
OUT=/opt/junkclean-app/out
rm -rf $OUT && mkdir -p $OUT/classes

echo "[1/6] aapt2 编译资源..."
$BT/aapt2 compile --dir res -o $OUT/res.zip

echo "[2/6] aapt2 链接..."
$BT/aapt2 link -o $OUT/base.apk -I $PLATFORM \
    --manifest AndroidManifest.xml -R $OUT/res.zip --auto-add-overlay

echo "[3/6] javac 编译..."
set +e
javac -source 8 -target 8 -bootclasspath $PLATFORM \
    -d $OUT/classes $(find src -name "*.java") 2>&1 \
    | grep -v "bootstrap class path" | tee $OUT/javac.log
set +e
JAVAC_FAIL=$(grep -cE "^[0-9]+ error" $OUT/javac.log)
JAVAC_FAIL=${JAVAC_FAIL:-0}
set -e
if [ "$JAVAC_FAIL" != "0" ]; then
    echo "❌ 编译失败，终止构建"
    grep -E "error:" $OUT/javac.log 2>/dev/null | head -20
    exit 1
fi

echo "[4/6] d8 dex..."
$BT/d8 --lib $PLATFORM --release --output $OUT $(find $OUT/classes -name "*.class")

echo "[5/6] 打包+对齐..."
cd $OUT && zip -j -q base.apk classes.dex
$BT/zipalign -f 4 base.apk aligned.apk

echo "[6/6] 签名..."
$BT/apksigner sign --ks /opt/junkclean-app/junkclean.keystore \
    --ks-pass pass:junkclean123 --key-pass pass:junkclean123 \
    --out JunkClean-v${VER}.apk aligned.apk

rm -f base.apk aligned.apk classes.dex res.zip
ls -lh $OUT/JunkClean-v${VER}.apk
echo "✅ 构建完成: $OUT/JunkClean-v${VER}.apk"
