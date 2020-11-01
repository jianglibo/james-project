package org.apache.james.custom.mailets;

import java.util.HashMap;
import java.util.Map;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.stream.Field;

public class StringifyHeader {
  private Map<String, String> itemMap;

  public StringifyHeader(Header header) {
    parseHeader(header);
  }

  public Map<String, String> getItemMap() {
    return itemMap;
  }

  private void parseHeader(Header header) {
    itemMap = new HashMap<>();
    for (Field f : header.getFields()
    ) {
      String name = f.getName();
      String value = f.getBody();
      itemMap.compute(name, (k, v) -> v == null ? value : v.concat(ParsedMail.HEADER_SEPARATOR).concat(value));
    }
  }
}
