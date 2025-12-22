## Service for fetching ort issues

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

