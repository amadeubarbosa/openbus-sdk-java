#!/bin/sh

(cd ..; mvn exec:java -Dexec.mainClass="tecgraf.openbus.demo.hello.HelloClient" -Dexec.classpathScope="runtime")
