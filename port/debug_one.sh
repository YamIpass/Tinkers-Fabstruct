#!/usr/bin/env bash
# Debug one file with the full harness classpath.
set -u
WS=/c/Users/YamIpass/Documents/deepseek-harness/default-workspace
MC=$(cat /tmp/portslice/mcpath.txt)
LOMBOK=/c/Users/YamIpass/.gradle/caches/modules-2/files-2.1/org.projectlombok/lombok/1.18.34/ec547ef414ab1d2c040118fb9c1c265ada63af14/lombok-1.18.34.jar
GSON=/c/Users/YamIpass/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1/b3add478d4382b78ea20b1671390a858002feb6c/gson-2.10.1.jar
JSR=/c/Users/YamIpass/.gradle/caches/modules-2/files-2.1/com.google.code.findbugs/jsr305/3.0.2/25ea2e8b0c338a877313bd4672d3fe056ea78f0d/jsr305-3.0.2.jar
SLF=/c/Users/YamIpass/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/2.0.1/f48d81adce2abf5ad3cfe463df517952749e03bc/slf4j-api-2.0.1.jar
REMAPPED=$WS/tconstruct-fabric/build/loom-cache/remapped_working
LIBS=$(ls $REMAPPED/*.jar | grep -v sources | tr '\n' ':')
PRE=$WS/tconstruct-fabric/build/classes/java/main
CP="$MC:$LIBS$LOMBOK:$GSON:$JSR:$SLF:$PRE"
cd $WS/tconstruct-fabric
javac -J-Duser.language=en -proc:full --release 17 -nowarn -cp "$CP" -d /tmp/portslice/t2 "$@" 2>&1 | head -20
