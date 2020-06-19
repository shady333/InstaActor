#!/bin/bash
#download for backup
scp -r pi@192.168.1.200:/home/pi/projects/InstaActor/data data_pi

#create backup
zip data_backup_pi.zip -r -u data_pi

#upload
scp -r data pi@192.168.1.200:/home/pi/projects/InstaActor