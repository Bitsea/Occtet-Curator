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




