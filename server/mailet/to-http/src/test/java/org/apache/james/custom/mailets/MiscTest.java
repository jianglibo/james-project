package org.apache.james.custom.mailets;

import java.util.Arrays;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static org.assertj.core.api.Assertions.assertThat;

public class MiscTest {

  @Test
  public void t() {
    String[] array = {"a,", "b"};
    String s = Arrays.toString(array);

    assertThat(s).isEqualTo("[a,, b]");

  }

  @Test
  public void tAddress() throws AddressException, JsonProcessingException {
    InternetAddress[] iad = InternetAddress.parse("=?UTF-8?Q?Beno=c3=aet_TELLIER?= <tellier@linagora.com>");
    ObjectMapper om = new ObjectMapper();
    om.enable(SerializationFeature.INDENT_OUTPUT);

    String s = om.writeValueAsString(iad);
    Address ad = new Address() {
      @Override
      public String getType() {
        return null;
      }

      @Override
      public String toString() {
        return null;
      }

      @Override
      public boolean equals(Object o) {
        return false;
      }
    };
  }
}
