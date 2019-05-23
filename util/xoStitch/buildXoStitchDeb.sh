#!/usr/bin/env bash
set -e

mkdir -p target/debian/DEBIAN
mkdir -p target/debian/usr/
mkdir -p target/debian/usr/share/doc/xoStitch
version=$(grep version target/maven-archiver/pom.properties | cut -d= -f 2)
cat << 'END' > target/debian/DEBIAN/control 
Package: xoStitch
Version: __version__
Section: web
Priority: optional
Architecture: all
Maintainer: Paul Ruth
Description: xoStitch is a tool used to create stitchport-to-stitchport 
 circuits across ExoGENI including special handling for facilities like Chameleon
 .
 .
END
sed -i "s/__version__/${version}/g" target/debian/DEBIAN/control
cp -R target/appassembler/bin target/debian/usr/
cp -R target/appassembler/lib target/debian/usr/
dpkg --build target/debian
mv target/debian.deb target/xoStitch.deb
rm -rf target/debian
