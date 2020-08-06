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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.FolderEvent;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.MaildirFolderData;
import org.abstracthorizon.mercury.maildir.util.MessageBase;
import org.apache.log4j.Logger;

public class StaticMaildirFolder extends Folder {

    public static final Logger logger = Logger.getLogger(StaticMaildirFolder.class);
    
    protected StaticMaildirStore store;

    /** Permanent flags for root are user defined &quot;\\Noselect&quot; */
    public static final Flags rootPermanentFlags = new Flags("\\Noselect");

    /**
     * This constant defines a filename of a zero length
     * flag file that denotes no subfolders are suppose
     * to be created for this folder */
    public static final String NO_SUBFOLDERS_FILENAME = ".nosubfolders";

    protected long lastAccessed;
    protected boolean rootFolder;
    protected File base;
    protected File tmp;
    protected File cur;
    protected File nw;
    protected String name;
    protected String fullName;

    /** Permanent flags cache */
    protected Flags permanentFlags;

    /** Count of open folders. When count reaches zero, storage may remove this folder data */
    protected int openCount = 0;
    
    /** Messages in this directory (folder(s)) */
    protected List<StaticMaildirMessage> messages = new ArrayList<StaticMaildirMessage>();

    /** Map from files to message objects */
    protected Map<String, StaticMaildirMessage> files = new ConcurrentHashMap<String, StaticMaildirMessage>();
    
    /** Amount of time between two accesses. TODO - make this as an attribute */
    protected int delay = 1000; // 1 sec

    /** Delay factor - amount of time needed for reading directory vs delay. TODO - make this as an attribute */
    protected int delayFactor = 3;

    /** Type of the folder. See {@link javax.mail.Folder#HOLDS_FOLDERS} and {@link javax.mail.Folder#HOLDS_MESSAGES}. */
    protected int type = -1;
    
    // --- javax.mail.Folder methods ---
    public StaticMaildirFolder(StaticMaildirStore store, File base, String fullName) {
        super(store);
        this.store = store;
        
        int i = fullName.lastIndexOf('.');
        if (i >= 0) {
            name = fullName.substring(i + 1);
        } else {
            name = fullName;
        }
        
        this.base = base;

        rootFolder = base.equals(store.getBaseFile());
        
        tmp = new File(base, "tmp");
        cur = new File(base, "cur");
        nw = new File(base, "new");
        
        if (logger.isDebugEnabled()) {
            logger.debug("Created folder object; fullName=" + fullName + ", name=" + name + ", base=" + base.getAbsolutePath());
        }
    }
    
    // --- Setters and getters
    

    /**
     * Returns maildir store
     * @return maildir store
     */
    public StaticMaildirStore getMaildirStore() {
        return store;
    }

    /**
     * Returns folder's directory
     * @return folder's directory
     */
    public File getFolderDir() {
        return base;
    }

    /**
     * Sets folder's directory
     * @param file folder's directory
     */
    protected void setFolderDir(File file) {
        this.base = file;
    }
    

    /**
     * Returns when this folder data is last accessd
     * @return when this folder data is last accessd
     */
    public long getLastAccessed() {
        return lastAccessed;
    }
    

    /**
     * Returns folder's new directory
     * @return folder's new directory
     */
    protected File getNewDir() {
        return nw;
    }

    /**
     * Returns folder's cur directory
     * @return folder's cur directory
     */
    protected File getCurDir() {
        return cur;
    }

    /**
     * Returns folder's tmp directory
     * @return folder's tmp directory
     */
    protected File getTmpDir() {
        return tmp;
    }

    /**
     * Returns folder's directory
     * @return folder's directory
     */
    public File getFolderFile() {
        return base;
    }

    /**
     * Returns <code>true</code> if it is root folder
     * @return <code>true</code> if it is root folder
     */
    protected boolean isRootFolder() {
        return rootFolder;
    }
    
    public String getFullName() {
        return fullName;
    }

    public String getName() {
        return name;
    }

    // --- javax.mail.Folder methods ---
    
    public void appendMessages(Message[] messages) throws MessagingException {
        ArrayList<StaticMaildirMessage> addedMessages = new ArrayList<StaticMaildirMessage>();
        Exception exception = null;
        int num = 0;

        for (int i = 0; i < messages.length; i++) {
            try {
                StaticMaildirMessage message = createNewMaildirMessage((MimeMessage)messages[i], num);
                files.put(message.getBaseName(), message);
                
                num = num + 1;
                addedMessages.add(message);
            } catch (IOException e) {
                exception = e;
            }
        }
        if (addedMessages.size() > 0) {
            addMessages(addedMessages);
        }
        if (exception != null) {
            throw new MessagingException("Adding message failed", exception);
        }
    }

    public boolean create(int type) throws MessagingException {
        // TODO notify listeners
        if (rootFolder) {
            return false;
        }
        StaticMaildirFolder parent = store.getFolder(getParentFolderName());
        if (!parent.isRootFolder() && !parent.exists()) {
            if (!parent.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES) || !parent.create(Folder.HOLDS_FOLDERS)) {
                return false;
            }
        }

        if (!getFolderFile().mkdir()) {
            return false;
        }
        
        
        checkDirs();
        if ((type & Folder.HOLDS_FOLDERS) == 0) {
            File f = new File(getFolderFile(), NO_SUBFOLDERS_FILENAME);
            try {
                f.createNewFile();
            } catch (IOException e) {
                throw new MessagingException("Cannot prevent folder of having subfolders; cannot create file=" + f, e);
            }
        }
        this.type = type;

        notifyFolderListeners(FolderEvent.CREATED);
        return true;
    }

    /**
     * Deletes folder.
     * @param recursive if <code>true</code> deletes all subfolders as well.
     * @return <code>true</code> if operation was successful.
     * @throws MessagingException in case of an error while deleting folder
     */
    public boolean delete(boolean recursive) throws MessagingException {
        // TODO notify listeners
        if (rootFolder) {
            return false;
        }
        if (recursive) {
            String[] subfolders = listNames("%");
            boolean res = true;
            for (int i = 0; i < subfolders.length; i++) {
                StaticMaildirFolder subFolderData = store.getFolder(subfolders[i]);
                res = subFolderData.delete(true) && res;
            }
            if (!res) {
                return false;
            }
        }

        notifyFolderListeners(FolderEvent.DELETED);
        
        return deleteAll(getFolderFile());
    }

    /**
     * Utility method that deletes all subdirectories and files from given directory.
     * @param file directory to be deleted
     * @return <code>true<code> if operation was successful.
     */
    protected boolean deleteAll(File file) {
        boolean res = true;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    res = deleteAll(files[i]) && res;
                } else {
                    res = files[i].delete() && res;
                }
            }
        }
        if (res) {
            res = file.delete() && res;
        }
        return res;
    }
    
    public Message[] expunge() throws MessagingException {
        ArrayList<StaticMaildirMessage> expunged = new ArrayList<StaticMaildirMessage>();
        List<StaticMaildirMessage> messages = this.messages;

        Iterator<StaticMaildirMessage> it = messages.iterator();
        while (it.hasNext()) {
            StaticMaildirMessage message = it.next();
            if (!message.isExpunged() && message.getFlags().contains(Flags.Flag.DELETED)) {
                expunged.add(message);
            }
        }
        if (expunged.size() > 0) {
            it = expunged.iterator();
            while (it.hasNext()) {
                StaticMaildirMessage msg = (StaticMaildirMessage)it.next();
                if (!expungeMessage(msg)) {
                    it.remove();
                }
                messages.remove(msg);
                files.remove(msg.getBaseName());
            }

            removeMessages(expunged);

            Message[] res = new Message[expunged.size()];
            res = expunged.toArray(res);
            return res;
        } else {
            Message[] res = new Message[0];
            return res;
        }
    }

    /**
     * Expunges one message
     * @param message message to be expunged
     * @return <code>true</code> if expunge succed
     */
    protected boolean expungeMessage(StaticMaildirMessage message) {
        return message.expunge();
    }

    public Message getMessage(int msgnum) throws MessagingException {
        return messages.get(msgnum);
    }

    public int getMessageCount() throws MessagingException {
        return messages.size();
    }

    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @return {@link #rootPermanentFlags} if root folder or {@link #permanentFlags} otherwise
     */
    public Flags getPermanentFlags() {
        // TODO add check for this as parameter in store
        if (isRootFolder()) {
            return rootPermanentFlags;
        } else {
            if (permanentFlags == null) {
                permanentFlags = new Flags();
                permanentFlags.add(javax.mail.Flags.Flag.ANSWERED);
                permanentFlags.add(javax.mail.Flags.Flag.SEEN);
                permanentFlags.add(javax.mail.Flags.Flag.DELETED);
                permanentFlags.add(javax.mail.Flags.Flag.DRAFT);
                permanentFlags.add(javax.mail.Flags.Flag.FLAGGED);
            }
            return permanentFlags;
        }
    }

    /**
     * Returns &quot;/&quot;
     * @return &quot;/&quot;
     */
    public char getSeparator() {
        return '/';
    }

    public int getType() throws MessagingException {
        return type;
    }

    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @return <code>getMessageCount() &gt; 0</code>
     * @throws MessagingException
     */
    public boolean hasNewMessages() throws MessagingException {
        return getMessageCount() > 0;
    }

    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @param folder
     * @return result of {@link #renameTo(MaildirFolderData)} method.
     * @throws MessagingException
     */
    public boolean renameTo(Folder folder) throws MessagingException {
        if (folder instanceof StaticMaildirFolder) {
            return renameTo((StaticMaildirFolder)folder) != null;
        }
        return false;
    }

    /**
     * Renames folder to given folder data
     * @param folder folder data
     * @return <code>true</code> if folder was successfully renamed
     * @throws MessagingException thrown in case when there is a problem renaming folder
     */
    public StaticMaildirFolder renameTo(StaticMaildirFolder folder) throws MessagingException {
        if (folder.store != store) {
            throw new IllegalStateException("Folder belongs to wrong store; "+getFullName());
        }
        if (folder.exists()) {
            return null;
        }
        // setFolderFile(folderData.getFolderFile());
        // rootFolder = folderData.rootFolder;

        notifyFolderRenamedListeners(folder);
        
        return folder;
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        openCount = openCount - 1;
        messages.clear();       
        files.clear();
    }

    @Override
    public boolean exists() throws MessagingException {
        return getFolderFile().exists();
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        String fullName = getFullName() + getSeparator() + name;
        
        return (StaticMaildirFolder)store.getFolder(fullName);
    }

    @Override
    public Folder getParent() throws MessagingException {
        String fullName = getFullName();
        int i = fullName.lastIndexOf(getSeparator());
        if (i >= 0) {
            fullName = fullName.substring(0, i);
            return (StaticMaildirFolder) getFolder(fullName);
        } else {
            return (StaticMaildirFolder) store.getDefaultFolder();
        }
    }

    @Override
    public boolean isOpen() {
        return openCount > 0;
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        if ((pattern == null) || (pattern.length() == 0)) {
            pattern = ".*";
        } else {
            pattern = pattern.replaceAll("[*]", ".*?");
            pattern = pattern.replaceAll("[%]", "[^/]*?");
        }

        if (fullName.length() > 0) {
            pattern = fullName + '/' + pattern;
        }
        Pattern p = Pattern.compile(pattern);

        boolean leadingDot = store.isLeadingDot();
        ArrayList<String> names = new ArrayList<String>();

        File[] files = store.getBaseFile().listFiles();
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
            Folder f = getFolder(fullName + getSeparator() + name);
            folders.add(f);
        }
        
        Folder[] result = new Folder[folders.size()];
        result = folders.toArray(result);

        return result;    
    }

    /**
     * Lists subfolder names.
     * @param pattern pattern
     * @return list of subfolder names
     * @throws MessagingException
     */
    public String[] listNames(String pattern) throws MessagingException {
        if ((pattern == null) || (pattern.length() == 0)) {
            pattern = ".*";
        } else {
            pattern = pattern.replaceAll("[*]", ".*?");
            pattern = pattern.replaceAll("[%]", "[^/]*?");
        }

        String n = getFullName();

        if (n.length() > 0) {
            pattern = n+'/'+pattern;
        }
        Pattern p = Pattern.compile(pattern);

        boolean leadingDot = store.isLeadingDot();
        ArrayList<String> res = new ArrayList<String>();

        File[] files = store.getBaseFile().listFiles();
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
                            res.add(name);
                        }
                    }
                }
            }
        }

        String[] array = new String[res.size()];
        return res.toArray(array);
    }
    
    @Override
    public void open(int mode) throws MessagingException {
        openCount = openCount + 1;

        obtainMessages();
        
        // TODO add thread to scan for messages!!!
    }
    
    

    /**
     * This method checks if new, cur and tmp directories exist. If not it tries to create them.
     * @throws MessagingException in case subdirectories cannot be created.
     */
    protected void checkDirs() throws MessagingException {
        if (!getTmpDir().exists()) {
            if (!getTmpDir().mkdirs()) {
                throw new MessagingException("Cannot open folder; cannot create tmp directory; "+getFullName());
            }
        }
        if (!getCurDir().exists()) {
            if (!getCurDir().mkdirs()) {
                throw new MessagingException("Cannot open folder; cannot create cur directory; "+getFullName());
            }
        }
        if (!getNewDir().exists()) {
            if (!getNewDir().mkdirs()) {
                throw new MessagingException("Cannot open folder; cannot create new directory; "+getFullName());
            }
        }
    }

    /**
     * This method creates new maildir message for folder data (not folder). It is expected
     * for this method to create appropriate file in proper directory based on flags in
     * supplied message (cur or new).
     * @param message message whose content will be copied to new message
     * @param num message number
     * @return new maildir message this folder will keep for its folder data
     * @throws IOException
     * @throws MessagingException
     */
    protected StaticMaildirMessage createNewMaildirMessage(MimeMessage message, int num) throws IOException, MessagingException {
        return new StaticMaildirMessage(this, message, num);
    }

    /**
     * This method reads directory and creates messages for given folder.
     * @throws MessagingException
     */
    @SuppressWarnings("unchecked")
    protected void obtainMessages() throws MessagingException {
        try {
            // It is called from open so we do not need to sycnhronise - open is synchronised.
            long time = System.currentTimeMillis();
            if ((time - lastAccessed) > delay) {
                int msgno = 1;
                Map<String, StaticMaildirMessage> expunged = new HashMap<String, StaticMaildirMessage>(files);
                ArrayList<StaticMaildirMessage> newMessages = new ArrayList<StaticMaildirMessage>();
                File[] oldFiles = getCurDir().listFiles();
                File[] newFiles = getNewDir().listFiles();
                if (oldFiles != null) {
                    for (int i = 0; i < oldFiles.length; i++) {
                        String baseName = StaticMaildirMessage.baseNameFromFile(oldFiles[i]);
                        StaticMaildirMessage message = files.get(baseName);
                        if (message == null) {
                            // new file
                            message = createExistingMaildirMessage(oldFiles[i], msgno);
                            msgno++;
                            newMessages.add(message);
                            files.put(message.getBaseName(), message);
                        } else {
                            // we still have that file
                            expunged.remove(baseName);
                        }
                    }
                }
                if (newFiles != null) {
                    for (int i = 0; i < newFiles.length; i++) {
                        String baseName = StaticMaildirMessage.baseNameFromFile(newFiles[i]);
                        StaticMaildirMessage message = (StaticMaildirMessage)files.get(baseName);
                        if (message == null) {
                            // new file
                            message = createExistingMaildirMessage(newFiles[i], msgno);
                            msgno++;
                            newMessages.add(message);
                            files.put(message.getBaseName(), message);
                        } else {
                            // we still have that file
                            expunged.remove(baseName);
                        }
                    }
                }
                if (newMessages.size() > 0) {
                    addMessages(newMessages);
                }
                if (expunged.size() > 0) {
                    Iterator it = expunged.values().iterator();
                    while (it.hasNext()) {
                        StaticMaildirMessage msg = (StaticMaildirMessage)it.next();
                        messages.remove(msg);
                        files.remove(msg.getBaseName());
                    }
                    removeMessages(expunged.values());
                }

                long now = System.currentTimeMillis();
                if ((now - time) > (delay / delayFactor)) {
                    delay = delay * delayFactor;
                }

                lastAccessed = now;
            }
        } catch (IOException e) {
            throw new MessagingException("Cannot create message file", e);
        }
    }


    /**
     * This method adds messages to the folder.
     * @param folder folder where messages should be added without being notified. All others will be notified. If null supplied then all will be notified.
     * @param messages messages to be added
     * @throws MessagingException
     */
    protected void addMessages(List<StaticMaildirMessage> messages) throws MessagingException {
        // There are opened folders
        Collections.sort(messages);
        renumerateMessages(messages.size()+1, messages);
        messages.addAll(messages);
        
        Message[] added = new Message[messages.size()];
        added = messages.toArray(added);
        notifyMessageAddedListeners(added);
    }

    /**
     * This method removes folder messages. Called only for implicite removal.
     * @param folder folder that initiated call. That folder will be excluded from removal
     * and method folder.removeMessages must be called separately
     * @param messages messages to be removed
     * @throws MessagingException
     */
    protected void removeMessages(Collection<? extends MimeMessage> messages) throws MessagingException {
        messages.removeAll(messages);

        Message[] removed = new Message[messages.size()];
        removed = messages.toArray(removed);
        notifyMessageAddedListeners(removed);
    }
    
    /**
     * Renumerates given list of maildir message objects
     * @param from first number for renumeration to start with
     * @param messages maildir message objects
     */
    public static void renumerateMessages(int from, List<? extends MessageBase> messages) {
        for (int i = 0; i < messages.size(); i++) {
            MessageBase msg = messages.get(i);
            msg.setMessageNumber(from + i);
        }
    }
    
    /**
     * This method creates new maildir message object for existing file in folder data's directory.
     * @param file file message object is going to be created
     * @param num message number
     * @return new maildir message this folder will keep for its folder data
     * @throws IOException
     * @throws MessagingException
     */
    protected StaticMaildirMessage createExistingMaildirMessage(File file, int num) throws IOException, MessagingException {
        return new StaticMaildirMessage(this, file, num);
    }


    /**
     * Returns folder parent's name
     * @return folder parent's name
     */
    public String getParentFolderName() {
        if (rootFolder) {
            return null;
        } else {
            String name = getFullName();

            int i = name.lastIndexOf('/');
            if (i > 0) {
                name = name.substring(0, i);
            } else {
                name = "";
            }

            return name;
        }
    }
}
