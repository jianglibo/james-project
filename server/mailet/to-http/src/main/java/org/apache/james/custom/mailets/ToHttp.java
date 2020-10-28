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

package org.apache.james.jdkim.mailets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.jdkim.DKIMSigner;
import org.apache.james.jdkim.api.BodyHasher;
import org.apache.james.jdkim.api.Headers;
import org.apache.james.jdkim.api.SignatureRecord;
import org.apache.james.jdkim.exceptions.PermFailException;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import com.github.fge.lambdas.Throwing;

/**
 * This mailet sign a message using the DKIM protocol
 * If the privateKey is encoded using a password then you can pass
 * the password as privateKeyPassword parameter.
 *
 * Sample configuration with inlined private key:
 *
 * <pre><code>
 * &lt;mailet match=&quot;All&quot; class=&quot;DKIMSign&quot;&gt;
 *   &lt;signatureTemplate&gt;v=1; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;&lt;/signatureTemplate&gt;
 *   &lt;privateKey&gt;
 *   -----BEGIN RSA PRIVATE KEY-----
 *   MIICXAIBAAKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoT
 *   M5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRH
 *   r7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB
 *   AoGBAI8XcwnZi0Sq5N89wF+gFNhnREFo3rsJDaCY8iqHdA5DDlnr3abb/yhipw0I
 *   /1HlgC6fIG2oexXOXFWl+USgqRt1kTt9jXhVFExg8mNko2UelAwFtsl8CRjVcYQO
 *   cedeH/WM/mXjg2wUqqZenBmlKlD6vNb70jFJeVaDJ/7n7j8BAkEA9NkH2D4Zgj/I
 *   OAVYccZYH74+VgO0e7VkUjQk9wtJ2j6cGqJ6Pfj0roVIMUWzoBb8YfErR8l6JnVQ
 *   bfy83gJeiQJBAOHk3ow7JjAn8XuOyZx24KcTaYWKUkAQfRWYDFFOYQF4KV9xLSEt
 *   ycY0kjsdxGKDudWcsATllFzXDCQF6DTNIWECQEA52ePwTjKrVnLTfCLEG4OgHKvl
 *   Zud4amthwDyJWoMEH2ChNB2je1N4JLrABOE+hk+OuoKnKAKEjWd8f3Jg/rkCQHj8
 *   mQmogHqYWikgP/FSZl518jV48Tao3iXbqvU9Mo2T6yzYNCCqIoDLFWseNVnCTZ0Q
 *   b+IfiEf1UeZVV5o4J+ECQDatNnS3V9qYUKjj/krNRD/U0+7eh8S2ylLqD3RlSn9K
 *   tYGRMgAtUXtiOEizBH6bd/orzI9V9sw8yBz+ZqIH25Q=
 *   -----END RSA PRIVATE KEY-----
 *   &lt;/privateKey&gt;
 * &lt;/mailet&gt;
 * </code></pre>
 *
 * Sample configuration with file-provided private key:
 *
 * <pre><code>
 * &lt;mailet match=&quot;All&quot; class=&quot;DKIMSign&quot;&gt;
 *   &lt;signatureTemplate&gt;v=1; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;&lt;/signatureTemplate&gt;
 *   &lt;privateKeyFilepath&gt;dkim-signing.pem&lt;/privateKeyFilepath&gt;
 * &lt;/mailet&gt;
 * </code></pre>
 *
 * By default the mailet assume that Javamail will convert LF to CRLF when sending
 * so will compute the hash using converted newlines. If you don't want this
 * behaviour then set forceCRLF attribute to false.
 */
public class ToHttp extends GenericMailet {

        private static final Logger LOGGER = LoggerFactory.getLogger(HeadersToHTTP.class);

    /**
     * The name of the header to be added.
     */
    private String url;
    private String parameterKey = null;
    private String parameterValue = null;
    private boolean passThrough = true;

    @Override
    public void init() throws MessagingException {

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
                        "Unable to contruct URL object from url");
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
     * @param mail
     *            the mail being processed
     * 
     */
    @Override
    public void service(Mail mail) {
        try {
            LOGGER.debug("{} HeadersToHTTP: Starting", mail.getName());
            MimeMessage message = mail.getMessage();
            HashSet<NameValuePair> pairs = getNameValuePairs(message);
            LOGGER.debug("{} HeadersToHTTP: {} named value pairs found", mail.getName(), pairs.size());
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

    private String httpPost(HashSet<NameValuePair> pairs) throws IOException {

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpUriRequest request = RequestBuilder.post(url).addParameters(pairs.toArray(new NameValuePair[0])).build();
            try (CloseableHttpResponse clientResponse = client.execute(request)) {
                String result = clientResponse.getStatusLine().getStatusCode() + ": " + clientResponse.getStatusLine();
                LOGGER.debug("HeadersToHTTP: {}", result);
                return result;
            }
        }
    }

    private HashSet<NameValuePair> getNameValuePairs(MimeMessage message) throws UnsupportedEncodingException, MessagingException {

        // to_address
        // from
        // reply to
        // subject

        HashSet<NameValuePair> pairs = new HashSet<>();

        if (message != null) {
            if (message.getSender() != null) {
                pairs.add(new BasicNameValuePair("from", message.getSender().toString()));
            }
            if (message.getReplyTo() != null) {
                pairs.add(new BasicNameValuePair("reply_to", Arrays.toString(message.getReplyTo())));
            }
            if (message.getMessageID() != null) {
                pairs.add(new BasicNameValuePair("message_id", message.getMessageID()));
            }
            if (message.getSubject() != null) {
                pairs.add(new BasicNameValuePair("subject", message.getSubject()));
            }
            pairs.add(new BasicNameValuePair("size", Integer.toString(message.getSize())));
        }

        pairs.add(new BasicNameValuePair(parameterKey, parameterValue));

        return pairs;
    }

    @Override
    public String getMailetInfo() {
        return "HTTP POST serialised message";
    }


}
