This is an experiment in wrapping Graal with tweaks to make it more
aware of how JRuby works.

Setup
=====

The Oracle Labs build of JDK 8 with JVMCI should be your PATH and
JAVA_HOME version of Java.

The Graal project should be checked out to ../graal/graal, with the
compiler and sdk subprojects built.

Building
========

With above requirements in place, just mvn package as normal.

Running
=======

Dependencies are not currently shaded, which means you must manually
put all dependencies on the boot classpath.

```
jruby -J-XX:+UnlockExperimentalVMOptions -J-XX:+EnableJVMCI -J-XX:+UseJVMCICompiler -J-Djvmci.Compiler=jruby-graal -J-Xbootclasspath/a:$JAVA_HOME/jre/lib/jvmci/jvmci-api.jar:$JAVA_HOME/jre/lib/jvmci/jvmci-hotspot.jar:../graal/graal/compiler/mxbuild/dists/graal-compiler.jar:../graal/graal/compiler/mxbuild/dists/graal-hotspot.jar:../graal/graal/compiler/mxbuild/dists/graal-options.jar:../graal/graal/compiler/mxbuild/dists/graal-serviceprovider.jar:../graal/graal/compiler/mxbuild/dists/graal.jar:../graal/graal/sdk/mxbuild/dists/graal-sdk.jar:../jruby-graal/target/jruby-graal-1.0-SNAPSHOT.jar -e 1

```