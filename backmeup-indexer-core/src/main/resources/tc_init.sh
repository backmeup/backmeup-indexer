#!/bin/sh

BASEDIR="$(dirname $0)"
DEVICE="$1"
MOUNTPOINT="$2"
PASSWORD="$3"
KEYFILE="$4"



# make sure to have "/dev/sdb" (not "/dev/sdb1")
#DEVICE="${DEVICE:0:8}" the bash way does not work in dash -.-
DEVICE="$(echo "$DEVICE" | awk '{print substr($1,0,9)}')"


if [ -z "$KEYFILE" ]
then
	message="$($tc_cmd -c --non-interactive --quick --filesystem=none --encryption=AES --hash=RIPEMD-160 -p "$PASSWORD" "$DEVICE")"
	result="$?"
else
	message="$($tc_cmd -c --non-interactive --quick --filesystem=none --encryption=AES --hash=RIPEMD-160 -p "$PASSWORD" -k "$KEYFILE" "$DEVICE")"
	result="$?"
fi



if [ -z "$KEYFILE" ]
then
	message="$message\n$($tc_cmd --non-interactive --filesystem=none -p "$PASSWORD" "$DEVICE")"
	result="$?"
else
	message="$message\n$($tc_cmd --non-interactive --filesystem=none -p "$PASSWORD" -k "$KEYFILE" "$DEVICE")"
	result="$?"
fi
	
if [ "$result" != "0" ]
then
	exit 1
fi


TC_DEVICE=$(truecrypt -l | awk '{print $3}')

message="$message\n$(mkfs.ntfs --quick "$TC_DEVICE")"
result="$?"

if [ "$result" != "0" ]
then
	sendErrorNotification "Filesystem creation failed"
	exit 1
fi