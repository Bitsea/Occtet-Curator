# Occtet-Curator

This repository contains two project trees: 

* occtet-frontend (a gradle project)
* occtet-nats (a maven project with several modules).

The whole project is meant to be run docker-based.

The docker-compose.yml file is in the occtet-nats directory. About how to build the components, please see the readme files in the occtet-frontend and occtet-nats directories. 

Basically you have to build occtet-common 
and occtet-common-jpa first, `mvn install` into your local m2 repo, then build the docker images for each microservice and the frontend. Note that you need to pull an llm with ollama before you can fire up everything with `docker compose up`.

# Funding

OCCTET project has received funding from the Digital Europe Programme (DIGITAL), under grant agreement number: 101190474.

![Funded by EU](./assets/funded.jpg)


