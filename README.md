This is an experiment in wrapping Graal with tweaks to make it more
aware of how JRuby works.

Dependencies
============

* Git
* Python 2.7
* gcc and g++

The mx and graal submodules will be initialized during the build with git.

Building
========

With above requirements in place, just mvn package as normal.

Running
=======

Dependencies are not currently shaded, which means you must manually
put all dependencies on the boot classpath.

Java 8 with JVMCI:
```
jruby -J-XX:+UnlockExperimentalVMOptions -J-XX:+EnableJVMCI -J-XX:+UseJVMCICompiler -J-Djvmci.Compiler=jruby-graal -J-Xbootclasspath/a:$JAVA_HOME/jre/lib/jvmci/jvmci-api.jar:$JAVA_HOME/jre/lib/jvmci/jvmci-hotspot.jar:../graal/graal/compiler/mxbuild/dists/graal-compiler.jar:../graal/graal/compiler/mxbuild/dists/graal-hotspot.jar:../graal/graal/compiler/mxbuild/dists/graal-options.jar:../graal/graal/compiler/mxbuild/dists/graal-serviceprovider.jar:../graal/graal/compiler/mxbuild/dists/graal.jar:../graal/graal/sdk/mxbuild/dists/graal-sdk.jar:../jruby-graal/target/jruby-graal-1.0-SNAPSHOT.jar -e 1

```

Java 9: working on it