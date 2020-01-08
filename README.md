Steps:
1. Build an Docker image:
    'docker image build -t instaactor:v0.1 .'
2. Execute image with parameters:
    'docker run instaactor:v0.1 exec:java -Dexec.mainClass="com.dudar.runner.Runner"'