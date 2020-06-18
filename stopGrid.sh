#!/bin/bash
echo "shutting down Selenium Grid in docker"
echo $OSTYPE
if [[ "$OSTYPE" == "linux-gnueabihf" ]]; then
  echo "PI4"
  docker-compose -f docker-compose-pi.yaml --compatibility down
else
  echo "OTHERS"
  docker-compose -f docker-compose.yaml --compatibility down
fi