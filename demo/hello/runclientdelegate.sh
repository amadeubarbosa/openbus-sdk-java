#!/bin/sh

mvn exec:java -Dexec.mainClass="tecgraf.openbus.demo.hello.HelloClientDelegate" -Dexec.classpathScope="runtime" -Dexec.args="$@"
