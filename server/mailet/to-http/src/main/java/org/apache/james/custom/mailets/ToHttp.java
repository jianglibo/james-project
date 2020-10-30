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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialise the email and pass it to an HTTP call
 * <p>
 * Sample configuration:
 *
 * <mailet match="All" class="HeadersToHTTP">
 * <url>http://192.168.0.252:3000/alarm</url>
 * <parameterKey>Test</parameterKey>
 * <parameterValue>ParameterValue</parameterValue>
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
  private String url;
  private String parameterKey = null;
  private String parameterValue = null;
  private boolean passThrough = true;

  @Override
  public void init() throws MessagingException {

    this.objectMapper = new ObjectMapper();

//        requestBuilder = RequestBuilder.post(url).setHeader("Content-Type", "application/json");
//        requestBuilder.setEntity()

    passThrough = (getInitParameter("passThrough", "true").compareToIgnoreCase("true") == 0);
    String targetUrl = getInitParameter("url");
    parameterKey = getInitParameter("parameterKey");
    parameterValue = getInitParameter("parameterValue");

    // Check if needed config values are used
    if (targetUrl == null || targetUrl.equals("")) {
      throw new MessagingException("Please configure a targetUrl (\"url\")");
    } else {
      try {
        // targetUrl = targetUrl + ( targetUrl.contains("?") ? "&" :
        // "?") + parameterKey + "=" + parameterValue;
        url = new URL(targetUrl).toExternalForm();
      } catch (MalformedURLException e) {
        throw new MessagingException(
                "Unable to construct URL object from url");
      }
    }

    // record the result
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("I will attempt to deliver serialised messages to "
              + targetUrl
              + ". "
              + (((parameterKey == null) || (parameterKey.length() < 1)) ? "I will not add any fields to the post. " : "I will prepend: " + parameterKey + "=" + parameterValue + ". ")
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
//            LOGGER.debug("{} HeadersToHTTP: {} named value pairs found", mail.getName(), pairs.size());
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

  private void addHeader(Mail mail, boolean success, String errorMessage) {
    try {
      MimeMessage message = mail.getMessage();
      message.setHeader("X-headerToHTTP", (success ? "Succeeded" : "Failed"));
      if (!success && errorMessage != null && errorMessage.length() > 0) {
        message.setHeader("X-headerToHTTPFailure", errorMessage);
      }
      message.saveChanges();
    } catch (MessagingException e) {
      LOGGER.error("Exception", e);
    }
  }


  private String httpPost(MailDto pairs) throws IOException {
    String body = objectMapper.writeValueAsString(pairs);

    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      HttpUriRequest request = RequestBuilder.post(url)
              .setEntity(EntityBuilder.create().setText(body).setContentType(ContentType.APPLICATION_JSON).build()).build();
      try (CloseableHttpResponse clientResponse = client.execute(request)) {
        String result = clientResponse.getStatusLine().getStatusCode() + ": " + clientResponse.getStatusLine();
        LOGGER.debug("HeadersToHTTP: {}", result);
        return result;
      }
    }
  }


  @Override
  public String getMailetInfo() {
    return "HTTP POST serialised message";
  }


}
