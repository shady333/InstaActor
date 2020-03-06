# General info
Simple application :alien: for interactions with Instagram service.
Its emulate user actions such as, "Like" :heart: and comment :pencil2: post.
Application written :computer: in research purpose to evaluate possibility
of automation usage on social networks services.

You can use application on your own risk and responsibility.

## Requirements:
* Docker installed (Memory 6GiB, Swap 2GiB);
* JDK 1.8;
* Luck and :raised_hands: from the right place :monkey:

## Optional:
* gmail account for agent interactions via email commands. [more info](#setup-and-configure-email-service)
* imagga account for image recognition possibilities. [IMAGGA Service](https://imagga.com/) [Not fully implemented]

# Steps to configure and execute

## General steps

1. Download git repo sources
2. Open "data" folder and update(and/or add more) files:
    1. Modify __*user.properties__ file (it should be like __myInsta_user.properties__) with your Instagram account info and actor parameters [details below](#InstaActor-configuration-parameters)
    2. Modify __*tags.csv__ file (it should be like __myInsta_tags.properties__) with tags to be used by Actor.
    All tags should be comma separated without spaces.
3. Modify access to other 3rd party services (optional):
    1. [Gmail service](#Setup-and-configure-email-service): __email.properties__, update file with your credentials for gmail service
    2. IMAGGA service: __access.properties__, update file with your credentials for imagga service

## Run service as a java process
4. Execute "__runAll.sh__" file.
5. Keep watching to the console output or use email service commands for interaction (if enabled and configured).
6. To Stop execution - stop the process "Ctrl+C".

## Run service inside Docker
4. Build an Docker image:
    ```
    'docker image build -t instaactor:v0.2 .'
   ```
5. Execute image with parameters:
    ```
    'docker-compose up'
   ```
6. Keep watching to the console output or use email service commands for interaction (if enabled and configured).
7. To Stop execution - stop the process "Ctrl+C" and shut down containers:
    ```
    'docker-compose down'
   ```
   
# InstaActor configuration parameters
__*user.properties__

|Parameter|Value|Comment|
|:---|:---|:---|
|hub.host|String|not used|
|hub.port|int|not used|
|view.min.delay|int|Minimum time to stay at the Image post (ms)|
|view.max.delay|int|Maximum time to stay at the Image post (ms)|
|video.min.delay|int|Minimum time for playback the Video post (ms)|
|video.max.delay|int|Maximum time for playback the Video post (ms)|
|likes.percentage|int|Probability to like the post (%)|
|comments.percentage|int|Probability to add the post comment (%)|
|posts.count|int|How many posts will proceed for each tag|
|acc.user|String|Instagram user login (REQUIRED)|
|acc.password|String|Instagram user password (REQUIRED)|
|debug.mode|boolean|Will use local chrome driver instead of connecting to the grid if enabled. For debugging purposes.|
|detect.media.content|boolean|To use or now Image Recognition service. Experimental feature.|
|email.service|boolean|Use configured email service for interactions|
|service.repeat|boolean|Shall service start over after completion|
|sleep.duration|int|Value in hours to wait before starting new run after completion (2 hour by default)|
|proxy|String|To use proxy address like 123.123.123.123:1111 for created ChromeDriver|

# Setup and configure email service
Current implementation is able to use gmail as an smtp service. This possibility could be used for getting status of current execution, etc.
You have to provide your gmail account credentials and configure your account properly.
My recommendation is to setup additional gmail account for this purposes.

Check account configuration:
1. [Two Step Verification should be turned off](https://support.google.com/accounts/answer/1064203?hl=en).
2. [Allow Less Secure App(should be turned on)](https://myaccount.google.com/lesssecureapps).

Update __data/email.properties__ with your account credentials and setup recipients and correct message subject if needed.

|Property|Description|
|:---|:---|
|username.email|username for gmail account which will receive requests|
|password.email|password for gmail account which will receive requests|
|subject.email|Generated email subject|
|actions.email|email address who will be able to send commends and receive responses|

Email structure for interaction:
To: email address of your gmail account which proceed actions.
Subject: __ACTION_NAME ACTOR_NAME__

|Subject variants|Description|Example (Actor name - __MyInsta__)|
|:---|:---|:---|
|ACTION_START ACTOR_"__name__"|Start service with provided __name__|Email Subject: ACTION_START ACTOR_MyInsta|
|ACTION_STOP  ACTOR_"__name__"|Stop service with __name__||
|ACTION_ENABLELIKE  ACTOR_"__name__"|Enable Like actions for service with __name__.||
|ACTION_DISABLELIKE  ACTOR_"__name__"|Disable Like actions for service with __name__||
|ACTION_ENABLECOMMENT  ACTOR_"__name__"|Enable Comment actions for service with __name__||
|ACTION_DISABLECOMMENT  ACTOR_"__name__"|Disable Comment actions for service with __name__||
|ACTION_DOWNLOAD ACTOR_"__name__"|Will sent an email with actor __name__ properties file|Email Subject: ACTION_DOWNLOAD ACTOR_MyInsta<br><br>You will receive an email with MyInsta.properties file attached.|
|ACTION_UPLOAD ACTOR_"__name__"|Replace existing actor __name__ properties file|Email Subject: ACTION_UPLOAD ACTOR_MyInsta<br>Attachment: MyInsta.properties<br><br>This will replace existing MyInsta.properties file with file from attachement|
|ACTION_STOP ACTOR_ALL|Stop execution for all instances||
|ACTION_ABORT ACTOR_ALL|Stop application execution. System.exit()||
|ACTION_STATUS ACTOR_"__name__"|Return status for service with __name__||
|ACTION_STATUS ACTOR_ALL|Return status for all registered services||

#______________
    
There are a lot of work **TODO**, feel free to contribute if you would like to :thumbsup:. 

Application workflow is described in short article - [Hey Insta, I'm not a bot!](https://shady333.blogspot.com/2020/01/instagram.html)
