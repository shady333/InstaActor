#!/bin/bash
#InstaActor sh script
echo "shutting down Selenium Grid in docker"
docker-compose -f docker-compose-gridonly.yaml --compatibility down