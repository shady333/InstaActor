#!/bin/bash
#InstaActor sh script
echo "shutting down Selenium Grid in docker"
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  docker-compose -f docker-compose-pi.yaml --compatibility down
else
  docker-compose -f docker-compose.yaml --compatibility down
fi