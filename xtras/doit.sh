#!/bin/bash

# Get the fully qualified path to the script
case $0 in
    /*)
        SCRIPT="$0"
        ;;
    *)
        PWD=`pwd`
        SCRIPT="$PWD/$0"
        ;;
esac

# Change spaces to ":" so the tokens can be parsed.
SCRIPT=`echo $SCRIPT | sed -e 's; ;:;g'`
# Get the real path to this script, resolving any symbolic links
TOKENS=`echo $SCRIPT | sed -e 's;/; ;g'`
_SCRIPTDIR=
for C in $TOKENS; do
    _SCRIPTDIR="$_SCRIPTDIR/$C"
    while [ -h "$_SCRIPTDIR" ] ; do
        LS="`ls -ld "$_SCRIPTDIR"`"
        LINK="`expr "$LS" : '.*-> \(.*\)$'`"
        if expr "$LINK" : '/.*' > /dev/null; then
            _SCRIPTDIR="$LINK"
        else
            _SCRIPTDIR="`dirname "$_SCRIPTDIR"`""/$LINK"
        fi
    done
done
# Change ":" chars back to spaces.
_SCRIPTDIR=`echo $_SCRIPTDIR | sed -e 's;:; ;g'`
_SCRIPTDIR="`dirname "$_SCRIPTDIR"`"

#strip trailing dot
if [ "`basename "$_SCRIPTDIR"`" = "." ] ; then
    _SCRIPTDIR="`dirname "$_SCRIPTDIR"`"
fi

#convert /./ to /
_SCRIPTDIR=`echo $_SCRIPTDIR | sed -e 's;/\./;/;g'`
_ROOTDIR="`dirname "$_SCRIPTDIR"`"

add64bitPackages() {
  yum install glibc.i686 zlib.i686 ncurses-libs.i686 libstdc libstdc++.i686 libzip.i686 libX11.i686 libXrandr.i686 SDL.i686
  yum install mesa-libGL.i686 mesa-libEGL.i686
}

chkWrkDir() {
  if [ -z "${_WORKDIR}" ] ; then
    echo _WORKDIR must be set
    exit 1
  fi
}

doFoo() {
  echo _ROOTDIR=${_ROOTDIR}
}


createProjects() {
  android list targets
#  android create project --target <target-id> --name MyFirstApp \
#    --path <path-to-workspace>/MyFirstApp --activity MainActivity \
#    --package com.example.myfirstapp

  _SUBDIR=tmp
  mkdir -p ${_ROOTDIR}/${_SUBDIR}
  pushd ${_ROOTDIR}/${_SUBDIR} > /dev/null

  android create project \
    --target android-16 \
    --name Shaman \
    --path ./shaman \
    --activity MainActivity \
    --package org.emergent.android.weave

  android create lib-project \
    --target android-16 \
    --name client \
    --path ./client \
    --package org.emergent.android.weave.client

  popd > /dev/null

}

createProjects2() {

#  android create lib-project --name <your_project_name> \
#    --target <target_ID> \
#    --path path/to/your/project \
#    --package <your_library_package_namespace>

  popd > /dev/null
}

doInSubDir() {
  _SUBDIR="$1"
  shift
  pushd ${_SUBDIR} > /dev/null
  exec $@
  popd > /dev/null
}

doSms() {
(
  echo open localhost 5554
  sleep 2
  echo "sms send 111111 $@"
  sleep 1
  echo "quit"
  sleep 1
  ) | telnet
}

mkSdcardWritable() {
  adb shell mount -o remount rw /sdcard
}

doLint() {
  ant debug
  lint --check NewApi .
}

_CMD=$1
shift

case "${_CMD}" in
  create )
    createProjects "$@"
    ;;
  foo )
    doFoo "$@"
    ;;
  sms )
    doSms "$@"
    ;;
  dosub )
    doInSubDir "$@"
    ;;
  mksdrw )
    mkSdcardWritable "$@"
    ;;
  lint )
    doLint "$@"
    ;;
  * )
    echo "Unknown command: ${_CMD}"
    ;;
esac
