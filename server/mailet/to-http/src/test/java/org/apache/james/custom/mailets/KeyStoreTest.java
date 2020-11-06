package org.apache.james.custom.mailets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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

  //   mvn -Dtest=TestCi*le test --file server/mailet/to-http/pom.xml
  @Test
  public void tls() {
        SSLContext context = null;
        try {
            KeyStore keyStore = KeyStore.getInstance("pkcs12");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            trustManagerFactory.init(keyStore);
            TrustManager[] trustAllCerts = trustManagerFactory.getTrustManagers();
            context = SSLContext.getInstance("TLSv1.3");
            context.init(null, trustAllCerts, new SecureRandom());

            SSLParameters params = context.getSupportedSSLParameters();
            String[] protocols = params.getProtocols();
            System.out.println("Java version : " + System.getProperty("java.runtime.version"));
            boolean supportsTLSv13 = false;
            for (String protocol : protocols) {
                if ("TLSv1.3".equals(protocol)) {
                    supportsTLSv13 = true;
                    break;
                }
            }
            if(supportsTLSv13) {
                System.out.println("JRE supports TLS v1.3!");
            } else {
                System.out.println("JRE does NOT support TLS v1.3!");
            }
            String[] suites = params.getCipherSuites();
            System.out.println("A total of " + suites.length + " TLS cipher suites is supported.");

        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
            System.exit(42);
        }
  }
}
