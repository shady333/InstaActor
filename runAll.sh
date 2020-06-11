#!/bin/bash
#InstaActor sh script

#Phase 1. Compile application
echo "compile application service"
mvn clean compile

#Phase 2. Start service
echo "starting service"
mvn exec:java -Dexec.mainClass=phaseII.Executor -Dlog4j.configuration=file:src/main/resources/log4j.properties