#!/bin/sh

(cd ..; mvn exec:java -Dexec.mainClass="tecgraf.openbus.demo.delegate.HelloClient" -Dexec.classpathScope="runtime")
