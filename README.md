## General info
Simple application :alien: for interactions with Instagram service to emulate user actions such as, "Like" :heart: and comment :pencil2: post.
Application written :computer: in research purpose to evaluate posibility of automation usage on social networks services.

You can use application on your own risk and responsibility.

## Requirements:
    Docker installed;

##Steps to execute:
1. Modify __*.properties__ file with your user info and enable required actions
2. Modify __*tags.csv__ file with required tags to be used
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
    
There are a lot of work **TODO**. feel free to contribute if you would like to :thumbsup:. 

Application workflow is described in short article - [Hey Insta, I'm not a bot!](https://shady333.blogspot.com/2020/01/instagram.html)
