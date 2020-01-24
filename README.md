## General info
Simple application :alien: for interactions with Instagram service to emulate user actions such as, "Like" :heart: and comment :pencil2: post.
Application written :computer: in research purpose to evaluate posibility of automation usage on social networks services.

You can use application on your own risk and responsibility.

## Requirements:
    Docker installed;

## Steps to execute:
1. Modify __*.properties__ file with your Instagram account info and enable required actions
2. Modify __*tags.csv__ file with required tags to be used
4. Modify __access.properties__ file with your credentials at imagoo service
3. Build an Docker image:
    ```
    'docker image build -t instaactor:v0.1 .'
   ```
4. Execute image with parameters:
    ```
    'docker-compose up'
   ```
5. Wait till application finish.
5. Stop:
    ```
    'docker-compose down'
   ```

## Setup

### email service
Current implementation uses gmail as an smtp service. You have to provide your gmail account credentials and configure your account properly.
My recomendation is to setup additional gmail account for this purposes.

Check account configuration:
1. [Two Step Verification should be turned off](https://support.google.com/accounts/answer/1064203?hl=en).
2. [Allow Less Secure App(should be turned on)](https://myaccount.google.com/lesssecureapps).

Update __email.properties__ with your account credentials and setup recepients and correct message subject if needed.

#______________
    
There are a lot of work **TODO**. feel free to contribute if you would like to :thumbsup:. 

Application workflow is described in short article - [Hey Insta, I'm not a bot!](https://shady333.blogspot.com/2020/01/instagram.html)
