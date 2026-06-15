## Service for turning valid spdx json into entities

# Building

Build JAR with Maven:

`mvn clean package`

The JAR will be created in the target folder.

To build a Docker file (with default profile, which is "live"):
`mvn clean package dockerfile:build`
This will be tagged automatically according to name and version in the pom.xml and made available locally
in the docker registry, so you should find it with `docker images`.

Export Docker image as file for remote deployment:
`docker save occtet-nats-spdx-service | gzip > occtet-nats-spdx-service.tar.gz`

Import Docker image on remote machine:
`gunzip -c occtet-nats-spdx-service.tar.gz | docker load`
