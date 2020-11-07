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
import java.time.Duration;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

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

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  private boolean passThrough = true;
  private QueueClient sendClient = null;
  private String queueLabel;
  private String queueContentType;
  private Duration queueTimeToLive;

  @Override
  public void init() throws MessagingException {

    this.objectMapper = new ObjectMapper();

    passThrough = (getInitParameter("passThrough", "true").compareToIgnoreCase("true") == 0);
    String servicebusConn = getInitParameter("servicebusConn");
    queueContentType = getInitParameter("queueContentType", "application/json");
    queueLabel = getInitParameter("queueLabel", "mailDto");
    String qttl = getInitParameter("queueTimeToLive");
    int qttli = 12;
    if (qttl != null) {
      try {
        qttli = Integer.parseInt(qttl);
      } catch (Exception ignored) {
      }
    }
    queueTimeToLive = Duration.ofHours(qttli);

    // Check if needed config values are used
    if (servicebusConn == null || servicebusConn.equals("")) {
      throw new MessagingException("Please configure a targetUrl (\"servicebusConn\")");
    }

    try {
      sendClient = new QueueClient(new ConnectionStringBuilder(servicebusConn), ReceiveMode.PEEKLOCK);
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new MessagingException("InterruptedException from QueueClient.");
    } catch (ServiceBusException e) {
      e.printStackTrace();
      throw new MessagingException("ServiceBusException from QueueClient.");
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
      MailDto mailDto = ParsedMail.parse(message);
      sendMessage(mailDto);
      if (passThrough) {
        addHeader(mail, true, "true");
      } else {
        mail.setState(Mail.GHOST);
      }
    } catch (MessagingException | IOException e) {
      LOGGER.error("Exception", e);
      addHeader(mail, false, e.getMessage());
    } catch (InterruptedException | ServiceBusException e) {
      LOGGER.error("from servicebus", e);
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    try {
      sendClient.close();
    } catch (ServiceBusException e) {
      e.printStackTrace();
    }
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

  private void sendMessage(MailDto mailDto) throws JsonProcessingException, ServiceBusException, InterruptedException {
//    final String messageId = Integer.toString(i);
    Message message = new Message(objectMapper.writeValueAsBytes(mailDto));
    message.setContentType(queueContentType);
    message.setLabel(queueLabel);
//    message.setMessageId(messageId);
    message.setTimeToLive(queueTimeToLive);
    sendClient.send(message);
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
  //  }


  @Override
  public String getMailetInfo() {
    return "HTTP POST json message";
  }


//      public class Sender {
//        public void run() throws Exception {
//            // Create a QueueClient instance and then asynchronously send messages.
//            // Close the sender once the send operation is complete.
//            QueueClient sendClient = new QueueClient(new ConnectionStringBuilder(servicebusConn), ReceiveMode.PEEKLOCK);
//            this.sendMessagesAsync(sendClient).thenRunAsync(() -> {
//                System.out.println("i'm here.");
//                sendClient.closeAsync();
//            }).get();
//            Thread.sleep(1000);
//            sendClient.close();
//        }
//
//        CompletableFuture<Void> sendMessagesAsync(QueueClient sendClient) throws JsonProcessingException {
//            List<MailDto> data = List.of(mailDto);
//
//
//            List<CompletableFuture> tasks = new ArrayList<>();
//            for (int i = 0; i < data.size(); i++) {
//                final String messageId = Integer.toString(i);
//                Message message = new Message(objectMapper.writeValueAsBytes(mailDto));
//                message.setContentType(appConfig.getQueueContentType());
//                message.setLabel(appConfig.getQueueLabel());
//                message.setMessageId(messageId);
//                message.setTimeToLive(appConfig.getQueueTimeToLive());
//                System.out.printf("\nMessage sending: Id = %s", message.getMessageId());
//                tasks.add(
//                        sendClient.sendAsync(message).thenRunAsync(() -> {
//                            System.out.printf("\n\tMessage acknowledged: Id = %s", message.getMessageId());
//                        }));
//            }
//            return CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[tasks.size()]));
//        }
//    }


}
