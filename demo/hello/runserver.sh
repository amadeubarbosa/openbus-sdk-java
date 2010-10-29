#!/bin/sh

mvn exec:java -Dexec.mainClass="tecgraf.openbus.demo.hello.HelloServer" -Dexec.classpathScope="runtime"
