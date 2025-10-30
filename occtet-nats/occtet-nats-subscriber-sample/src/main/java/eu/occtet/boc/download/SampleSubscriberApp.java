/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.download;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class SampleSubscriberApp {

  @Autowired
  private Connection natsConnection;

  @Value("${nats.subject}")
  private String subject;

  public static void main(String[] args) {
      SpringApplication.run(SampleSubscriberApp.class, args);
  }


  @PostConstruct
  public void onInit() {
    subscribeToSubject(subject);
  }

  public void subscribeToSubject(String subject) {
    Subscription subscription = natsConnection.subscribe(subject);
    try {
      while(true) {
        Message message = subscription.nextMessage(1000);
        if (message != null) {
          String msg = new String(message.getData(), StandardCharsets.UTF_8);
          System.out.println("Received message: " + msg);
        }
      }
    } catch (InterruptedException e) {

    }



  }


}