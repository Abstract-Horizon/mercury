/*
 * Copyright (c) 2010 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.maildir.ext;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

public class MaildirStore extends Store {

    /** Leading dot session attribute name */
    public static final String LEADING_DOT = "maildir.leadingDot";

    /** Store's home directory session attribute name */
    public static final String HOME = "maildir.home";

    /** Info separator session attribute name */
    public static final String INFO_SEPARATOR = "maildir.infoSeparator";

    /** Http syntax session attribute name */
    public static final String HTTP_SYNTAX = "maildir.httpSyntax";

    /** Store's base directory file */
    protected File base;

    /** Cached leading dot property */
    protected boolean leadingDot = true;

    /** Cached http syntax property */
    protected boolean httpSyntax = false;

    /** Cached info separator property */
    protected char infoSeparator;

    /** Cache */
    protected Map<File, Reference<StaticMaildirFolder>> folderDataMap = new WeakHashMap<File, Reference<StaticMaildirFolder>>();

    /** Amount of time folder is going to be kept in list of folders */
    public static final long MAX_FOLDER_DATA_LIFE = 1000*60*60; // 1 hour

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

    // --- javax.mail.Store interface
    
    @Override
    public Folder getDefaultFolder() throws MessagingException {
        return getFolder("");
    }

    @Override
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

        name = normaliseName(name);
        
        File file = new File(base, name);
        
        return createFolder(file);
    }

    @Override
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

    // --- Getters and setters

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

    // --- Utility methods
    
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

    protected void setBaseFile(File file) {
        base = file;
        if (!base.exists()) {
            base.mkdirs();
        }
    }

    protected String extractFullName(File file) {
        return "";
    }
    
    protected String extractName(File file) {
        return "";
    }

    protected String normaliseName(String name) {
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
        return name;
    }
    
    /**
     * Replaces substring
     * @param s string on which operation is done
     * @param what what to be replaced
     * @param with new value to be placed instead
     * @return new string
     */
    protected String replace(String s, String what, String with) {
        return s.replace(what, with);
    }

    // --- Maildir methods
    
    
    public MaildirFolder createFolder(File file) {
        StaticMaildirFolder folderData = getFolderData(file); 
        String fullName = extractFullName(file);
        String name = extractName(file);
        if (folderData == null) {
            return new MaildirFolder(this, file, fullName, name);
        } else {
            return new MaildirFolder(this, folderData, file, fullName, name);
        }
    }
    
    public StaticMaildirFolder getFolderData(File file) {
        StaticMaildirFolder folderData = null; 
        Reference<StaticMaildirFolder> reference = folderDataMap.get(file);
        if (reference != null) {
            folderData = reference.get();
        }
        if (folderData != null) {
            if ((System.currentTimeMillis() - folderData.lastAccessed) > MAX_FOLDER_DATA_LIFE) {
                folderData = null;
                folderDataMap.remove(file);
            }
        }
        return folderData;
    }
    
    public StaticMaildirFolder createFolderData(File file, int type) {
        return null;
    }
    
    public MaildirFolder getFolder(StaticMaildirFolder folder, String name) throws MessagingException {
        String fullName = folder.getFullName() + folder.getSeparator() + name;
        
        return (MaildirFolder) getFolder(fullName);
    }

    public MaildirFolder getParent(StaticMaildirFolder folder) throws MessagingException {
        String fullName = folder.getFullName();
        int i = fullName.lastIndexOf(folder.getSeparator());
        if (i >= 0) {
            fullName = fullName.substring(0, i);
            return (MaildirFolder) getFolder(fullName);
        } else {
            return (MaildirFolder) getDefaultFolder();
        }
    }

    public Folder[] list(MaildirFolder folder, String pattern) throws MessagingException {
        if ((pattern == null) || (pattern.length() == 0)) {
            pattern = ".*";
        } else {
            pattern = pattern.replaceAll("[*]", ".*?");
            pattern = pattern.replaceAll("[%]", "[^/]*?");
        }

        String fullName = folder.getFullName();

        if (fullName.length() > 0) {
            pattern = fullName + '/' + pattern;
        }
        Pattern p = Pattern.compile(pattern);

        ArrayList<String> names = new ArrayList<String>();

        File[] files = getBaseFile().listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    String name = files[i].getName();
                    if (!"tmp".equals(name) && !"new".equals(name) && !"cur".equals(name) &&
                            ((leadingDot && name.startsWith(".")) || (!leadingDot && !name.startsWith(".")))) {

                        if (leadingDot) {
                            name = name.substring(1);
                        }
                        name = name.replace('.', '/');
                        if (p.matcher(name).matches()) {
                            names.add(name);
                        }
                    }
                }
            }
        }
        
        ArrayList<Folder> folders = new ArrayList<Folder>();
        for (String name : names) {
            Folder f = getFolder(fullName + folder.getSeparator() + name);
            folders.add(f);
        }
        
        Folder[] result = new Folder[folders.size()];
        result = folders.toArray(result);

        return result;
    }

    public void open(MaildirFolder folder, int mode) {
        File file = folder.getFolderDir(); 
        StaticMaildirFolder folderData = getFolderData(file);
        if (folderData == null) {
            if (file.exists()) {
                folderData = new StaticMaildirFolder(this, file);
                folderDataMap.put(file, new WeakReference<StaticMaildirFolder>(folderData));
            }
        }
        folder.setFolderData(folderData);
    }

    public boolean rename(MaildirFolder from, MaildirFolder to) {
        return false;
    }
}
