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

```
java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler -Djvmci.Compiler=jruby-graal --module-path ../jruby-graal/target/jruby-graal-0.1-SNAPSHOT.jar:../jruby-graal/graal/compiler/mxbuild/modules/jdk.internal.vm.compiler.jar:../jruby-graal/graal/sdk/mxbuild/modules/org.graalvm.graal_sdk.jar:../jruby-graal/graal/truffle/mxbuild/modules/com.oracle.truffle.truffle_api.jar --add-exports jdk.internal.vm.ci/jdk.vm.ci.services=jruby.graal -Djruby.home=/Users/headius/projects/jruby -jar ../jruby/lib/jruby.jar -e 1
```
