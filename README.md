# Occtet-Curator

This repository contains two project trees: 

* occtet-frontend (a gradle project)
* occtet-nats (a maven project with several modules).

The whole project is meant to be run docker-based.

## Deployment Overview 
To get the system running in a live/dockerized enviroment:
1. Configure your enviroment in the `.env` file (see `occtet-nats/.env.example`)
2. Build the common libraries in occtet-nats (mvn install).
3. Build the frontend Docker image in occtet-frontend.
4. Build the microservice Docker images in occtet-nats.
5. Deploy using docker-compose up -d from the occtet-nats directory.

<small>Detailed instructions are available in the respective subdirectories.</small>

# Funding

OCCTET project has received funding from the Digital Europe Programme (DIGITAL), under grant agreement number: 101190474.

![Funded by EU](./assets/funded.jpg)


