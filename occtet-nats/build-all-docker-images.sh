#
# Copyright (C) 2025 Bitsea GmbH
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https:www.apache.orglicensesLICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  License-Filename: LICENSE
#

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
cd occtet-nats-licensematcher-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-ort-run-start-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-process-run-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-spdx-export-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-spdx-service
mvn clean package dockerfile:build
cd ..
cd occtet-nats-vulnerability-service
mvn clean package dockerfile:build
cd ..

