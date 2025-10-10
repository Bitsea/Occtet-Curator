## Service for filtering false copyrights

# Building

Build JAR with Maven:

`mvn clean package`

The JAR will be created in the target folder.

Create docker image:

`docker build -t occtet-nats-copyrightFilter-service .`

Export Docker image as file for remote deployment:
`docker save occtet-nats-copyrightFilter-service | gzip > occtet-nats-copyrightFilter-service.tar.gz`

Import Docker image on remote machine:
`gunzip -c occtet-nats-copyrightFilter-service.tar.gz | docker load`

