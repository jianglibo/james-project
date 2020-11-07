/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.custom.mailets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.microsoft.azure.servicebus.QueueClient;

/**
 * Serialise the email and pass it to an HTTP call
 * <p>
 * Sample configuration:
 *
 * <mailet match="All" class="org.apache.james.custom.mailets.ToHTTP">
 * <servicebusConn>Endpoint=sb://xxx</servicebusConn>
 * <passThrough>true</passThrough>
 * </mailet>
 */

public class ToHttp extends GenericMailet {

  private static final Logger LOGGER = LoggerFactory.getLogger(ToHttp.class);

  private ObjectMapper objectMapper = new ObjectMapper();

  //    private RequestBuilder requestBuilder;

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * The name of the header to be added.
   */
  private String servicebusConn;
  private boolean passThrough = true;

  @Override
  public void init() throws MessagingException {

    this.objectMapper = new ObjectMapper();

    passThrough = (getInitParameter("passThrough", "true").compareToIgnoreCase("true") == 0);
    servicebusConn = getInitParameter("servicebusConn");

    // Check if needed config values are used
    if (servicebusConn == null || servicebusConn.equals("")) {
      throw new MessagingException("Please configure a targetUrl (\"url\")");
    }

    // record the result
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("I will attempt to deliver serialised messages to "
              + servicebusConn
              + ". "
              + (passThrough ? "Messages will pass through." : "Messages will be ghosted."));
    }
  }

  /**
   * Takes the message, serialises it and sends it to the URL
   *
   * @param mail the mail being processed
   */
  @Override
  public void service(Mail mail) {
    try {
      LOGGER.debug("{} HeadersToHTTP: Starting", mail.getName());
      MimeMessage message = mail.getMessage();
      MailDto pairs = ParsedMail.parse(message);
      String result = httpPost(pairs);
      if (passThrough) {
        addHeader(mail, true, result);
      } else {
        mail.setState(Mail.GHOST);
      }
    } catch (MessagingException | IOException e) {
      LOGGER.error("Exception", e);
      addHeader(mail, false, e.getMessage());
    }
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  private void addHeader(Mail mail, boolean success, String errorMessage) {
    try {
      MimeMessage message = mail.getMessage();
      message.setHeader("X-toHTTP", (success ? "Succeeded" : "Failed"));
      if (!success && errorMessage != null && errorMessage.length() > 0) {
        message.setHeader("X-toHTTPFailure", errorMessage);
      }
      message.saveChanges();
    } catch (MessagingException e) {
      LOGGER.error("Exception", e);
    }
  }


  // private String httpPost(MailDto pairs) throws IOException {
  //   String body = objectMapper.writeValueAsString(pairs);

  //   try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
  //     HttpUriRequest request = RequestBuilder.post(url)
  //             .setEntity(EntityBuilder.create().setText(body).setContentType(ContentType.APPLICATION_JSON).build()).build();
  //     try (CloseableHttpResponse clientResponse = client.execute(request)) {
  //       String result = clientResponse.getStatusLine().getStatusCode() + ": " + clientResponse.getStatusLine();
  //       LOGGER.debug("HeadersToHTTP: {}", result);
  //       return result;
  //     }
  //   }
  }


  @Override
  public String getMailetInfo() {
    return "HTTP POST json message";
  }


      public class Sender {
        public void run() throws Exception {
            // Create a QueueClient instance and then asynchronously send messages.
            // Close the sender once the send operation is complete.
            QueueClient sendClient = new QueueClient(new ConnectionStringBuilder(servicebusConn), ReceiveMode.PEEKLOCK);
            this.sendMessagesAsync(sendClient).thenRunAsync(() -> {
                System.out.println("i'm here.");
                sendClient.closeAsync();
            }).get();
            Thread.sleep(1000);
            sendClient.close();
        }

        CompletableFuture<Void> sendMessagesAsync(QueueClient sendClient) throws JsonProcessingException {
            List<MailDto> data = List.of(mailDto);


            List<CompletableFuture> tasks = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                final String messageId = Integer.toString(i);
                Message message = new Message(objectMapper.writeValueAsBytes(mailDto));
                message.setContentType(appConfig.getQueueContentType());
                message.setLabel(appConfig.getQueueLabel());
                message.setMessageId(messageId);
                message.setTimeToLive(appConfig.getQueueTimeToLive());
                System.out.printf("\nMessage sending: Id = %s", message.getMessageId());
                tasks.add(
                        sendClient.sendAsync(message).thenRunAsync(() -> {
                            System.out.printf("\n\tMessage acknowledged: Id = %s", message.getMessageId());
                        }));
            }
            return CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[tasks.size()]));
        }
    }



}
