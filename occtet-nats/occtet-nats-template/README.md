Occtet NATS Microservice template
-

Use this as a starting point for new microservices. Create a copy and adjust (at least):
- artifactId (in pom.xml)
- package names (in all .java files)
- application.properties (in src/main/resources, and/or the -local/-dev/-live versions)
- name of main class
- microserviceDescriptor.json
- this README.

Also add your new service to the docker-compose.yml file in the root directory, so it can be started together with the other services and the NATS server and Postgres database.

### Building

To build: run
`mvn package`

To run (with dev profile, default is live):
`mvn spring-boot:run -Pdev`

To build a Docker file (with default profile, which is "live"):
`mvn package dockerfile:build`
This will be tagged automatically according to name and version in the pom.xml and made available locally
in the docker registry, so you should find it with `docker images`.

Please add the build command for your new module to the build-all-docker-images.sh script in the root directory, so it is built together with the other services when you run that script.



