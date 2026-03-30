# Occtet Curator NATS Backend
This directory contains the microservice and shared libraries for the Occtet-backend 

## 1. Configuration (.env)
We use a `.env` file to manage environment-specific variables (DB credentials, Keycloak URIs, etc.)
1. Copy `.env.example` to `.env`
2. Adjust the values (e.g., `DB_PASSWORD`, `KEYCLOAK_CLIENT_SECRET`)
3. **Important**: These variables are injected into the containers at runtime; no rebuild is required when values 
   in `.env`, simply just restart the images.

## 2. Building

To build everything, in the root directory, execute

`mvn package`

To build a single module, for example the template, execute the same command in its directory.

You can skip tests with `-DskipTests=true`.

**Create docker images:**

To **build all** microservice images using the provided script:
run 

```.\build-all-docker-images.sh```

or do it manually:

Enter the module directory which includes a Dockerfile, for example occtet-nats-ai-service, and execute
`mvn clean package dockerfile:build`

Export Docker image as file for remote deployment:
`docker save occtet-nats-ai-service | gzip > occtet-nats-ai-service.tar.gz`

Import Docker image on remote machine:
`gunzip -c occtet-nats-ai-service.tar.gz | docker load`

## 3. Installing 

**Install Common Libraries**

In `occtet-nats` run:
```
mvn clean install 
```

<small>Note: the occtet-common library is required for the frontend</small>

## 3. Running the Stack - Docker Setup
**Prerequisites**
1. **Volumes**: Create the persistent storage volumes: 
```
docker volume create nats_data
docker volume create db_data
docker volume create ollama_data
```
2. **AI Models**: The AI services will crash if the model is missing. Start Ollama and pull the model manually:
```
docker-compose up -d ollama
docker exec -it <ollama_container_id> 
ollama pull qwen3:30b
```
**Launching**
```
docker-compose up -d

docker-compose up -d <service-name>
```
<small>(Without -d if you want to see the log output in the console. Press Ctrl-C to stop.)</small>

The startup order is orchestrated: db & nats -> frontend (Initializes DB) -> backend-services.


### Common modules

The occtet-common and occtet-common-jpa modules contain shared code and resources. They are dependencies of all (jpa: most) other modules.
They are JAR files which Maven builds and puts into your local Maven Repository. They are not executable.
Model classes, tools (common), Dao and Entity (common-jpa) classes belong here. No services or controllers or UI.

### Creating new modules

To create a new module, you can copy the template module and adjust the pom.xml file (mainly artifactId, name, description).

### DB Schema

The init-postgresql-db.sql file contains the SQL commands to create the required logging tables.

### DB Logging

The microservices are using logback to log into the postgres database.
To create the required tables, use create-logback-tables-postgresql.sql

# Funding

OCCTET project has received funding from the Digital Europe Programme (DIGITAL), under grant agreement number: 101190474.

![Funded by EU](./assets/funded.jpg)




