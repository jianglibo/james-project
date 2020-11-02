package org.apache.james.custom.mailets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.message.SimpleContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All we need is MimeMessage's header and subject and bodies.
 */
public class MyHandler extends SimpleContentHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MyHandler.class);
  private final List<MailDto.MailStringBody> mailStringBodies;

  public List<MailDto.MailStringBody> getMailStringBodies() {
    return mailStringBodies;
  }

  public MyHandler() {
    mailStringBodies = new ArrayList<>();
  }

  @Override
  public void headers(Header header) {
  }

  @Override
  public void body(BodyDescriptor bd, InputStream is) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[1024];
      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();
      byte[] byteArray = buffer.toByteArray();

      String text = new String(byteArray, StandardCharsets.UTF_8);

      mailStringBodies.add(new MailDto.MailStringBody(bd, text));
    }
  }
}
