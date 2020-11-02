package org.apache.james.custom.mailets;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.MimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedMail {

  protected static String HEADER_SEPARATOR = ",,,";

  private static final Logger LOGGER = LoggerFactory.getLogger(ParsedMail.class);

  public static MailDto parse(MimeMessage im) throws IOException, MessagingException {
    ContentType ct = null;
    try {
      ct = new ContentType(im.getContentType());
    } catch (MessagingException e) {
      LOGGER.error("construct ContentType failed.", e);
    }
    MyHandler handler = new MyHandler(ct);
    MimeConfig config = MimeConfig.PERMISSIVE;
    MimeStreamParser parser = new MimeStreamParser(config);
    parser.setContentHandler(handler);
    parser.setContentDecoding(true);

    DecodeMonitor dm = new DecodeMonitor();

    try (InputStream is = im.getInputStream()) {
      parser.parse(is);
    } catch (MimeException e) {
      LOGGER.error("parse message failed.", e);
    }

    return new MailDto(im, handler.getMailStringBodies());
  }

}
