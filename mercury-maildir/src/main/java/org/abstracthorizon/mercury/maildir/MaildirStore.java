/*
 * Copyright (c) 2005-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.maildir;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;


/**
 * <p>This is simple maildir implementation store. It reads files from the <i>cur</i> and
 * <i>new</i> subdirectories of the store or subdirectories of store's folders. When new
 * message is created <i>tmp</i> subdirectory is used for RFC-822 mail to be written
 * in it and then it is renamed to <i>new</i> directory. This implementation doesn't
 * write/read to/from any special index files and it is not implementing <code>UIDFolder</code>
 * interface from <code>javax.mail</code> API. Messages are ordered in folders in the
 * way <code>java.io.File.list</code> method is returning files.</p>
 *
 * <p>It takes a path of the store an input parameter (path in URLName). Also
 * it uses following session properties:
 *
 * <table border="1">
 * <tr><th>property</th><th>default value if not present</th><th>description</th></tr>
 * <tr>
 *   <td><code>maildir.leadingDot</code></td>
 *   <td><code>true</code></td>
 *   <td>If this property has value of <code>true</code> then folders' directories are
 *   with the leading dot. This is standard used by most implementations of maildir. If value
 *   is set to <code>false</code> then folders' directories are stored without leading dot.
 *   Note: If directories are without leading dot then <i>tmp</i>, <i>cur</i> and <i>new</i>
 *   are invalid names of folders.
 *   </td>
 * </tr>
 * <tr>
 *   <td><code>maildir.infoSeparator</code></td>
 *   <td><code>System.getProperty("path.separator")</code> is &quot;:&quot; then
 *   &quot;:&quot;. Othewise &quot;.&quot;</td>
 *   <td>If value is set then it will be used as separator of info part in maildir's filename.
 * </tr>
 * <tr>
 *   <td><code>maildir.httpSyntax</code></td>
 *   <td><code>false</code> is &quot;:&quot; then
 *   &quot;:&quot;. Othewise &quot;.&quot;</td>
 *   <td>If set to <code>true</code> then URLName's syntax for maildir should be:
 *   <code>maildir://user:password@host:port/foldername?base=base_directory_of_maildir_store</code>.
 *   Otherwise it is <code>maildir://user:password@host:port/base_directory_of_maildir_store#foldername</code>.
 *   Note: foldername is not used when store is being obtained.
 *   </td>
 * </tr>
 * <tr>
 *   <td><code>maildir.home</code></td>
 *   <td>&nbsp;</td>
 *   <td>If maildir store's base directory is not set in URL this session property will be queried and its
 *   value used if present.</td>
 * </tr>
 * </table>
 * </p>
 * <p>Maildir store's base directory is searched in following order:
 * <ol>
 * <li>URLName (check <code>maildir.httpSyntax</code> for more details). If not present then</li>
 * <li><code>maildir.home</code> from session. If not present then</li>
 * <li><code>user.home</code> from system properties with <code>.mail</code> appended to the path. If that directory is not present then</li>
 * <li><code>user.home</code> from system properties with <code>Mail</code> appended to the path.</li>
 * </ol>
 *
 * </p>
 * <p>If all fails it obtaining the store will fail as well.</p>
 * <p>This implementation also does substitutions in supplied path based on url name parameters:
 * <ul>
 * <li><code>{user}</code> in the path is substituted by username from url name</li>
 * <li><code>{protocol}</code> in the path is substituted by protocol from url name</li>
 * <li><code>{port}</code> in the path is substituted by port from url name</li>
 * <li><code>{host}</code> in the path is substituted by host from url name</li>
 * </ul>
 * </p>
 *
 * <p>This class implements centralised cached directory of all currently open folder.
 * </p>
 *
 * <p>Folder data actually contains messages while folders have only wrappers
 * with references to folder data's messages. Each folder uses wrappers to maintain
 * message number. All open folders are registered with folder data and changes
 * one folder, through folder data, are immediately propagated to other folder
 * over the same folder data (same virtually folder).
 * Folder data here serves as central repository of messages over one directory.
 * </p>
 *
 * <p>If new messages are discovered in directory or one folder over folder data
 * has new messages added by append method, all other folders over the same folder
 * data will be notified of new messages and then will immediately appear in their
 * lists</p>
 *
 * <p>Similarily if one folder expunges messages or messages are detected as deleted
 * from the underlaying directory, all folder's will be notified of the change and
 * all messages will be marked as expunged.</p>
 *

 * @author Daniel Senudula
 */
public class MaildirStore extends Store {

    /** Leading dot session attribute name */
    public static final String LEADING_DOT = "maildir.leadingDot";

    /** Store's home directory session attribute name */
    public static final String HOME = "maildir.home";

    /** Info separator session attribute name */
    public static final String INFO_SEPARATOR = "maildir.infoSeparator";

    /** Http syntax session attribute name */
    public static final String HTTP_SYNTAX = "maildir.httpSyntax";

    /** Amount of time folder is going to be kept in list of folders */
    public static final long MAX_FOLDER_DATA_LIFE = 1000*60*60; // 1 hour

    /** Store's base directory file */
    protected File base;

    /** Cached leading dot property */
    protected boolean leadingDot = true;

    /** Cached http syntax property */
    protected boolean httpSyntax = false;

    /** Cached info separator property */
    protected char infoSeparator;

    static {
        CommandMap commandMap = CommandMap.getDefaultCommandMap();
        if (commandMap instanceof MailcapCommandMap) {
            MailcapCommandMap mailcapCommandMap = (MailcapCommandMap)commandMap;
            mailcapCommandMap.addMailcap("multipart/*;; x-java-content-handler="+MaildirMimeMultipartDataContentHandler.class.getName());
        }
    }

    /** Cache */
    protected Map<File, Reference<MaildirFolderData>> directories = new WeakHashMap<File, Reference<MaildirFolderData>>();


    /**
     * Constructor
     * @param session mail session
     * @param urlname url name
     */
    public MaildirStore(Session session, URLName urlname) {
        super(session, urlname);

        String leadingDotString = session.getProperty(LEADING_DOT);
        if (leadingDotString != null) {
            leadingDot = "true".equals(leadingDotString);
        }

        String httpSyntaxString = session.getProperty(HTTP_SYNTAX);
        if (httpSyntaxString != null) {
            httpSyntax = "true".equalsIgnoreCase(httpSyntaxString);
        }

        String infoSeparatorString = session.getProperty(INFO_SEPARATOR);
        if ((infoSeparatorString != null) && (infoSeparatorString.length() > 0)) {
            infoSeparator = infoSeparatorString.charAt(0);
        } else {
            if (":".equals(System.getProperty("path.separator"))) {
                infoSeparator = ':';
            } else {
                infoSeparator = '.';
            }
        }

        if (httpSyntax) {
            parseURLName(urlname);
        } else {
            String baseFileName = urlname.getFile();
            setBaseFile(createBaseFile(urlname, baseFileName));
        }

        if (base == null) {
            String homeString = session.getProperty(HOME);
            if (homeString != null) {
                base = new File(homeString);
            } else {

                File home = new File(System.getProperty("user.home"));
                base = new File(home, ".mail");
                if (!base.exists()) {
                    File t = base;
                    base = new File(home, "Mail");
                    if (base.exists()) {
                        if (!"true".equals(session.getProperty(LEADING_DOT))) {
                            leadingDot = false;
                        }
                    } else {
                        base = t;
                    }
                }
            }
        }
    }

    /**
     * Parses url name
     * @param urlname url name
     */
    protected void parseURLName(URLName urlname) {
        String file = urlname.getFile();
        int i = file.indexOf('?');
        if (i >= 0) {
            String params = file.substring(i+1);
            i = 0;
            int j = params.indexOf(',', i);
            while (j > 0) {
                processParam(urlname, params.substring(i, j));
                i = j+1;
                j = params.indexOf(',', i);
            }
            processParam(urlname, params.substring(i));
        }
    }

    protected void setBaseFile(File file) {
        base = file;
        if (!base.exists()) {
            base.mkdirs();
        }
    }
    
    /**
     * Processes singe parameter from url name
     * @param urlName url name
     * @param param parameter
     */
    protected void processParam(URLName urlName, String param) {
        if (param.startsWith("base=")) {
            String baseString = param.substring(5);
            setBaseFile(createBaseFile(urlName, baseString));
        }
    }

    /**
     * Creates base file and substitues <code>{user}</code>, <code>{port}</code>,
     * <code>{host}</code> and <code>{protocol}</code>
     * @param urlName url name
     * @param baseName directory path
     * @return created file that represents base directory of the store
     */
    protected File createBaseFile(URLName urlName, String baseName) {
        baseName = replace(baseName, "{protocol}", urlName.getProtocol());
        baseName = replace(baseName, "{host}", urlName.getHost());
        baseName = replace(baseName, "{port}", Integer.toString(urlName.getPort()));
        baseName = replace(baseName, "{user}", urlName.getUsername());

        return new File(baseName);
    }

    /**
     * Replaces substring
     * @param s string on which operation is done
     * @param what what to be replaced
     * @param with new value to be placed instead
     * @return new string
     */
    protected String replace(String s, String what, String with) {
        int i = s.indexOf(what);
        if (i > 0) {
            return s.substring(0, i)+with+s.substring(i+what.length());
        } else {
            return s;
        }
    }

    /**
     * Returns info separator
     * @return info separator
     */
    public char getInfoSeparator() {
        return infoSeparator;
    }

    /**
     * Returns leading dot
     * @return leading dot
     */
    public boolean isLeadingDot() {
        return leadingDot;
    }

    /**
     * Returns http syntax
     * @return http syntax
     */
    public boolean isHttpSyntax() {
        return httpSyntax;
    }

    /**
     * Retuns base file (store's base directory)
     * @return base file
     */
    public File getBaseFile() {
        return base;
    }

    /**
     * Returns default folder
     * @return default folder
     * @throws MessagingException
     */
    public Folder getDefaultFolder() throws MessagingException {
        return getFolder("");
    }

    /**
     * Returns folder data for given folder. This is needed for new folder is created.
     * This implementation obtains proper folder's directory and passes file to
     * {@link #getFolderData(File)} method.
     * @param name full folder's name
     * @return folder data
     */
    protected MaildirFolderData getFolderData(String name) {
        if (!isConnected()) {
            throw new IllegalStateException("Store is not connected");
        }
        name = name.replace('/', '.');
        name = name.replace('\\', '.');
        if (name.startsWith(".")) {
            name = name.substring(1);
        }
        if ("inbox".equalsIgnoreCase(name)) {
            name = "inbox";
        }
        if ((name.length() > 0) && leadingDot) {
            name = '.'+name;
        }
        File file = new File(base, name);
        return getFolderData(file);
    }

    /**
     * This method returns folder data needed for folder to operate on.
     * If first checks cache and if there is no folder data in it
     * new will be created and stored in the cache.
     * @param file directory
     * @return new folder data
     */
    protected MaildirFolderData getFolderData(File file) {
        Reference<MaildirFolderData> ref = directories.get(file);
        MaildirFolderData folderData = null;
        if (ref != null) {
            folderData = (MaildirFolderData)ref.get();
            if ((System.currentTimeMillis() - folderData.getLastAccessed()) > MAX_FOLDER_DATA_LIFE) {
                folderData = null;
                directories.remove(file);
            }
        }
        if (folderData == null) {
            folderData = createFolderData(file);
            directories.put(file, new WeakReference<MaildirFolderData>(folderData));
        }

        return folderData;
    }

    /**
     * This implementation creates {@link MaildirFolderData} from supplied file.
     * @param file file
     * @return new maildir folder data
     */
    protected MaildirFolderData createFolderData(File file) {
        return new MaildirFolderData(this, file);
    }

    /**
     * Returns folder with given folder data
     * @param folderData folder data
     * @return new folder instance
     * @throws MessagingException
     */
    public Folder getParentFolder(MaildirFolderData folderData) throws MessagingException {
        String parentFolderName = folderData.getParentFolderName();
        return getFolder(parentFolderName);
    }

    /**
     * Returns new folder from full folder's name
     * @param name full folder's name
     * @return new folder
     * @throws MessagingException
     */
    public Folder getFolder(String name) throws MessagingException {
        if (!isConnected()) {
            throw new IllegalStateException("Store is not connected");
        }
        MaildirFolderData folderData = getFolderData(name);
        return createFolder(folderData);
    }

    /**
     * Returns new folder from URLName. if <code>maildir.httpSyntax</code> attribute
     * has value of <code>true</code> then url name's file is used, otherwise
     * url name's ref is used.
     * @param urlName url name
     * @return new folder
     * @throws MessagingException
     */
    public Folder getFolder(URLName urlName) throws MessagingException {
        if (isHttpSyntax()) {
            return getFolder(urlName.getFile());
        } else {
            return getFolder(urlName.getRef());
        }
    }

    /**
     * Creates new folder instance with given folder data. This metod is to be overriden by
     * class extensions.
     * @param folderData folder data
     * @return new folder instance
     */
    protected MaildirFolder createFolder(MaildirFolderData folderData) {
        return new MaildirFolder(this, folderData);
    }

    /**
     * This method returns <code>true</code> always.
     * @param host ignored
     * @param port ignored
     * @param user ignored
     * @param password ignored
     * @return <code>true</code>
     * @throws MessagingException never
     */
    protected boolean protocolConnect(String host,
            int port,
            String user,
            String password)
     throws MessagingException {
        return true;
    }
}
