# preliminary convinience script to build all docker images for OCCTET NATS services (includes building and installing deps)
mvn install
cd occtet-common
mvn clean install
cd ..
cd occtet-common-jpa
mvn clean install
cd ..
cd occtet-ort-client
mvn clean install
cd ..
cd occtet-nats-ai-copyrightfilter-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-ai-licensematcher-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-copyrightfilter-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-download-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-foss-report-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-issue-catcher-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-licensematcher-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-ort-run-starter-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-spdx-export
mvn clean package dockerfile:build
cd ..
cd occtet-nats-spdx-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-vulnerability-service
mvn clean package dockerfile:build
cd ..

