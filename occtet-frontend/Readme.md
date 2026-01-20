*The Occtet Boc Frontend application*

*Prerequisites*

* PostgreSQL database (name etc see application-xxx.properties)

* NATS server

*Profiles*

this project uses 3 profiles:

* local (running on dev's machine)

* dev (run on dev server)

* live (run on a live server in dockerized environment)

Note the different connection urls in the appropriate application.properties file. This affects the database, nats url and subject.

*Just run*

Run the gradle wrapper with `bootRun` and the desired profile (local or dev):

`./gradlew bootRun --args='--spring.profiles.active=dev'`

You can also create a run config like this in IntelliJ.

*Building*

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

under Resources suggestions.json you will find String for the autocomplete feature