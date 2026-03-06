## Service for starting ORT run

For this to run, you have to run a local ORT server docker image. 
See here into the ORT server github repository for details to start the docker instance: https://github.com/eclipse-apoapsis/ort-server/tree/main
Basically you clone the tag version you want and then run "docker pull" and "docker compose up" in the ORT server directory. 

Then you can connect to the ORT Server API on localhost:8080 and see the ort-server on localhost:8082 in the browser. 

# Building

Build JAR with Maven:

`mvn clean package`

The JAR will be created in the target folder.

Create docker image:

`docker build -t occtet-nats-issue-catcher-service .`

Export Docker image as file for remote deployment:
`docker save occtet-nats-issue-catcher-service | gzip > occtet-nats-issue-catcher-service.tar.gz`

Import Docker image on remote machine:
`gunzip -c occtet-nats-issue-catcher-service.tar.gz | docker load`

