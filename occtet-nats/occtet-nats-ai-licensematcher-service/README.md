##Service for calling an AI model

# Building

Build JAR with Maven:

`mvn clean package`

The JAR will be created in the target folder.

<<<<<<<< HEAD:occtet-nats-ai-licenseMatcher-service/README
Create docker image:

`docker build -t occtet-nats-ai-licenseMatcher-service .`
========
To build a Docker file (with default profile, which is "live"):
`mvn clean package dockerfile:build`
This will be tagged automatically according to name and version in the pom.xml and made available locally
in the docker registry, so you should find it with `docker images`.
>>>>>>>> main:occtet-nats-ai-licenseMatcher-service/README.md

Export Docker image as file for remote deployment:
`docker save occtet-nats-ai-licenseMatcher-service | gzip > occtet-nats-ai-licenseMatcher-service.tar.gz`

Import Docker image on remote machine:
`gunzip -c occtet-nats-ai-licenseMatcher-service.tar.gz | docker load`


# Setup

# Pgvector:

To run the application you need a working pgvector DB setup:

First install PostgreSQL.

To create an empty DB for the application:

```
CREATE DATABASE copyrights;
```

```
CREATE USER occtetUser WITH encrypted PASSWORD 'the password set in the application properties';
```

```
GRANT ALL PRIVILEGES ON DATABASE copyrights to occtetUser;
```

```
ALTER DATABASE copyrights OWNER TO occtetUser;
```

```
ALTER USER occtetUser WITH SUPERUSER;
```

You need to install pgvetor to do so follow the instruction here: https://github.com/pgvector/pgvector
if you have trouble with access rights, see that you have full access to your postgresql
and your dir where pgvector is forked

You can control your connection with the command:
psql -p <portnumber> -U <username> -d <databasename>
then you can also control the list of users (\du) or type help for other commands

Please ensure your PostgreSQL server is running on the same port defined in the application properties
# PG_TRGM
for the similarity search for text install extension pg_trm in postgresql with:

```
\c copyrights

CREATE EXTENSION pg_trgm;
```


# Ollama

As of now the application uses Ollama to connect to a LLM Model.
To set up, you need to download and install Ollama (https://ollama.com/download)

Before running the application make sure you have pulled the model set in application.properties.

Use ollama pull <model_name> or ollama run <modelname> to download the model onto your maschine, be careful with bigger models!

With 'ollama help' you see other helpful commands

Under application.properties the url and the name of the model are defined,
which have to be changed if you use a different model or a different 'model provider' than ollama

