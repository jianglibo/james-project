package org.apache.james.custom.mailets;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.message.SimpleContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MyHandler extends SimpleContentHandler {

  private List<StringifyHeader> parsedHeaders;
  private List<ParsedMail.MailStringBody> mailStringBody;

  public List<ParsedMail.MailStringBody> getMailStringBody() {
    return mailStringBody;
  }

  public List<StringifyHeader> getParsedHeaders() {
    return parsedHeaders;
  }


  public MyHandler() {
    parsedHeaders = new ArrayList<>();
    mailStringBody = new ArrayList<>();
  }

  @Override
  public void headers(Header header) {
    parsedHeaders.add(new StringifyHeader(header));
  }

  @Override
  public void startMultipart(BodyDescriptor bd) throws MimeException {
    super.startMultipart(bd);
  }

  @Override
  public void endMultipart() throws MimeException {
    super.endMultipart();
  }

  @Override
  public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[1024];
      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();
      byte[] byteArray = buffer.toByteArray();

      String text = new String(byteArray, StandardCharsets.UTF_8);

      mailStringBody.add(new ParsedMail.MailStringBody(bd, text));

    }
  }
}
