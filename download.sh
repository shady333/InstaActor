#!/bin/bash
#create backup
zip data_backup.zip -r -u data

#download
scp -r pi@192.168.1.200:/home/pi/projects/InstaActor/data .