#!/bin/bash

#Phase. Start selenium grid
echo "starting Selenium GRID in docker"
if [[ "$OSTYPE" == "linux-gnueabihf" ]]; then
  echo "PI4"
  docker-compose -f docker-compose-pi.yaml --compatibility up -d
else
  echo "OTHERS"
  docker-compose -f docker-compose.yaml --compatibility up -d
fi
