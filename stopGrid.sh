#!/bin/bash
#InstaActor sh script
echo "shutting down Selenium Grid in docker"
if [[ "$OSTYPE" == "linux-gnueabihf" ]]; then
  echo "PI4"
  docker-compose -f docker-compose-pi.yaml --compatibility down
else
  echo "OTHERS"
  docker-compose -f docker-compose.yaml --compatibility down
fi