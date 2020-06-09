#!/bin/bash

#Phase 2. Start selenium grid
echo "starting Selenium GRID in docker"
docker-compose -f docker-compose-gridonly.yaml --compatibility up -d
