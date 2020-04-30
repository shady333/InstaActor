#!/bin/bash
#InstaActor sh script
trap ctrl_c INT

function ctrl_c() {
        echo "** Trapped CTRL-C"
        echo "shutting down Selenium Grid in docker"
        docker-compose -f docker-compose-gridonly.yaml --compatibility down
}

#Phase 1. copy resources from git (optional)

rm -rfv target

#Phase 2. Start selenium grid
echo "starting Selenium GRID in docker"
docker-compose -f docker-compose-gridonly.yaml --compatibility up -d

#Phase 3. Compile application
echo "compile application service"
mvn clean compile

#Phase 4. Start service
echo "starting service"
mvn exec:java -Dexec.mainClass=com.dudar.runner.Runner -Dlog4j.configuration=file:src/main/resources/log4j.properties -Dexec.args="-Doption=START"