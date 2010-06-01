#!/bin/sh

(mvn exec:java -Dexec.mainClass="tecgraf.openbus.demo.eventSink.EventSinkDemo" -Dexec.classpathScope="runtime")
