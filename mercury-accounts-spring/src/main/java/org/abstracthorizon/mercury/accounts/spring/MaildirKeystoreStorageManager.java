/*
 * Copyright (c) 2007-2020 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.mercury.accounts.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.abstracthorizon.mercury.common.ConfigurableStorageManager;
import org.abstracthorizon.mercury.common.SimpleStorageManager;
import org.abstracthorizon.mercury.common.exception.UserRejectedException;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage manager that uses maildir java mail provider for storing messages, properties file for defining
 * storages, domains and aliases and keystore for stroring passwords.
 *
 * @author Daniel Sendula
 */
public class MaildirKeystoreStorageManager extends SimpleStorageManager
        implements ConfigurableStorageManager {

    /** Logger */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /** Path where maildir domains are stored */
    private File mailboxesPath;

    /** Path where send queue is stored */
    private File sendqueuePath;

    /** Keystore URL */
    private File keystoreFile;

    /** Timestamp when it was load last time */
    private long loadTimestamp;

    /** Keystore password */
    private String keystorePassword;

    /** Keystore type */
    private String keystoreType = KeyStore.getDefaultType();

    /** Keystore provider */
    private String keystoreProvider = "";

    /** Options */
    private Map<String, Object> options = new HashMap<String, Object>();

    private KeyStore loadedKeyStore;

    /**
     * Constructor
     */
    public MaildirKeystoreStorageManager() {
    }

    /**
     * Sets keystore password
     * @param password keystore password
     */
    public void setKeyStorePassword(String password) {
        this.keystorePassword = password;
        if (password != null) {
            options.put("keyStorePassword", password);
        } else {
            options.remove("keyStorePassword");
        }
    }

    /**
     * Keystore location
     * @param keystoreLocation keystore location
     * @throws IOException
     */
    public void setKeyStoreFile(File keystoreFile) throws IOException {
        this.keystoreFile = keystoreFile;
    }

    /**
     * Returns keystore resource
     * @return keystore resource
     */
    public File getKeyStoreFile() {
        return keystoreFile;
    }

    /**
     * Sets mailboxes path
     * @param mailboxesPath path mailboxes are defined on
     * @throws IOException
     */
    public void setMailboxesPath(File mailboxesPath) throws IOException {
        this.mailboxesPath = mailboxesPath;
    }

    /**
     * Returns mailboxes path
     * @return mailboxes path
     */
    public File getMailboxesPath() {
        return mailboxesPath;
    }

    /**
     * Sets send queue path
     * @param sendqueuePath path mailboxes are defined on
     * @throws IOException
     */
    public void setSendqueuePath(File sendqueuePath) throws IOException {
        this.sendqueuePath = sendqueuePath;
    }

    /**
     * Returns send queue path
     * @return send queue path
     */
    public File getSendqueuePath() {
        return sendqueuePath;
    }

    /**
     * Sets keystore type
     * @param type keystore type
     */
    public void setKeyStoreType(String type) {
        keystoreType = type;
        if (type != null) {
            options.put("keyStoreType", type);
        } else {
            options.remove("keyStoreType");
        }
    }

    /**
     * Returns keystore type
     * @return keystore type
     */
    public String getKeyStoreType() {
        return keystoreType;
    }

    /**
     * Sets keystore provider
     * @param provider keystore provider
     */
    public void setKeyStoreProvider(String provider) {
        this.keystoreProvider = provider;
        if (provider != null) {
            options.put("keyStoreProvider", provider);
        } else {
            options.remove("keyStoreProvider");
        }
    }

    /**
     * Returns keystore provider
     * @return keystore provider
     */
    public String getKeyStoreProvider() {
        return keystoreProvider;
    }

    /**
     * Loads keystore
     * @return keystore
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws MalformedURLException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    protected KeyStore loadKeyStore() throws KeyStoreException, NoSuchProviderException, MalformedURLException,
        IOException, NoSuchAlgorithmException, CertificateException {

        logger.debug("Loading keystore from " + keystoreFile.getAbsolutePath());
        InputStream keystoreInputStream = new FileInputStream(keystoreFile);
        try {
            KeyStore keyStore;
            if ((keystoreProvider == null) || (keystoreProvider.length() == 0)) {
                keyStore = KeyStore.getInstance(keystoreType);
            } else {
                keyStore = KeyStore.getInstance(keystoreType, keystoreProvider);
            }

            /* Load KeyStore contents from file */
            keyStore.load(keystoreInputStream, keystorePassword.toCharArray());
            return keyStore;

        } finally {
            keystoreInputStream.close();
        }
    }

    /**
     * Stores keystore back to provided resource
     * @param keystore keystore to be stored
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws MalformedURLException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    protected void storeKeyStore(KeyStore keystore) throws KeyStoreException, NoSuchProviderException, MalformedURLException,
        IOException, NoSuchAlgorithmException, CertificateException {

        logger.debug("Storing keystore to " + keystoreFile.getAbsolutePath());
        OutputStream keystoreOutputStream = new FileOutputStream(keystoreFile);

        try {
            keystore.store(keystoreOutputStream, keystorePassword.toCharArray());
        } finally {
            keystoreOutputStream.close();
        }
    }

    /**
     * Adds entry to a keystore
     * @param keyStore keystore
     * @param entry entry
     * @param password password an entry
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws InvalidKeyException
     * @throws SecurityException
     * @throws SignatureException
     * @throws NoSuchProviderException
     * @throws IllegalStateException
     * @throws CertificateEncodingException
     */
    protected void addEntryToStore(KeyStore keyStore, String entry, char[] password) throws NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, SecurityException, SignatureException, CertificateEncodingException, IllegalStateException, NoSuchProviderException {
        String name = "CN=" + entry + ", OU=, O=, L=, ST=, C=";
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

        keyStore.setKeyEntry(entry, pair.getPrivate(), password, new Certificate[]{cert});

    }

    protected String decorateStoreString(String mailbox, String domain, String storeString) {
        if ((storeString == null) || (storeString.length() == 0)) {
            File mailboxDir = ensureExists(mailbox, domain);
            storeString = "maildir://" + mailbox + "@" + domain + "/" + mailboxDir.getAbsolutePath();
        }
        return storeString;
    }

    /**
     * Ensures that mailbox (in domain) exists
     * @param mailbox mailbox
     * @param domain domain
     * @return a file of the mailbox (maildir)
     */
    protected File ensureExists(String mailbox, String domain) {
        File domainPath = null;
        if (domain != null) {
            domainPath = new File(mailboxesPath, domain);
        } else {
            domainPath = mailboxesPath;
        }
        if (!domainPath.exists()) {
            domainPath.mkdirs();
        }
        File mailboxPath = new File(domainPath, mailbox);
        if (!mailboxPath.exists()) {
            mailboxPath.mkdirs();
        }
        makeMaildirLayout(mailboxPath);
        return mailboxPath;
    }

    /**
     * Makes maildir layout of an mailbox
     * @param mailbox mailbox path
     */
    protected void makeMaildirLayout(File mailbox) {
        File inbox = new File(mailbox, ".inbox");
        File cur = new File(inbox, "cur");
        File nw = new File(inbox, "new");
        File tmp = new File(inbox, "tmp");
        inbox.mkdirs();
        cur.mkdirs();
        nw.mkdirs();
        tmp.mkdirs();
        // TODO catch problems and report them somehow
    }

    /**
     * Adds new mailbox
     * @param mailbox mailbox
     * @param domain domain
     * @param password password
     */
    public void addMailbox(String mailbox, String domain, char[] password) {
        try {
            /*File mailboxDir =*/ ensureExists(mailbox, domain);

            // String store = "maildir://" + mailbox + "@" + domain + "/" + mailboxDir.getAbsolutePath();
            super.addMailbox(makeEntry(mailbox, domain), "");

            String entry = makeEntry(mailbox, domain);

            KeyStore keyStore = loadKeyStore();
            addEntryToStore(keyStore, entry, password);
            storeKeyStore(keyStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes mailbox
     * @param mailbox mailbox
     * @param domain domain
     * @return <code>true</code> if mailbox existed
     */
    public boolean removeMailbox(String mailbox, String domain) {
        boolean res = false;
        try {
            res = super.removeMailbox(mailbox, domain);
            String entry = makeEntry(mailbox, domain);
            KeyStore keyStore = loadKeyStore();
            keyStore.deleteEntry(entry);
            storeKeyStore(keyStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    /**
     * Changes password of a mailbox. This is administrator's function since it requires only new
     * password
     *
     * @param mailbox mailbox
     * @param domain domain
     * @param newPassword new password
     */
    public void changeMailboxPassword(String mailbox, String domain, char[] newPassword) {
        changeMailboxPassword(mailbox, domain, null, newPassword);
    }

    /**
     * Changes mailboxes password. This is user's function
     * @param mailbox mailbox
     * @param domain domain
     * @param oldPassword old password
     * @param newPassword new password
     */
    public void changeMailboxPassword(String mailbox, String domain, char[] oldPassword, char[] newPassword) {
        try {
            String entry = makeEntry(mailbox, domain);
            KeyStore keyStore = loadKeyStore();
            if (oldPassword != null) {
                Key key = keyStore.getKey(entry, oldPassword);

                Certificate[] certs = keyStore.getCertificateChain(entry);

                keyStore.setKeyEntry(entry, key, newPassword, certs);
            } else {
                keyStore.deleteEntry(entry);
                addEntryToStore(keyStore, entry, newPassword);
            }
            storeKeyStore(keyStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an array of mailbox names in all domains
     * @return an array of mailbox names
     */
    public String[] getMailboxNames() {
        return super.getMailboxNames();
    }

    /**
     * Returns an array of mailbox names for given domain
     * @param domain domain
     * @return an array of mailbox names for given domain
     */
    public String[] getMailboxNames(String domain) {
        return super.getMailboxNames(domain);
    }

    /**
     * Adds new domain
     * @param domain
     */
    public void addDomain(String domain) {
        super.addDomain(domain);
    }

    /**
     * Removes domain
     * @param domain domain
     * @return <code>true</code> if domain existed
     */
    public boolean removeDomain(String domain) {
        return super.removeDomain(domain);
    }

    /**
     * Returns domains
     * @return domains
     */
    public String[] getDomains() {
        return super.getDomains();
    }

    /**
     * Sets main domain
     * @param domain
     */
    public void setMainDomain(String domain) {
        super.setMainDomain(domain);
    }

    public void authenticate(String mailbox, String domain, char[] password) {
        try {
            long timestamp = this.keystoreFile.lastModified();
            if (timestamp != this.loadTimestamp) {
                loadedKeyStore = loadKeyStore();
                this.loadTimestamp = timestamp;
            }

            String entry = makeEntry(mailbox, domain);
            Key key = loadedKeyStore.getKey(entry, password);

            if (key == null) {
                throw new UserRejectedException("User: " + makeEntry(mailbox, domain) + " cannot be authenticated");
            }
            /* byte[] encoded = */key.getEncoded();

            /* Certificate[] certs = */loadedKeyStore.getCertificateChain(entry);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new UserRejectedException("User: " + makeEntry(mailbox, domain) + " cannot be authenticated");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new UserRejectedException("User: " + makeEntry(mailbox, domain) + " cannot be authenticated");
        }
    }
}
