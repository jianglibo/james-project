package org.apache.james.custom.mailets;

import org.apache.james.util.MimeMessageUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

class ParsedMailTest {

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void t() throws MessagingException, IOException {
    MimeMessage mm = MimeMessageUtil.mimeMessageFromStream(
//            ClassLoader.getSystemResourceAsStream("mime/sendToRemoteHttp.mime"));
    ClassLoader.getSystemResourceAsStream("mime/gmail.mime"));
    MailDto mailDto = ParsedMail.parse(mm);

    assertThat("a").hasSize(1);
  }


  @Test
  void tJapanese() throws MessagingException, IOException {
    MimeMessage mm = MimeMessageUtil.mimeMessageFromStream(
//            ClassLoader.getSystemResourceAsStream("mime/sendToRemoteHttp.mime"));
            ClassLoader.getSystemResourceAsStream("mime/japanese.mime"));
    MailDto mailDto = ParsedMail.parse(mm);

    assertThat("a").hasSize(1);
  }
  @Test
  void tJapaneseShift() throws MessagingException, IOException {
    MimeMessage mm = MimeMessageUtil.mimeMessageFromStream(
//            ClassLoader.getSystemResourceAsStream("mime/sendToRemoteHttp.mime"));
            ClassLoader.getSystemResourceAsStream("mime/japanese_shift_js.mime"));
    MailDto mailDto = ParsedMail.parse(mm);

    assertThat("a").hasSize(1);
  }

  @Test
  void tChinese() throws MessagingException, IOException {
    MimeMessage mm = MimeMessageUtil.mimeMessageFromStream(
//            ClassLoader.getSystemResourceAsStream("mime/sendToRemoteHttp.mime"));
            ClassLoader.getSystemResourceAsStream("mime/chinese.eml"));
    MailDto mailDto = ParsedMail.parse(mm);

    assertThat("a").hasSize(1);
  }
}