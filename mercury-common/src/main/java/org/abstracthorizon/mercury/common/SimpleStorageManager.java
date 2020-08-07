/*
 * Copyright (c) 2004-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.abstracthorizon.danube.support.RuntimeIOException;
import org.abstracthorizon.mercury.common.exception.UnknownUserException;
import org.abstracthorizon.mercury.common.exception.UserRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class is simple implementation of SMTPManager that uses
 * properties file to read (and write) domain names to served,
 * mail boxes that exist and aliases that point to existing mailboxes.
 * </p>
 * <p>
 * Format of properties file is as described in {@link Properties} and
 * this class uses special prefixes and property names to denote domain names, mailboxes
 * and aliases:
 * </p>
 * <ul>
 * <li><i>server.domain</i> - main domain name to appear in response to HELO/EHLO command</li>
 * <li><i>-@domainname</i> - defines served domain. Main domain must appear in this form as well.
 *                    Also this can point to a mailbox/alias (key's value) and then it will act as
 *                    &quot;catch all&quot; account. In case value of this key is empty there
 *                    is no catch all account.</li>
 * <li><i>-mailbox@domain</i> - this is standard entry for mailbox/alias. Value of such key
 *                              defines what does it represent:
 *   <ul>
 *     <li><i>S=</i> - a mail box. Rest of the string represents full URL as used to obtain
 *                     JavaMail storage.
 *     </li>
 *     <il><i>A=</i> - an alias. It contains full e-mail address of a mailbox as defined before (not the URL).</li>
 *   </ul>
 *   Note: these two appear in a properties file like <code>&quot;S\=&quot</code> and <code>&quot;A\=&quot;</code>.
 * </li>
 * </ul>
 *
 *
 * @author Daniel Sendula
 */
public class SimpleStorageManager implements StorageManager {

    /** Mail box is case sensitive flag */
    protected boolean caseSensitive = false;

    /** Properties to keep all elements in */
    protected Properties props = new Properties();

    /** Maildir session to work  in */
    protected Session session;

    /** Properties file */
    protected File propertiesFile;

    /** Shell changes in properties automatically trigger saving values back to the file */
    protected boolean autosave = true;

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(SimpleStorageManager.class);

    /**
     * Constructor
     */
    public SimpleStorageManager() {
    }

    /**
     * Setter for file
     * @param propertiesFile file
     */
    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    /**
     * Returns properties file
     * @return properties file
     */
    public File getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Sets autosave flag
     * @param autosave autosave flag
     * @see #autosave
     */
    public void setAutosave(boolean autosave) {
        this.autosave = autosave;
    }

    /**
     * Returns autosave flag
     * @return autosave flag
     * @see #autosave
     */
    public boolean isAutosave() {
        return autosave;
    }

    /**
     * Sets case sensitive flag
     * @param caseSensitive case sensitive
     * @see #caseSensitive
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        if (!caseSensitive) {
            toLowerCase();
        }
    }

    /**
     * Returns case senstive
     * @return case sensitive
     * @see #caseSensitive
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Returns java mail session
     * @return java mail session
     */
    public Session getJavaMailSession() {
        return session;
    }

    /**
     * Sets java mail session
     * @param session java mail session
     */
    public void setJavaMailSession(Session session) {
        this.session = session;
    }


    /**
     * This method loads properties and sets the session if not already defined
     * @throws IOException
     */
    public void init() throws IOException {
        load();
        if (session == null) {
            session = Session.getDefaultInstance(new Properties());
        }
    }

    /**
     * This method loads the properties
     * @throws IOException
     */
    public void load() throws IOException {
        InputStream fis = getPropertiesInputStream();
        try {
            props.load(fis);
        } finally {
            fis.close();
        }
        if (!caseSensitive) {
                toLowerCase();
        }
    }

    /**
     * This method saves the properties
     * @throws IOException
     */
    public void save() {
        try {
            OutputStream fos = getPropertiesOutputStream();
            if (fos != null) {
                try {
                    props.store(fos, "# SMTP Manager data");
                } finally {
                    fos.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    /**
     * Returns input stream to read properties from
     * @return input stream to read properties from
     * @throws IOException
     */
    protected InputStream getPropertiesInputStream() throws IOException {
        return new FileInputStream(propertiesFile);
    }

    /**
     * Returns output stream to write properties to
     * @return output stream to write properties to
     * @throws IOException
     */
    protected OutputStream getPropertiesOutputStream() throws IOException {
        return new FileOutputStream(propertiesFile);
    }

    /**
     * Returns main domain as it appears in HELO/EHLO command response
     * @return main domain
     */
    public String getMainDomain() {
        return props.getProperty("server.domain");
    }

    /**
     * Sets main domain name
     * @param domain main domain name
     */
    public void setMainDomain(String domain) {
        props.setProperty("server.domain", domain);
    }

    /**
     * Returns <code>true</code> if domain is served by this server
     * @param <code>true</code> if domain is served by this server
     */
    public boolean hasDomain(String domain) {
        if (!caseSensitive) {
            domain = domain.toLowerCase();
        }
        if (domain.equals(getMainDomain())) {
            return true;
        }
        return props.getProperty("-@" + domain) != null;
    }


    public Store findStore(String mailbox, String domain, char[] password) throws UserRejectedException, MessagingException {

        Folder folder = findInbox(mailbox, domain, password);

        authenticate(mailbox, domain, password);

        return folder.getStore();
    }

    /**
     * Updates path parameter with local mailbox (folder from JavaMail if mailbox/alias exists.
     * @param mailbox mailbox to be accesses
     * @param domain domain
     * @param domain domain where mailbox is supposed ot be defined.
     * @throws UserRejectedException thrown in user is rejected
     * @throws MessagingException if error happens while accessing the folder
     */
    public Folder findInbox(String mailbox, String domain, char[] password) throws UserRejectedException, MessagingException {
        if (domain == null) {
            domain = getMainDomain();
        }
        if (!caseSensitive) {
            mailbox = mailbox.toLowerCase();
            if (domain != null) {
                domain = domain.toLowerCase();
            }
        }

        String storeString = obtainStoreString(mailbox, domain);
        if (storeString != null) {
            URLName urlName = null;
            if (password != null) {
                urlName = createURLName(mailbox, new String(password), storeString);
            } else {
                urlName = createURLName(mailbox, null, storeString);
            }

            try {
                if (password != null) {
                    PasswordAuthentication auth = new PasswordAuthentication(mailbox, new String(password));
                    session.setPasswordAuthentication(urlName, auth);
                }
                Store store = obtainStore(urlName);

                store.connect();

                String ref = urlName.getRef();
                if (ref != null) {
                    return store.getFolder(ref);
                } else {
                    return store.getFolder("INBOX");
                }
            } catch (NoSuchProviderException e) {
                // try for catch all, but only in case that mail wasn't directed or redirected to postmaster and
                // that postmaster exists on final domain after resolving aliases
                if (!mailbox.equals("postmaster") && (props.getProperty("-" + makeEntry("postmaster", domain)) != null)) {
                    return findInbox("postmaster", domain, null);
                } else {
                    throw new UnknownUserException("User: " + makeEntry(mailbox, domain) + " now known", e);
                }
            }
        }

        throw new UnknownUserException("User: " + makeEntry(mailbox, domain) + " now known");
    }

    public void authenticate(String mailbox, String domain, char[] password) {
        // throw new UserRejectedException("User: " + makeEntry(mailbox, domain) + " cannot be authenticated");
    }

    protected String obtainStoreString(String mailbox, String domain) throws MessagingException {

        String storeString = null;
        String mb = mailbox;
        String dn = domain;
        if (dn == null) {
            dn = "";
        }

        String map = props.getProperty("-" + makeEntry(mb, dn));
        if (map == null) {
            map = props.getProperty("-" + makeEntry("", dn));
        }
        while ((map != null) && (storeString == null)) {
            if (map.startsWith("S=")) {
                storeString = map.substring(2);
                storeString = decorateStoreString(mb, dn, storeString);
            } else if (map.startsWith("A=")) {
                int i = map.indexOf('@');
                if (i >= 0) {
                    String t = map.substring(2, i);
                    if (t.length() > 0) {
                        mb = t;
                    }
                    dn = map.substring(i+1);
                } else {
                    throw new MessagingException("Configuration error; expecting '@' in map='" + map + "'");
                }
            } else{
                mb = "";
                dn = "";
            }

            map = props.getProperty("-" + makeEntry(mb, dn));
            if (map == null) {
                map = props.getProperty("-" + makeEntry("", dn));
            }
        }
        return storeString;
    }

    protected String decorateStoreString(String mailbox, String domain, String storeString) {
        return storeString;
    }

    protected Store obtainStore(URLName urlName) throws MessagingException {
        // TODO Replacing alias with actual store
        // urlName = new URLName(urlName.getProtocol(), urlName.getHost(), urlName.getPort(), urlName.getFile(), mb, null);

        Store store = session.getStore(urlName);
        return store;
    }

    protected URLName createURLName(String username, String password, String storeString) {
        URLName urlName = new URLName(storeString);

        urlName = new URLName(urlName.getProtocol(), urlName.getHost(), urlName.getPort(), urlName.getRef() == null ? urlName.getFile() : urlName.getFile() + "#" + urlName.getRef(), username, password);
        return urlName;
    }

    /**
     * Utility method to add new mailbox to this manager. It saves properties if {@link #autosave} is on.
     *
     * @param mailbox mailbox (name@domain).
     * @param store url
     * @throws IOException
     */
    public void addMailbox(String mailbox, String store) {
        if (!caseSensitive) {
            mailbox = mailbox.toLowerCase();
        }
        props.setProperty("-" + mailbox, "S=" + store);
        if (autosave) { save(); }
    }

    /**
     * Utility method that adds new alias to this manager. It saves properties if {@link #autosave} is on.
     * @param mailbox mailbox (name@domain)
     * @param destMailbox destination mailbox (name@domain)
     * @throws IOException
     */
    public void addAlias(String mailbox, String destMailbox) throws IOException {
        if (!caseSensitive) {
            mailbox = mailbox.toLowerCase();
        }
        props.setProperty("-"+mailbox, "A="+destMailbox);
        if (autosave) { save(); }
    }

    /**
     * Removes the alias. It won't remove anything else.
     * @param mailbox removes the alias
     */
    public void removeAlias(String mailbox) {
        if (!caseSensitive) {
            mailbox = mailbox.toLowerCase();
        }
        String property = props.getProperty("-" + mailbox);
        if ((property != null) && property.startsWith("A=")) {
            props.remove("-" + mailbox);
        }
    }

    /**
     * Adds new domain to this manager. It saves properties if {@link #autosave} is on.
     * @param domain domain name.
     * @throws IOException
     */
    public void addDomain(String domain) {
        if (!caseSensitive) {
            domain = domain.toLowerCase();
        }
        if (props.getProperty("-" + makeEntry("", domain)) == null) {
            props.setProperty("-" + makeEntry("", domain), "R");
            if (autosave) { save(); }
        }
    }

    /**
     * Adds new &quot;raw&quot; entry to properties.
     *
     * @param mailbox key that is automatically prefixed with &quot;-&quot;
     * @param entry entry
     * @throws IOException
     */
    public void addEntry(String mailbox, String entry) throws IOException {
        if (!caseSensitive) {
            mailbox = mailbox.toLowerCase();
        }
        props.setProperty("-"+mailbox, entry);
        if (autosave) { save(); }
    }

    /**
     * Removes mailbox
     * @param mailbox mailbox
     * @throws IOException
     */
    public boolean removeMailbox(String mailbox, String domain) {
        boolean res = (props.remove("-" + makeEntry(mailbox, domain)) != null);

        if (autosave) { save(); }
        return res;
    }

    /**
     * Deletes domain
     * @param domain domain name
     * @throws IOException
     */
    public boolean removeDomain(String domain) {
        boolean res = (props.remove("-" + makeEntry("", domain)) != null);
        if (autosave) { save(); }
        return res;
    }

    /**
     * Returns all domains that are served
     * @return all domains that are served
     */
    public String[] getDomains() {
        List<String> list = new ArrayList<String>();
        Iterator<Object> it = props.keySet().iterator();
        while (it.hasNext()) {
            String s = (String)it.next();
            if (s.startsWith("-@")) {
                list.add(s.substring(2));
            }
        }
        String[] res = new String[list.size()];
        return list.toArray(res);
    }

    /**
     * Returns all mailbox urls
     * @return mailbox urls
     */
    public String[] getMailboxNames() {
        List<String> list = new ArrayList<String>();
        Iterator<Object> it = props.keySet().iterator();
        while (it.hasNext()) {
            String s = (String)it.next();
            if (s.startsWith("-")) {
                String e = props.getProperty(s);
                if (e.startsWith("S=")) {
                    s = s.substring(1);
                    list.add(s);
                }
            }
        }
        String[] res = new String[list.size()];
        return list.toArray(res);
    }

    /**
     * Returns all mailbox urls
     * @param domain domain name
     * @return mailbox urls
     */
    public String[] getMailboxNames(String domain) {
        List<String> list = new ArrayList<String>();
        Iterator<Object> it = props.keySet().iterator();
        while (it.hasNext()) {
            String s = (String)it.next();
            if (s.startsWith("-")) {
                String e = props.getProperty(s);
                if (e.startsWith("S=")) {
                    String d = "";
                    int i = s.indexOf('@');
                    if (i >= 1) {
                        d = s.substring(i+1);
                        s = s.substring(1, i);
                    } else {
                        s = s.substring(1);
                    }
                    if (domain.equals(d)) {
                        list.add(s);
                    }
                }
            }
        }
        String[] res = new String[list.size()];
        return list.toArray(res);
    }

    /**
     * Returns all aliases
     * @return all aliases
     */
    public String[] getAliases() {
        List<String> list = new ArrayList<String>();
        Iterator<Object> it = props.keySet().iterator();
        while (it.hasNext()) {
            String s = (String)it.next();
            if (s.startsWith("-")) {
                String e = props.getProperty(s);
                if (e.startsWith("A=")) {
                    list.add(s.substring(1)+"="+e.substring(2));
                }
            }
        }
        String[] res = new String[list.size()];
        return list.toArray(res);
    }

    /**
     * Returns all aliases
     * @return all aliases
     */
    public String[] getAliases(String mailbox, String domain) {
        if (domain != null && !domain.equals("")) {
            mailbox = mailbox + "@" + domain;
        }

        List<String> list = new ArrayList<String>();
        Iterator<Object> it = props.keySet().iterator();
        while (it.hasNext()) {
            String s = (String)it.next();
            if (s.startsWith("-")) {
                String e = props.getProperty(s);
                if (e.equals("A=" + mailbox)) {
                    list.add(s.substring(1));
                }
            }
        }
        String[] res = new String[list.size()];
        return list.toArray(res);
    }

    /**
     * Converts all entries to lower case
     */
    protected void toLowerCase() {
        ArrayList<Entry> changes = new ArrayList<Entry>();
        Iterator<Object> it = props.keySet().iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            String lowerCaseKey = key.toLowerCase();
            if (!lowerCaseKey.equals(key) && (props.getProperty(lowerCaseKey) == null)) {
                    String value = props.getProperty(key);
                    it.remove();
                    changes.add(new Entry(lowerCaseKey, value));
            }
        }
        Iterator<Entry> it2 = changes.iterator();
        while (it2.hasNext()) {
            Entry entry = it2.next();
            props.setProperty(entry.key, entry.value);
        }
    }

    protected String makeEntry(String mailbox, String domain) {
        if (domain != null) {
            return mailbox + "@" + domain;
        } else {
            return mailbox + "@";
        }
    }

    /**
     * Entry
     */
    protected static class Entry {
        protected String key;
        protected String value;
        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Fix for url name
     */
    protected static class URLNameFix extends URLName {

        /**
         * Constructor
         * @param urlName url name
         * @param username username
         */
        public URLNameFix(URLName urlName, String username) {
            super(urlName.getProtocol(), urlName.getHost(), urlName.getPort(), urlName.getRef() == null ? urlName.getFile() : urlName.getFile() + "#" + urlName.getRef(), username, null);
        }

        /**
         * Returns fixed filename of URL name (adding ref part
         * @return file
         */
        public String getFile() {
            String file = super.getFile();
            String ref = super.getRef();
            if (ref == null) {
                return file;
            } else {
                return file + "#" + ref;
            }
        }

    }
}
