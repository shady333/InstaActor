#!/bin/bash

#Phase. Start selenium grid
echo "starting Selenium GRID in docker"
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  docker-compose -f docker-compose-pi.yaml --compatibility up -d
else
  docker-compose -f docker-compose.yaml --compatibility up -d
fi
