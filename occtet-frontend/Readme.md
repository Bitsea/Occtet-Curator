# The Occtet Curator Frontend Application
The frontend is a Jmix application. It acts as the primary UI and the database schema orchestrator.

## Custom Certificates (SSL/TLS)
If your Keycloak server uses internal certificates, the java runtime inside Docker must trust them. 
1. Create a folder named .certs in this directory (it is ignored by Git).
2. Export your certificate (e.g., keycloak.crt) and place it inside .certs/.
3. The entrypoint.sh script will automatically import all files in this folder into the container's JVM truststore at startup.

## Building the Docker Image
**Step 1: Build the JAR**

Build the application in production mode. Ensure you clean old jars to prevent Docker from picking up the wrong version.

```./gradlew -Pvaadin.productionMode=true bootJar```
if this fails try:
```./gradlew bootJar "-Pvaadin.productionMode=true"```

**Step 2: Build the Image**<small> (make sure the package `occtet-common` is installed first)</small>
```
docker build -t occtet-boc-frontend:0.3.10-SNAPSHOT .
```
<small>Note: change the version if needed</small>

## Profiles
- **local:** For development on your host machine (uses localhost).
- **Live:** The default for Docker. It resolves all infrastructure URLs via environment variables (e.g., DB_URL, NATS_URL) provided by docker-compose.

## Prerequisites

* PostgreSQL database 

* NATS server

## Profiles

The project uses 3 profiles:

* local (running on dev's machine)

* dev (run on dev server)

* live (run on a live server in dockerized environment)

Note the different connection urls in the appropriate application.properties file. This affects the database, nats url and subject.

## Run

Run the gradle wrapper with `bootRun` and the desired profile (local or dev):

`./gradlew bootRun --args='--spring.profiles.active=dev'`

You can also create a run config like this in IntelliJ.

## Building

use:

`./gradlew bootJar` (or gradlew.bat on windows)

Note that gradlew must have execute permission flags (do `chmod +x gradlew`).

Run the jar with dev profile active:

`./gradlew bootRun'`

`java -Dspring.profiles.active=dev -jar build/libs/BocFrontend-(version).jar`

## Liquibase

We are using Liquibase to manage the database schema. The changelog file is located in `src/main/resources/eu/occtet/bocfrontend/liquibase'.
To generate the initial 000-tables.xml file from the current DB, run:

`./gradlew generateChangelog`

Note that this connects to the database configured in the build.gradle (liquibase block) and that database must have the proper structure, i.e. all changelogs applied. 
The 000-tables.xml is written to the default folder, and you should review it before committing.
Once the 000-tables.xml file is generated, remove all updates (in the year/month subdirectories), because the tables.xml already contains the current state of the DB structure.
Also make sure the logging-xx tables are not created by the tables.xml, because they are created by a separate file by the backend.
Do NOT remove the init-data files, because the tables.xml only contains the structure, not the data, and the init-data files contain the default data for the application to work properly.

## Autocomplete

For the autocomplete to work properly make sure that the init-data.xml got read into the database, to have default data of suggestions

# Funding

OCCTET project has received funding from the Digital Europe Programme (DIGITAL), under grant agreement number: 101190474. 

![Funded by EU](./src/main/resources/assets/funded.jpg)

