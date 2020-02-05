# General info
Simple application :alien: for interactions with Instagram service.
Its emulate user actions such as, "Like" :heart: and comment :pencil2: post.
Application written :computer: in research purpose to evaluate possibility
of automation usage on social networks services.

You can use application on your own risk and responsibility.

## Requirements:
* Docker installed;
* Java 1.8;
* Luck and :raised_hands: from the right place :monkey:

## Optional:
gmail account for tracking actions. [more info](#setup-and-configure-email-service)

# Steps to configure and execute:
1. Download git repo sources
2. Open "data" folder and update(and/or add more) files:
    1. Modify __*user.properties__ file with your Instagram account info and actor parameters [details below](#InstaActor-configuration-parameters)
    2. Modify __*tags.csv__ file with required tags to be used.
    All tags should be comma separated without spaces.
3. Modify access to other 3rd party services (optional):
    1. IMAGOO service: __src/main/resources/access.properties__, update file with your credentials for imagoo service
    2. Gmail service: __src/main/resources/email.properties__, update file with your credentials for gmail service
4. Build an Docker image:
    ```
    'docker image build -t instaactor:v0.2 .'
   ```
5. Execute image with parameters:
    ```
    'docker-compose up'
   ```
6. Keep watching to the console output or use email service commands for interaction (if enabled and configured).
5. To Stop execution - stop the process "Ctrl+C" and shut down containers:
    ```
    'docker-compose down'
   ```

# InstaActor configuration parameters
__*user.properties__

|Parameter|Value|Comment|
|:---|:---|:---|
|hub.host|String|not used|
|hub.port|int|not used|
|view.min.delay|int|Minimum time for stay at the Image post (ms)|
|view.max.delay|int|Maximum time to stay at the Image post (ms)|
|...|...|...|

# Setup and configure email service
Current implementation uses gmail as an smtp service. You have to provide your gmail account credentials and configure your account properly.
My recomendation is to setup additional gmail account for this purposes.

Check account configuration:
1. [Two Step Verification should be turned off](https://support.google.com/accounts/answer/1064203?hl=en).
2. [Allow Less Secure App(should be turned on)](https://myaccount.google.com/lesssecureapps).

Update __email.properties__ with your account credentials and setup recepients and correct message subject if needed.

#______________
    
There are a lot of work **TODO**. feel free to contribute if you would like to :thumbsup:. 

Application workflow is described in short article - [Hey Insta, I'm not a bot!](https://shady333.blogspot.com/2020/01/instagram.html)
