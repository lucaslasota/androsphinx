#!/bin/sh

set -e

SPHINX=libsphinx
SODIUM=libsodium_headers
OUTDIR=app/src/main/jniLibs

compile_libsphinx_arch() {
	ARCH=$1
	TARGET=../../$OUTDIR/$ARCH
	cd goldilocks
	make clean
	make $4 android_$2
	cd ..
	cp ../../app/build/intermediates/merged_native_libs/debug/out/lib/$ARCH/libsodiumjni.so .
	make clean
	make CC=$3 SODIUM=../../$SODIUM android
	mkdir -p $TARGET
	cp libsphinx.so $TARGET
}

git submodule update --init --recursive --remote

rm -rf $SODIUM
mkdir $SODIUM
ln -s /usr/include/sodium* $SODIUM

cd $SPHINX/src

compile_libsphinx_arch "x86_64"      "x86_64"      "x86_64-linux-android21-clang"
compile_libsphinx_arch "x86"         "i686"          "i686-linux-android21-clang" "FIELD_ARCH=arch_32"
compile_libsphinx_arch "arm64-v8a"   "aarch64"    "aarch64-linux-android21-clang" "FIELD_ARCH=arch_ref64"
compile_libsphinx_arch "armeabi-v7a" "armv7a"  "armv7a-linux-androideabi21-clang" "FIELD_ARCH=arch_32"

cd ../..
rm -rf $SODIUM
