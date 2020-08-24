package org.abstracthorizon.mercury.test;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.abstracthorizon.danube.http.util.Base64;

public class AdminConsoleAdapter {

    private int port;
    private String user = "admin";
    private String password = "admin";
    private MailSuite owner;

    public AdminConsoleAdapter(MailSuite owner) {
        this.owner = owner;
        this.port = owner.getAdminPort();
    }

    public AdminConsoleAdapter withUserAndPassword(String user, String password) {
        this.user  = user;
        this.password = password;
        return this;
    }

    private HttpsURLConnection decorateConnection(URLConnection urlConnection) throws IOException {
        try {
            HttpsURLConnection connection = (HttpsURLConnection)urlConnection;

            TrustManager[] trustAllCerts = new TrustManager[] { new X509ExtendedTrustManager() {

                @Override public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }

                @Override public void checkClientTrusted(X509Certificate[] certs, String authType, Socket socket) throws CertificateException {
                }

                @Override public void checkClientTrusted(X509Certificate[] certs, String authType, SSLEngine sslEngine) throws CertificateException {
                }

                @Override public void checkServerTrusted(X509Certificate[] certs, String authType, Socket socket) throws CertificateException {
                }

                @Override public void checkServerTrusted(X509Certificate[] certs, String authType, SSLEngine sslEngine) throws CertificateException {
                }
            } };

            SSLContext sc = SSLContext.getInstance("TLSv1");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier acceptAllHostnames = new HostnameVerifier() {
                @Override public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            connection.setHostnameVerifier(acceptAllHostnames);

            connection.setRequestProperty("Authorization", "Basic " + Base64.encode(user + ":" + password));

            return connection;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (KeyManagementException e) {
            throw new IOException(e);
        }
    }

    private void collectResult(BufferedReader reader, List<String> output) throws IOException {
        String line = reader.readLine();
        while (line != null) {
            if (output != null) {
                output.add(line);
            }
            line = reader.readLine();
        }
    }

    private void collectResult(HttpsURLConnection connection, List<String> output) throws IOException {
        try (InputStream inputStream = connection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader)) {
            collectResult(reader, output);
        }
    }

    public void addDomain(String domainName, List<String> output) throws IOException {
        URL url = new URL("https://localhost:" + port + "/add_domain?domain=" + domainName);

        try (WaitForModified propertiesModified = new WaitForModified(owner.getAccountPropertiesFile())) {

            owner.getAccountPropertiesFile();
            owner.getAccountKeystoreFile();
            collectResult(decorateConnection(url.openConnection()), output);
        }
    }

    public void addMailbox(String domainName, String mailbox, String password, List<String> output) throws IOException {
        URL url = new URL("https://localhost:" + port + "/add_mailbox?domain=" + domainName + "&mailbox=" + mailbox + "&password=" + password + "&password2="+ password);

        try (WaitForModified propertiesModified = new WaitForModified(owner.getAccountPropertiesFile());
                WaitForModified keystoreModified = new WaitForModified(owner.getAccountKeystoreFile())) {

            collectResult(decorateConnection(url.openConnection()), output);
        }
    }

    public void changePassword(String domainName, String mailbox, String oldPassword, String newPassword) throws IOException {
        URL url = new URL("https://localhost:" + port + "/password?mailbox=" + mailbox + "&domain=" + domainName + "&oldpassword=" + oldPassword +  "&password=" + newPassword + "&password2="+ newPassword);

        try (WaitForModified keystoreModified = new WaitForModified(owner.getAccountKeystoreFile())) {

            collectResult(decorateConnection(url.openConnection()), new ArrayList<>());
        }
    }

    public static class WaitForModified implements Closeable {
        private File f;
        private long timestamp;
        private long timeout;

        public WaitForModified(File f) {
            this(f, 2000);
        }

        public WaitForModified(File f, long timeout) {
            this.f = f;
            this.timeout = timeout;
            this.timestamp = f.lastModified();
            if (this.timestamp % 1000 == 0) {
                // If FS timestamp resolution is at 1s then need to wait until next second
                while (this.timestamp + 1000 > System.currentTimeMillis()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {}
                }
            }
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public void close() {
            waitForModified();
        }

        public void waitForModified() {
            waitForModified(timeout);
        }

        public void waitForModified(long timeout) {
            long now = System.currentTimeMillis();
            while (System.currentTimeMillis() < now + timeout) {
                if (f.lastModified() > timestamp) {
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {}
            }
            throw new AssertionError("File " + f.getAbsolutePath() + " has not changed within " + timeout + "ms");
        }
    }
}
