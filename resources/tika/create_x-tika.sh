#! /bin/sh

# don't change these
PLATFORM="all"
PACKAGENAME="$( basename $0 | cut -d_ -f2 | cut -d. -f1 )"
VERSIONLINE=$( grep "Version" "$PACKAGENAME/DEBIAN/control" )
VERSION=$( echo "$VERSIONLINE" | cut -d: -f2 | awk "{ print $1 }" | tr -d " ")
NUM1=$( echo "$VERSION" | cut -d. -f1 | awk "{ print $1 }" | tr -d " " )
NUM2=$( echo "$VERSION" | cut -d. -f2 | awk "{ print $1 }" )
NUM3=$( echo "$VERSION" | cut -d. -f3 | awk "{ print $1 }" )
OLDPKG="$PACKAGENAME""_*_""$PLATFORM"".deb"

# delete old packages
rm -f $OLDPKG

# set execution rights for all files matching "DEBIAN/p"
# which should only be the maintainer scripts
for file in $( find "$PACKAGENAME/DEBIAN/" | grep "$PACKAGENAME/DEBIAN/p" ); do

	chmod +x $file
done

# create new strings
NUM3=$( expr $NUM3 + 1 )
NEWVERSION="$NUM1"".""$NUM2"".""$NUM3"
NEWVERSIONLINE="Version: $NEWVERSION"
NEWPKG="$PACKAGENAME""_""$NEWVERSION""_""$PLATFORM"".deb"

# increment 'Version' field in the control file
sed -i "s/$VERSIONLINE/$NEWVERSIONLINE/g" "$PACKAGENAME/DEBIAN/control"

# calculate and change 'Installed-Size' field in the control file
DEBSIZE=$( du -c "$PACKAGENAME" | tail -n 1 | awk '{ print $1 }' )
sed -i "/Installed-Size: /c\Installed-Size: $DEBSIZE" "$PACKAGENAME/DEBIAN/control"

# create new package
dpkg -b "$PACKAGENAME/" "$NEWPKG"

exit 0
