#!/bin/sh

#
#  Copyright (C) 2025 Bitsea GmbH
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      https:www.apache.orglicensesLICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  License-Filename: LICENSE
#
#
#

if [ -d "/certs" ]; then
    for cert in /certs/*; do
      if [ -f "$cert" ]; then
        alias=$(basename "$cert")
        echo "Importing runtime certificate: $alias"
        keytool -importcert -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -alias "$alias" -file "$cert" -noprompt
      fi
    done
fi

echo "Starting Spring Boot..."
exec java -Dspring.profiles.active=live -jar /app.jar