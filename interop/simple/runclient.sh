#!/bin/sh

mvn exec:java -Dexec.mainClass="tecgraf.openbus.demo.hello.Client" -Dexec.classpathScope="runtime"
