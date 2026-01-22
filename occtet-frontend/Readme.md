# The Occtet Boc Frontend Application

## Prerequisites

* PostgreSQL database (name etc see application-xxx.properties)

* NATS server

## Profiles

this project uses 3 profiles:

* local (running on dev's machine)

* dev (run on dev server)

* live (run on a live server in dockerized environment)

Note the different connection urls in the appropriate application.properties file. This affects the database, nats url and subject.

## Run the Application

Run the gradle wrapper with `bootRun` and the desired profile (local or dev):

`./gradlew bootRun --args='--spring.profiles.active=dev'`

You can also create a run config like this in IntelliJ.

## Building

use:

`./gradlew bootJar` (or gradlew.bat on windows)

note that gradlew must have execute permission flags (do `chmod +x gradlew`).

Run the jar with dev profile active:

`./gradlew bootRun'`

`java -Dspring.profiles.active=dev -jar build/libs/BocFrontend-(version).jar`


*Building a docker image*

Step 1: build the jar in production mode (note that live profile is default):

`./gradlew -Pvaadin.productionMode=true bootJar`

Step 2: build the docker image (in the same directory as the Dockerfile, replace correct version):

`docker build -t occtet-boc-frontend:0.3.3-SNAPSHOT .`

IMPORTANT: Do not use gradle jibDockerBuild to build currently, because due to
an unknown bug it does not build in production mode.



*Autocomplete*

for the autocomplete to work properly make sure that the init-data.xml got read into the database, to have default data of suggestions

## Development

### Configuration

To create a new application configuration (`AppConfiguration`), follow these steps:

1. Navigate to the enum  
   `/java/../entity/appconfigurations/AppConfigKey.java`
2. Add a new enum constant with the following attributes:
    1. **Key** – Group name (use the inner enum `AppConfigGroup`) combined with the configuration name
    2. **Default value**
    3. **Data type** – Use the `AppConfigType` enum
    4. **Description**

#### Example

```java
GENERAL_BASE_PATH(
        AppConfigGroup.GENERAL + ".base_path",
        "C:",
        AppConfigType.STRING,
        "The base path to which project files will be downloaded."
),
```
#### Usage example
```java
String basePath = appConfigurationRepository
        .findByConfigKey(AppConfigKey.GENERAL_BASE_PATH)
        .get()
        .getValue();
```
