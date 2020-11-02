package org.apache.james.custom.mailets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.mime4j.stream.BodyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The sender domain is what receiving email server sees when initiating the session.
 * The from is what your recipients will see.
 * Return-Path would also be set to mailagent@mywebmail.com so that any delivery reports go to it instead of the sender.
 */
public class MailDto {

  private Map<String, String> headers = new HashMap<>();
  private List<MailStringBody> mailStringBodies;

  public Addresses getAddresses() {
    return addresses;
  }

  private Addresses addresses;

  public Map<String, String> getHeaders() {
    return headers;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(MailDto.class);

  public List<MailStringBody> getMailStringBodies() {
    return mailStringBodies;
  }

  public MailDto() {
  }

  public MailDto(MimeMessage message, List<MailStringBody> mailStringBodies) {
    this.mailStringBodies = mailStringBodies;
    this.addresses = new Addresses();
    if (message != null) {
      try {
        if (message.getSender() != null) {
          addresses.setSender(message.getSender());
        }
        if (message.getFrom() != null) {
          addresses.setFrom(message.getFrom());
        }
        if (message.getReplyTo() != null) {
          addresses.setReplyTo(message.getReplyTo());
        }
        if (message.getAllRecipients() != null) {
          addresses.setAllRecipients(message.getAllRecipients());
        }
        if (message.getMessageID() != null) {
          headers.put("message_id", message.getMessageID());
        }
        if (message.getSubject() != null) {
          headers.put("subject", message.getSubject());
        }
        headers.put("size", Integer.toString(message.getSize()));
      } catch (MessagingException e) {
        LOGGER.error("get message header failed.", e);
      }
    }
  }

  public static class Addresses {
    private Address sender;
    private Address[] from;
    private Address[] replyTo;
    private Address[] allRecipients;

    public Address getSender() {
      return sender;
    }

    public void setSender(Address sender) {
      this.sender = sender;
    }

    public Address[] getFrom() {
      return from;
    }

    public void setFrom(Address[] from) {
      this.from = from;
    }

    public Address[] getReplyTo() {
      return replyTo;
    }

    public void setReplyTo(Address[] replyTo) {
      this.replyTo = replyTo;
    }

    public Address[] getAllRecipients() {
      return allRecipients;
    }

    public void setAllRecipients(Address[] allRecipients) {
      this.allRecipients = allRecipients;
    }
  }

  public static class BodyDescriptorDto {
    private String mimeType;
    private String mediaType;
    private String subType;
    private String boundary;
    private String charset;
    private String transferEncoding;
    private long contentLength;

    public BodyDescriptorDto() {
    }

    public BodyDescriptorDto(BodyDescriptor bd) {
      this.mimeType = bd.getMimeType();
      this.mediaType = bd.getMediaType();
      this.subType = bd.getSubType();
      this.boundary = bd.getBoundary();
      this.charset = bd.getCharset();
      this.transferEncoding = bd.getTransferEncoding();
      this.contentLength = bd.getContentLength();
    }

    public String getMimeType() {
      return mimeType;
    }

    public void setMimeType(String mimeType) {
      this.mimeType = mimeType;
    }

    public String getMediaType() {
      return mediaType;
    }

    public void setMediaType(String mediaType) {
      this.mediaType = mediaType;
    }

    public String getSubType() {
      return subType;
    }

    public void setSubType(String subType) {
      this.subType = subType;
    }

    public String getBoundary() {
      return boundary;
    }

    public void setBoundary(String boundary) {
      this.boundary = boundary;
    }

    public String getCharset() {
      return charset;
    }

    public void setCharset(String charset) {
      this.charset = charset;
    }

    public String getTransferEncoding() {
      return transferEncoding;
    }

    public void setTransferEncoding(String transferEncoding) {
      this.transferEncoding = transferEncoding;
    }

    public long getContentLength() {
      return contentLength;
    }

    public void setContentLength(long contentLength) {
      this.contentLength = contentLength;
    }

  }

  public static class MailStringBody {
    private BodyDescriptorDto bodyDescriptor;
    private String body;

    public MailStringBody() {
    }

    public MailStringBody(BodyDescriptor bodyDescriptor, String body) {
      this.bodyDescriptor = new BodyDescriptorDto(bodyDescriptor);
      this.body = body;
    }

    public BodyDescriptorDto getBodyDescriptor() {
      return bodyDescriptor;
    }

    public String getBody() {
      return body;
    }
  }
}
