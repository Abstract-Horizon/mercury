package org.abstracthorizon.mercury.test;

import static java.util.Arrays.asList;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.abstracthorizon.danube.auth.jaas.keystore.KeyStoreModuleService;
import org.abstracthorizon.danube.connection.ConnectionHandler;
import org.abstracthorizon.danube.freemarker.FreeMarkerViewAdapter;
import org.abstracthorizon.danube.http.HTTPContext;
import org.abstracthorizon.danube.http.HTTPMVCConnectionHandler;
import org.abstracthorizon.danube.http.HTTPServerConnectionHandler;
import org.abstracthorizon.danube.http.Selector;
import org.abstracthorizon.danube.http.auth.JAASAuthenticator;
import org.abstracthorizon.danube.http.matcher.Matcher;
import org.abstracthorizon.danube.http.matcher.Pattern;
import org.abstracthorizon.danube.http.matcher.Prefix;
import org.abstracthorizon.danube.mvc.Controller;
import org.abstracthorizon.danube.service.server.MultiThreadServerSSLSocketService;
import org.abstracthorizon.danube.service.server.MultiThreadServerSocketService;
import org.abstracthorizon.danube.webdav.spring.SpringResourceWebDAVConnectionHandler;
import org.abstracthorizon.extend.server.auth.SpringAuthConfiguration;
import org.abstracthorizon.extend.server.deployment.DeploymentManager;
import org.abstracthorizon.extend.server.deployment.DeploymentManagerImpl;
import org.abstracthorizon.extend.server.deployment.danube.DanubeWarModuleLoader;
import org.abstracthorizon.mercury.accounts.spring.MaildirKeystoreStorageManager;
import org.abstracthorizon.mercury.adminconsole.AddAliasController;
import org.abstracthorizon.mercury.adminconsole.AddDomainController;
import org.abstracthorizon.mercury.adminconsole.AddMailboxController;
import org.abstracthorizon.mercury.adminconsole.ChangeMailboxPasswordController;
import org.abstracthorizon.mercury.adminconsole.DeleteAliasController;
import org.abstracthorizon.mercury.adminconsole.DeleteDomainController;
import org.abstracthorizon.mercury.adminconsole.DeleteMailboxController;
import org.abstracthorizon.mercury.adminconsole.IndexController;
import org.abstracthorizon.mercury.adminconsole.LogoutController;
import org.abstracthorizon.mercury.adminconsole.MailboxController;
import org.abstracthorizon.mercury.adminconsole.RequiresIndexController;
import org.abstracthorizon.mercury.adminconsole.RequiresMailboxController;
import org.abstracthorizon.mercury.adminconsole.RequiresStorageManager;
import org.abstracthorizon.mercury.filter.spam.DestinationMailboxFilter;
import org.abstracthorizon.mercury.filter.spam.FinalSPAMFilter;
import org.abstracthorizon.mercury.filter.spam.SimpleSubjectFilter;
import org.abstracthorizon.mercury.imap.IMAPConnectionHandler;
import org.abstracthorizon.mercury.smtp.SMTPConnectionHandler;
import org.abstracthorizon.mercury.smtp.filter.quiet.QuietFindStorageFilter;
import org.abstracthorizon.mercury.smtp.filter.quiet.SMTPQuietFilterCommandFactory;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.springframework.core.io.FileSystemResource;

public class CreateMailSuite implements Closeable {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private File workDir;
    private int smtpPort = 8125;
    private int imapPort = 8143;
    private int adminPort = -1;
    private int syncPort = 8146;

    private MaildirKeystoreStorageManager storageManager;
    private File configDir;
    private File deployDir;
    private File pagesDir;
    private File mercuryDataDir;
    private File mercuryDataConfigDir;
    private File mercuryDataMailboxesDir;

    private String keystorePassword = "password1234";
    private String adminConsoleKeystorePassword = "admin-console-keystore-password";

    private String serverDomain = "test.domain";
    private Map<String, String> userPasswords = new HashMap<String, String>();
    private File accountKeystoreFile;
    private File accountPropertiesFile;

    private MultiThreadServerSocketService smtpService;
    private SMTPConnectionHandler smtpConnectionHandler;
    private SMTPQuietFilterCommandFactory smtpQuietFilterCommandFactory;
    private MultiThreadServerSocketService imapService;
    private IMAPConnectionHandler imapConnectionHandler;
    private MultiThreadServerSSLSocketService danubeSSLServer;
    private File danubeSSLKeystoreFile;
    private HTTPServerConnectionHandler httpServerConnectionHandler;
    private Selector httpServerSelector;
    private HTTPContext adminHTTPContext;
    private IndexController indexController;
    private MailboxController mailboxController;
    private KeyStoreModuleService keyStoreModuleService;
    private SpringAuthConfiguration configuration;
    private JAASAuthenticator jaasAuthenticator;

    public CreateMailSuite() {
    }

    public CreateMailSuite withWorkDir(File workDir) {
        this.workDir = workDir;
        return this;
    }

    public CreateMailSuite withSMTPPort(int smtpPort) {
        this.smtpPort = smtpPort;
        return this;
    }

    public CreateMailSuite withIMAPPort(int imapPort) {
        this.imapPort = imapPort;
        return this;
    }

    public CreateMailSuite withSyncPort(int syncPort) {
        this.syncPort = syncPort;
        return this;
    }

    public CreateMailSuite withAdminPort(int adminPort) {
        this.adminPort = adminPort;
        return this;
    }

    public CreateMailSuite withServerDomain(String serverDomain) {
        this.serverDomain = serverDomain;
        return this;
    }

    public CreateMailSuite withUserWithPassword(String user, String password) {
        userPasswords.put(user,  password);
        return this;
    }

    public CreateMailSuite create() throws IOException {
        if (workDir == null) {
            workDir = File.createTempFile("mailSuite", ".dir");
            if (!workDir.delete()) {
                throw new IOException("Cannot delete temp file " + workDir.getAbsolutePath());
            }
            if (!workDir.mkdir()) {
                throw new IOException("Cannot create temp dir " + workDir.getAbsolutePath());
            }
        }
        configDir = createDir(workDir, "config");
        deployDir = createDir(workDir, "deploy");
        pagesDir = createDir(workDir, "admin-pages");
        mercuryDataDir = createDir(deployDir, "mercury-data");
        mercuryDataConfigDir = createDir(mercuryDataDir, "config");
        mercuryDataMailboxesDir = createDir(mercuryDataDir, "mailboxes");

        createAccountProperties(mercuryDataConfigDir);
        if (adminPort > 0 && !userPasswords.containsKey("admin")) {
            userPasswords.put("admin", "admin");
        }
        createAccountKeystore(mercuryDataConfigDir);

        if (storageManager == null) {
            storageManager = new MaildirKeystoreStorageManager();
        }
        storageManager.setPropertiesFile(accountPropertiesFile);
        storageManager.setKeyStoreFile(accountKeystoreFile);
        storageManager.setMailboxesPath(mercuryDataMailboxesDir);
        storageManager.setKeyStorePassword(keystorePassword);

        // Inbound SMTP

        if (smtpService == null) {
            smtpService = new MultiThreadServerSocketService();
        }
        smtpService.setName("smtp");
        smtpService.setPort(smtpPort);
        smtpService.setServerSocketTimeout(1000);
        smtpService.setNewSocketTimeout(60000);

        smtpConnectionHandler = new SMTPConnectionHandler();
        smtpService.setConnectionHandler(smtpConnectionHandler);
        smtpConnectionHandler.setStorageManager(storageManager);

        smtpQuietFilterCommandFactory = new SMTPQuietFilterCommandFactory();
        smtpConnectionHandler.setConnectionHandler(smtpQuietFilterCommandFactory);
        smtpQuietFilterCommandFactory.setInactivityTimeout(1800000);
        smtpQuietFilterCommandFactory.setMaxFlushSpeed(10240);
        smtpQuietFilterCommandFactory.setFilters(asList(
                new SimpleSubjectFilter(),
                new FinalSPAMFilter(),
                new QuietFindStorageFilter(),
                new DestinationMailboxFilter()
        ));

        // Outbound SMTP ?

        // IMAP

        imapService = new MultiThreadServerSocketService();
        imapService.setName("imap");
        imapService.setPort(8143);
        imapService.setServerSocketTimeout(1000);
        imapService.setNewSocketTimeout(60000);

        imapConnectionHandler = new IMAPConnectionHandler();
        imapService.setConnectionHandler(imapConnectionHandler);

        // Admin Console

        // -- Danube
        if (adminPort > 0) {
            danubeSSLKeystoreFile = new File(configDir, "adminSSL.keystore");
            createCertificateKeystore(danubeSSLKeystoreFile);

            danubeSSLServer = new MultiThreadServerSSLSocketService();
            danubeSSLServer.setPort(adminPort);
            danubeSSLServer.setServerSocketTimeout(1000);
            danubeSSLServer.setNewSocketTimeout(60000);
            danubeSSLServer.setKeyStorePassword(adminConsoleKeystorePassword );
            danubeSSLServer.setKeyStoreURL(danubeSSLKeystoreFile.toURI().toURL());

            httpServerConnectionHandler = new HTTPServerConnectionHandler();
            danubeSSLServer.setConnectionHandler(httpServerConnectionHandler);

            httpServerSelector = new Selector();
            httpServerConnectionHandler.setConnectionHandler(httpServerSelector);


            jaasAuthenticator = new JAASAuthenticator();
            Prefix matcher = new Prefix();
            matcher.setPrefix("/");
            matcher.setConnectionHandler(jaasAuthenticator);
            httpServerSelector.getComponents().add(matcher);
            jaasAuthenticator.setLoginContextName("mercury-admin-console-context");

            adminHTTPContext = new HTTPContext();
            jaasAuthenticator.setHandler(adminHTTPContext);

            SpringResourceWebDAVConnectionHandler springResourceWebDAVConnectionHandler = new SpringResourceWebDAVConnectionHandler();
            springResourceWebDAVConnectionHandler.setResourcePath(new FileSystemResource(pagesDir));
            springResourceWebDAVConnectionHandler.setReadOnly(true);

            URL pageIndexURL = getClass().getResource("/pages/index.html");
            String pagesIndedxURLString = pageIndexURL.toString();
            pageIndexURL = new URL(pagesIndedxURLString.substring(0, pagesIndedxURLString.length() - 11));

            FreeMarkerViewAdapter freeMarkerViewAdapter = new FreeMarkerViewAdapter();
            freeMarkerViewAdapter.setTemplatesURL(pageIndexURL);
            freeMarkerViewAdapter.setSuffix("page");
            freeMarkerViewAdapter.init();

            indexController = new IndexController();
            mailboxController = new MailboxController();

            adminHTTPContext.setComponents(asList(
                pattern("(/style\\.css)|(/images/.*)|(/favicon\\.ico)", springResourceWebDAVConnectionHandler, false),
                prefix("logout", new LogoutController(), freeMarkerViewAdapter),
                prefix("/add_domain", new AddDomainController(), freeMarkerViewAdapter),
                prefix("/delete_domain", new DeleteDomainController(), freeMarkerViewAdapter),
                prefix("/add_mailbox", new AddMailboxController(), freeMarkerViewAdapter),
                prefix("/delete_mailbox", new DeleteMailboxController(), freeMarkerViewAdapter),
                prefix("/mailbox", mailboxController, freeMarkerViewAdapter),
                prefix("/password", new ChangeMailboxPasswordController(), freeMarkerViewAdapter),
                prefix("/add_alias", new AddAliasController(), freeMarkerViewAdapter),
                prefix("/delete_alias", new DeleteAliasController(), freeMarkerViewAdapter),
                prefix("/", indexController, freeMarkerViewAdapter)
            ));

             configuration = new SpringAuthConfiguration();
             configuration.init();

             keyStoreModuleService = new KeyStoreModuleService();
             keyStoreModuleService.setLoginContext("mercury-admin-console-context");
             keyStoreModuleService.setKeyStoreFile(accountKeystoreFile);
             keyStoreModuleService.setKeyStorePassword(keystorePassword);
             keyStoreModuleService.setConfiguration(configuration);
        }

        // Sync Server

        // Sync Client


        // Start methods

        smtpService.create();
        // smtpConnectionHandler.create();
        // smtpQuietFilterCommandFactory.create();
        imapService.create();
        // imapConnectionHandler.create();

        if (adminPort > 0) {
            danubeSSLServer.create();
        }

        return this;
    }

    private Matcher prefix(String prefixString, Controller controller, FreeMarkerViewAdapter freeMarkerViewAdapter) {
        Prefix prefix = new Prefix();
        prefix.setPrefix(prefixString);
        HTTPMVCConnectionHandler connectionHandler = new HTTPMVCConnectionHandler();
        connectionHandler.setController(controller);
        connectionHandler.setView(freeMarkerViewAdapter);

        if (controller instanceof RequiresIndexController) {
            ((RequiresIndexController)controller).setIndexController(indexController);
        }

        if (controller instanceof RequiresMailboxController) {
            ((RequiresMailboxController)controller).setMailboxController(mailboxController);
        }

        if (controller instanceof RequiresStorageManager) {
            ((RequiresStorageManager)controller).setStorageManager(storageManager);
        }

        prefix.setConnectionHandler(connectionHandler);
        return prefix;
    }

    private Matcher pattern(String patternString, ConnectionHandler connectionHandler, boolean matchAsComponentPath) {
        Pattern pattern = new Pattern();
        pattern.setPattern(patternString);
        pattern.setConnectionHandler(connectionHandler);
        pattern.setMatchAsComponentPath(matchAsComponentPath);
        return pattern;
    }

    public void start() throws Exception {
        smtpService.start();
        // smtpConnectionHandler.start();
        // smtpQuietFilterCommandFactory.start();
        imapService.start();
        // imapConnectionHandler.start();

        if (adminPort > 0) {
            danubeSSLServer.start();
            keyStoreModuleService.start();
        }

    }

    public void stop() throws Exception {
        if (adminPort > 0) {
            keyStoreModuleService.stop();
            danubeSSLServer.stop();
        }

        // imapConnectionHandler.stop();
        imapService.stop();
        // smtpQuietFilterCommandFactory.stop();
        // smtpConnectionHandler.stop();
        smtpService.stop();
    }

    public void destroy() {
        if (adminPort > 0) {
            danubeSSLServer.destroy();
        }

        // imapConnectionHandler.destroy();
        imapService.destroy();
        // smtpQuietFilterCommandFactory.destroy();
        // smtpConnectionHandler.destroy();
        smtpService.destroy();
    }

    private void createAccountKeystore(File mercuryDataConfigDir) throws IOException {
        accountKeystoreFile = new File(mercuryDataConfigDir, "accounts.keystore");
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null, null);

            for (Map.Entry<String, String> entry : userPasswords .entrySet()) {
                String name = "CN=" + entry.getKey() + ", OU=, O=, L=, ST=, C=";
                //CN=Me, OU=Java Card Development, O=MyFirm, C=UK, ST=MyCity";

                Security.addProvider(new BouncyCastleProvider());

                X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
                KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
                kpGen.initialize(1024);
                KeyPair pair = kpGen.generateKeyPair();

                generator.setSerialNumber(BigInteger.valueOf(1));
                generator.setIssuerDN(new X509Principal(name));
                generator.setNotBefore(new Date());
                generator.setNotAfter(new Date(System.currentTimeMillis()+1000*60*60*24*365)); // a year
                generator.setSubjectDN(new X509Principal(name));
                generator.setPublicKey(pair.getPublic());
                generator.setSignatureAlgorithm("MD5WithRSAEncryption");

                Certificate cert = generator.generate(pair.getPrivate(), "BC");

                keystore.setKeyEntry(entry.getKey(), pair.getPrivate(), entry.getValue().toCharArray(), new Certificate[]{cert});
            }

            try (OutputStream keystoreOutputStream = new FileOutputStream(accountKeystoreFile)) {
                keystore.store(keystoreOutputStream, keystorePassword.toCharArray());
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (CertificateException e) {
            throw new IOException(e);
        } catch (KeyStoreException e) {
            throw new IOException(e);
        } catch (InvalidKeyException e) {
            throw new IOException(e);
        } catch (IllegalStateException e) {
            throw new IOException(e);
        } catch (NoSuchProviderException e) {
            throw new IOException(e);
        } catch (SignatureException e) {
            throw new IOException(e);
        }
    }

    private void createCertificateKeystore(File keystoreFile) throws IOException {
        try {
            String domainName = "localhost";

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null, null);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair KPair = keyPairGenerator.generateKeyPair();

            X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

            v3CertGen.setSerialNumber(BigInteger.valueOf(Math.abs(new SecureRandom().nextInt())));
            v3CertGen.setIssuerDN(new X509Principal("CN=" + domainName + ", OU=None, O=None L=None, C=None"));
            v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
            v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365*10)));
            v3CertGen.setSubjectDN(new X509Principal("CN=" + domainName + ", OU=None, O=None L=None, C=None"));

            v3CertGen.setPublicKey(KPair.getPublic());
            v3CertGen.setSignatureAlgorithm("MD5WithRSAEncryption");

            X509Certificate PKCertificate = v3CertGen.generateX509Certificate(KPair.getPrivate());

            keystore.setKeyEntry("sslCert", KPair.getPrivate(),
                    adminConsoleKeystorePassword.toCharArray(),
                    new java.security.cert.Certificate[]{PKCertificate});

            try (OutputStream keystoreOutputStream = new FileOutputStream(keystoreFile)) {
                keystore.store(keystoreOutputStream, adminConsoleKeystorePassword.toCharArray());
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (CertificateException e) {
            throw new IOException(e);
        } catch (KeyStoreException e) {
            throw new IOException(e);
        } catch (InvalidKeyException e) {
            throw new IOException(e);
        } catch (IllegalStateException e) {
            throw new IOException(e);
        } catch (SignatureException e) {
            throw new IOException(e);
        }
    }

    private void createAccountProperties(File mercuryDataConfigDir) throws IOException {
        accountPropertiesFile = new File(mercuryDataConfigDir, "accounts.properties");
        try (FileWriter fileWriter = new FileWriter(accountPropertiesFile);
                PrintWriter out = new PrintWriter(fileWriter)) {

            out.println("server.domain = " + serverDomain);
            out.println("-@s" + serverDomain + "=");

        }
    }

    private File createDir(File parent, String name) throws IOException {
        File dir = new File(parent, name);
        if (!dir.mkdirs()) {
            throw new IOException("Failed to create dir " + dir.getAbsolutePath());
        }
        return dir;
    }

    @Override
    public void close() throws IOException {
        if (workDir != null) {
            deleteRecursively(workDir);
            if (!workDir.delete()) {
                throw new IOException("Cannot delete temp dir " + workDir.getAbsolutePath());
            }
        }
    }

    private void deleteRecursively(File dir) {
        for (File f : dir.listFiles()) {
            if (!f.getName().equals(".") && !f.getName().equals("..")) {
                if (f.isFile()) {
                    if (!f.delete()) {
                        System.err.println("Failed to remove file  " + f.getAbsolutePath());
                    }
                } else if (f.isDirectory()) {
                    deleteRecursively(f);
                    if (!f.delete()) {
                        System.err.println("Failed to remove dir   " + f.getAbsolutePath());
                    }
                } else {
                    if (!f.delete()) {
                        System.err.println("Failed to remove other " + f.getAbsolutePath());
                    }
                }
            }
        }
    }

    public File getWorkDir() {
        return workDir;
    }

    public static void main(String[] args) throws Exception {
        try (CreateMailSuite mailSuite = new CreateMailSuite()){
            mailSuite
                .withAdminPort(8443)
                .create();


            mailSuite.start();

            System.out.println("Workdir " + mailSuite.getWorkDir());
            while (true) {
                Thread.sleep(1);
            }
        }
    }
}
