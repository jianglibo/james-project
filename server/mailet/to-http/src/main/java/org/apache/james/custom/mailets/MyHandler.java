package org.apache.james.custom.mailets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.mail.internet.ContentType;

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

  private final Optional<ContentType> ctOp;

  public List<MailDto.MailStringBody> getMailStringBodies() {
    return mailStringBodies;
  }


  public MyHandler(ContentType ct) {
    this.ctOp = Optional.ofNullable(ct);
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
      String charset = ctOp.map(ct -> {
        String c = ct.getParameter("charset");
        if (c == null) {
          c = ct.getParameter("CHARSET");
        }
        return c;
      }).orElse("UTF-8");
      String text = new String(byteArray, charset);

      mailStringBodies.add(new MailDto.MailStringBody(bd, text));
    }
  }
}
