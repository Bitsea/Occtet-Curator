Occtet NATS SPDX Export
-

Service for exporting Spdx documents.

### Building

To build: run
`mvn package`

To run (with dev profile, default is live):
`mvn spring-boot:run -Pdev`

To build a Docker file (with default profile, which is "live"):
`mvn package dockerfile:build`
This will be tagged automatically according to name and version in the pom.xml and made available locally
in the docker registry, so you should find it with `docker images`.

#Setup

# DB:

To run the application you need a working DB setup:

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

You can control your connection with the command:
psql -p <portnumber> -U <username> -d <databasename>
then you can also control the list of users (\du) or type help for other commands

Please ensure your PostgreSQL server is running on the same port defined in the application properties


