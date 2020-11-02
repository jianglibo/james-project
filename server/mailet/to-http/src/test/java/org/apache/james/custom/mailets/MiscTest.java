package org.apache.james.custom.mailets;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MiscTest {

  @Test
  public void t() {
    String[] array = {"a,", "b"};
    String s = Arrays.toString(array);

    assertThat(s).isEqualTo("[a,, b]");

  }
}
