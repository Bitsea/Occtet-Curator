Occtet NATS Parent
-

### Building

To build everything, in the root directory, execute

`mvn package`

To build a sigle module, for example the template, execute the same command in its directory.

You can skip tests with `-DskipTests=true`.

Create docker images:

Enter the module directory which includes a Dockerfile, for example occtet-nats-ai-service, and execute
`mvn clean package dockerfile:build`

Export Docker image as file for remote deployment:
`docker save occtet-nats-ai-service | gzip > occtet-nats-ai-service.tar.gz`

Import Docker image on remote machine:
`gunzip -c occtet-nats-ai-service.tar.gz | docker load`


### Installing the Common Library

Install the occtet-nats-parent first, afterward the common library and the common-jpa into your local Maven repository, in directories (occtet-nats(occtet-nats-parent), occtet-common and occtet-common-jpa), execute
`mvn install`
This is required to build the frontend project.

### Running

The template module is a standalone Spring Boot application. To run it, just execute the created jar file in the target directory.

### Common modules

The occtet-common and occtet-common-jpa modules contain shared code and resources. They are dependencies of all (jpa: most) other modules.
They are JAR files which Maven builds and puts into your local Maven Repository. They are not executable.
Model classes, tools (common), Dao and Entity (common-jpa) classes belong here. No services or controllers or UI.

### Creating new modules

To create a new module, you can copy the template module and adjust the pom.xml file (mainly artifactId, name, description).

### DB Logging

The microservices are using logback to log into the postgres database.
To create the required tables, use create-logback-tables-postgresql.sql

### Docker setup

We provice a docker-compose.yml file to start a NATS server and a Postgres database init script (init-postgresql-db.sql,
this includes the logback tables creation script) to create and initialize a Postgres database
with the required tables.

Create the required Docker volumes:

`docker volume create nats_data`

`docker volume create db_data`

`docker volume create ollama_data`

You need to get a model for ollama (use the current container id, see docker ps):
`docker exec -it b5946a233546 sh`
`ollama pull qwen3:30b`

Note that the ai based services will not start correctly without a model, so download it first.

Once you have built and installed all docker images locally (see above), you can start everything with
`docker-compose up -d`

To start up just a single service, use
`docker-compose up -d <service-name>`
(Without -d if you want to see the log output in the console. Press Ctrl-C to stop.)




