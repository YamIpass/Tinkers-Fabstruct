#!/bin/bash
# javap a fully qualified class from the libs jars: tools/jp.sh io.github.fabricators_of_create.porting_lib.fluids.FluidStack
JAVAP=/c/Users/YamIpass/.jdks/azul-21/bin/javap
CLS="$1"
JAR=$(for j in /c/Users/YamIpass/Documents/deepseek-harness/default-workspace/tconstruct-fabric/libs/*.jar; do
  if unzip -l "$j" 2>/dev/null | grep -q " ${CLS//.//}.class"; then echo "$j"; break; fi
done)
if [ -z "$JAR" ]; then echo "NOT FOUND: $CLS"; exit 1; fi
"$JAVAP" -cp "$JAR" "$CLS" 2>&1 | grep -v '^Compiled from'
