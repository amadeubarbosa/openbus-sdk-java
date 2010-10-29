#!/bin/sh

mvn exec:java -Dexec.mainClass="tecgraf.openbus.demo.delegate.HelloServer" -Dexec.classpathScope="runtime"
