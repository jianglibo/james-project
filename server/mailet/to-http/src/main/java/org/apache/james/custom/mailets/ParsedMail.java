package org.apache.james.custom.mailets;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.MimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;

public class ParsedMail {

  protected static String HEADER_SEPARATOR = ",,,";

  private static final Logger LOGGER = LoggerFactory.getLogger(ParsedMail.class);

  public static MailDto parse(MimeMessage im) throws IOException, MessagingException {
    MyHandler handler = new MyHandler();
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
