package org.apache.james.custom.mailets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.Test;


public class KeyStoreTest {

  @Test
  public void t() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {

    KeyStore ks = KeyStore.getInstance("JKS");

    File f = new File("G:\\james-server-app-3.5.0-app\\james-server-app-3.5.0\\conf\\keystore");
    long l = f.length();

//    InputStream is = ClassLoader.getSystemResourceAsStream("conf/keystore");
    InputStream is = new FileInputStream(f);
//    InputStream is = ClassLoader.getSystemResourceAsStream("mime/cc.mime");
    ks.load(is, "yoursecret".toCharArray());

    //Get the System Classloader
    ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();

    //Get the URLs
    URL[] urls = ((URLClassLoader) sysClassLoader).getURLs();

    for (int i = 0; i < urls.length; i++) {
      System.out.println(urls[i].getFile());
    }

  }
}
