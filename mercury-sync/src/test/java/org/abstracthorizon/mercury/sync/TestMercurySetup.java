package org.abstracthorizon.mercury.sync;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Arrays;

import org.abstracthorizon.danube.service.server.MultiThreadServerSocketService;
import org.abstracthorizon.danube.support.logging.LoggingConnectionHandler;
import org.abstracthorizon.mercury.accounts.spring.MaildirKeystoreStorageManager;
import org.abstracthorizon.mercury.smtp.SMTPConnectionHandler;
import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.FindStorageFilter;
import org.abstracthorizon.mercury.smtp.filter.SMTPFilterCommandFactory;
import org.abstracthorizon.mercury.smtp.logging.SMTPSPAMAccessLogConnectionHandler;

public class TestMercurySetup {

    private MultiThreadServerSocketService multiThreadServerSocketService;
    private LoggingConnectionHandler loggingConnectionHandler;
    private SMTPFilterCommandFactory smtpFilterCommandFactory;
    private SMTPSPAMAccessLogConnectionHandler smtpspamAccessLogConnectionHandler;
    private int port = -1;
    private File propertiesFile;
    private File keystoreFile;
    private MercuryDirSetup mercuryDirSetup;
    private File tempDir;
    private File configDir;

    public TestMercurySetup() {
        smtpspamAccessLogConnectionHandler = new SMTPSPAMAccessLogConnectionHandler();
    }

    public void setupDir() throws IOException {
        mercuryDirSetup = new MercuryDirSetup();
        tempDir = mercuryDirSetup.create();
        mercuryDirSetup.getMailboxes().mkdir();
        System.out.println(tempDir.getAbsolutePath());

        createConfigs();

    }

    public void createConfigs() throws IOException {
        configDir = new File(tempDir, "config");
        if (!configDir.mkdir()) {
            throw new IOException("Cannot create " + configDir.getAbsolutePath());
        }

        propertiesFile = new File(configDir, "accounts.properties");
        TestUtils.writeFile(propertiesFile, "");

        keystoreFile = new File(configDir, "accounts.keystore");
        try (InputStream keystoreStream = getClass().getClassLoader().getResourceAsStream("setup/accounts.keystore")) {
            TestUtils.writeFile(keystoreFile, keystoreStream);
        }

    }

    public TestMercurySetup(File propertiesFile, File keystoreFile) {
        this.propertiesFile = propertiesFile;
        this.keystoreFile = keystoreFile;
    }

    public void create() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        MaildirKeystoreStorageManager storageManager = new MaildirKeystoreStorageManager();
        storageManager.setPropertiesFile(propertiesFile);
        storageManager.setKeyStoreFile(keystoreFile);
        storageManager.setMailboxesPath(getMailboxes());
        storageManager.setKeyStorePassword("password1234");
        storageManager.init();

        multiThreadServerSocketService = new MultiThreadServerSocketService();

        multiThreadServerSocketService.setName("smtp");
        multiThreadServerSocketService.setPort(port);
        multiThreadServerSocketService.setAddress("localhost");
        multiThreadServerSocketService.setServerSocketTimeout(1000);
        multiThreadServerSocketService.setNewSocketTimeout(60000);

        loggingConnectionHandler = new LoggingConnectionHandler();
        multiThreadServerSocketService.setConnectionHandler(loggingConnectionHandler);

        SMTPConnectionHandler smtpConnectionHandler = new SMTPConnectionHandler();
        smtpConnectionHandler.setStorageManager(storageManager);
        loggingConnectionHandler.setConnectionHandler(smtpConnectionHandler);

        smtpFilterCommandFactory = new SMTPFilterCommandFactory();
        smtpFilterCommandFactory.setFilters(Arrays.<Filter>asList(new FindStorageFilter()));
        smtpFilterCommandFactory.init();
        smtpFilterCommandFactory.setInactivityTimeout(60000);
        smtpConnectionHandler.setConnectionHandler(smtpFilterCommandFactory);

        multiThreadServerSocketService.create();
        multiThreadServerSocketService.start();

        System.out.println("Started multiThreadServerSocketService on at " + multiThreadServerSocketService.getAddress()
                + " on port " + multiThreadServerSocketService.getPort());
    }

    public void setupMailBox(String address) throws IOException {

        String[] split = address.split("@");

        if (split.length < 2) {
            throw new IOException("Invalid mailbox address: " + address);
        }
        String name = split[0];
        String domain = split[1];

        File mailbox = new File(getMailboxes(), domain);
        if (!mailbox.mkdir()) {
            throw new IOException("Cannot create " + mailbox.getAbsolutePath());
        }
        File mail = new File(mailbox, name);
        if (!mail.mkdir()) {
            throw new IOException("Cannot create " + mail.getAbsolutePath());
        }
        File inbox = new File(mail, ".inbox");
        if (!inbox.mkdir()) {
            throw new IOException("Cannot create " + inbox.getAbsolutePath());
        }

        File cur1 = new File(inbox, "cur");
        if (!cur1.mkdir()) {
            throw new IOException("Cannot create " + cur1.getAbsolutePath());
        }
        File new1 = new File(inbox, "new");
        if (!new1.mkdir()) {
            throw new IOException("Cannot create " + new1.getAbsolutePath());
        }
        File tmp1 = new File(inbox, "tmp");
        if (!tmp1.mkdir()) {
            throw new IOException("Cannot create " + tmp1.getAbsolutePath());
        }

        StringBuilder properties = new StringBuilder();

        if (getPropertiesFile().exists()) {
            String loadFile = TestUtils.loadFile(getPropertiesFile());
            properties.append(loadFile);
        }

        properties.append("-@" + domain + "=\n");
        properties.append("-" + name + "@" + domain + "=S\\=\n");
        TestUtils.writeFile(getPropertiesFile(), properties.toString());

        System.out.println(getPropertiesFile().getAbsolutePath());
    }

    public void cleanup() {
        multiThreadServerSocketService.stop();

        multiThreadServerSocketService.destroy();
        smtpFilterCommandFactory.cleanup();

    }

    public MultiThreadServerSocketService getMultiThreadServerSocketService() {
        return multiThreadServerSocketService;
    }

    public void setMultiThreadServerSocketService(MultiThreadServerSocketService multiThreadServerSocketService) {
        this.multiThreadServerSocketService = multiThreadServerSocketService;
    }

    public LoggingConnectionHandler getLoggingConnectionHandler() {
        return loggingConnectionHandler;
    }

    public void setLoggingConnectionHandler(LoggingConnectionHandler loggingConnectionHandler) {
        this.loggingConnectionHandler = loggingConnectionHandler;
    }

    public SMTPFilterCommandFactory getSmtpFilterCommandFactory() {
        return smtpFilterCommandFactory;
    }

    public void setSmtpFilterCommandFactory(SMTPFilterCommandFactory smtpFilterCommandFactory) {
        this.smtpFilterCommandFactory = smtpFilterCommandFactory;
    }

    public SMTPSPAMAccessLogConnectionHandler getSmtpspamAccessLogConnectionHandler() {
        return smtpspamAccessLogConnectionHandler;
    }

    public void setSmtpspamAccessLogConnectionHandler(
            SMTPSPAMAccessLogConnectionHandler smtpspamAccessLogConnectionHandler) {
        this.smtpspamAccessLogConnectionHandler = smtpspamAccessLogConnectionHandler;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MercuryDirSetup getMercuryDirSetup() {
        return mercuryDirSetup;
    }

    public void setMercuryDirSetup(MercuryDirSetup mercuryDirSetup) {
        this.mercuryDirSetup = mercuryDirSetup;
    }

    public File getMailboxes() {
        return mercuryDirSetup.getMailboxes();
    }

    public File getPropertiesFile() {
        return propertiesFile;
    }

    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

}
