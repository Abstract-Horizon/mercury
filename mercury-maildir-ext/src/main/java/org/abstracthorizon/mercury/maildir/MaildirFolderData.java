/*
 * Copyright (c) 2005-2007 Creative Sphere Limited.
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
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.util.MessageBase;
import org.abstracthorizon.mercury.maildir.util.MessageWrapper;


/**
 * <p>Folder data actually represents the model of the maildir folder. This class
 * handles all model operations - all operations that are low level and deal
 * with files and directories.
 * </p>
 *
 * <p>This folder data serves as central repository for messages over
 * a directory. Each directory will have only one folder data for store.
 * That is maintained in store itself</p>
 *
 * <p>Messages that are present in folder data are later wrapped inside of
 * folder. Pair of (message, file) is kept in folder data's cache.
 * Cache for each directory is maintained as long as there are opened folders
 * over that directory. After that time it is left to be collected throught
 * weak reference. If folder over folder data is opened before reference
 * is garbage collected it is reused.
 * </p>
 *
 * @author Daniel Sendula
 */
public class MaildirFolderData extends Folder {

    /**
     * This constant defines a filename of a zero length
     * flag file that denotes no subfolders are suppose
     * to be created for this folder */
    public static final String NO_SUBFOLDERS_FILENAME = ".nosubfolders";

    /** Permanent flags for root are user defined &quot;\\Noselect&quot; */
    public static final Flags rootPermanentFlags = new Flags("\\Noselect");

    /** Permanent flags cache */
    protected Flags permanentFlags;

    /** Maildir store */
    protected MaildirStore store;

    /** Directory folder data is for */
    protected File base;

    /** Flag to denote is this root folder or not */
    protected boolean rootFolder;

    /** Type of the folder. See {@link javax.mail.Folder#HOLDS_FOLDERS} and {@link javax.mail.Folder#HOLDS_MESSAGES}. */
    protected int type = -1;

    /** Tmp subdirectory */
    protected File tmp;

    /** Cur subdirectory */
    protected File cur;

    /** New subdirectory */
    protected File nw;

    /** Cached folder's full name */
    protected String cachedFullName;

    /** Cached folder's name */
    protected String cachedName;

    /** Last time folder data was accessed or 0 if no open folders */
    protected long lastAccess;

    /** Amount of time between two accesses. TODO - make this as an attribute */
    protected int delay = 1000; // 1 sec

    /** Delay factor - amount of time needed for reading directory vs delay. TODO - make this as an attribute */
    protected int delayFactor = 3;

    /** List of open folders */
    protected WeakHashMap<Folder, Object> openedFolders = new WeakHashMap<Folder, Object>();

    /** Folder's data */
    protected Data data;

    /** Weak reference to data when there are no open folders */
    protected Reference<Data> closedRef;

    /** Count of open folders. When count reaches zero, storage may remove this folder data */
    protected int openCount = 0;

    /**
     * Constructor
     * @param store store
     * @param file directory
     */
    public MaildirFolderData(MaildirStore store, File file) {
        super(store);
        this.store = store;
        setFolderFile(file);
        rootFolder = base.equals(store.getBaseFile());
        tmp = new File(base, "tmp");
        cur = new File(base, "cur");
        nw = new File(base, "new");
    }

    /**
     * Returns maildir store
     * @return maildir store
     */
    public MaildirStore getMaildirStore() {
        return store;
    }

    /**
     * Returns folder's directory
     * @return folder's directory
     */
    public File getFolderFile() {
        return base;
    }

    /**
     * Sets folder's directory
     * @param file folder's directory
     */
    protected void setFolderFile(File file) {
        this.base = file;
    }

    /**
     * Returns when this folder data is last accessd
     * @return when this folder data is last accessd
     */
    public long getLastAccessed() {
        return lastAccess;
    }

    /**
     * Returns <code>true</code> if it is root folder
     * @return <code>true</code> if it is root folder
     */
    protected boolean isRootFolder() {
        return rootFolder;
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
     * Returns folder's name
     * @return folder's name
     */
    public String getName() {
        if (cachedName != null) {
            return cachedName;
        }
        if (rootFolder) {
            cachedName = "";
        } else {
            String name = getFolderFile().getName();
            int i = name.lastIndexOf('.');
            if (i > 0) {
                name = name.substring(i+1);
            }
            if (store.isLeadingDot() && name.startsWith(".")) {
                name = name.substring(1);
            }
            cachedName = name;
        }
        return cachedName;
    }

    /**
     * Returns folder's full name (path and name)
     * @return folder's full name
     */
    public String getFullName() {
        if (cachedFullName != null) {
            return cachedFullName;
        }
        if (rootFolder) {
            cachedFullName = "";
        } else {
            String name = getFolderFile().getName();
            if (store.isLeadingDot() && name.startsWith(".")) {
                name = name.substring(1);
            }
            cachedFullName = name.replace('.', '/');
        }
        return cachedFullName;
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

    /**
     * Returns <code>true</code> if folder exists. This method checks if folder's directory exist.
     * @return <code>true</code> if folder exists
     * @throws MessagingException
     */
    public boolean exists() throws MessagingException {
        return getFolderFile().exists();
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

    /**
     * Returns &quot;/&quot;
     * @return &quot;/&quot;
     */
    public char getSeparator() {
        return '/';
    }

    /**
     * Returns name for given subfolder.
     * @param name name of subfolder
     * @return subfolder's full name
     */
    public String getSubFolderName(String name) {
        if (rootFolder) {
            return name;
        } else {
            return getFullName()+getSeparator()+name;
        }
    }

    /**
     * Creates folder.
     * @param type See {@link javax.mail.Folder#HOLDS_FOLDERS} and {@link javax.mail.Folder#HOLDS_MESSAGES}.
     * @return <code>true</code> if subfolder is successfully created.
     * @throws MessagingException in case of a problem while creating folder.
     */
    public boolean create(int type) throws MessagingException {
        if (rootFolder) {
            return false;
        }
        MaildirFolderData parent = store.getFolderData(getParentFolderName());
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
        return true;
    }

    /**
     * Deletes folder.
     * @param recursive if <code>true</code> deletes all subfolders as well.
     * @return <code>true</code> if operation was successful.
     * @throws MessagingException in case of an error while deleting folder
     */
    public boolean delete(boolean recursive) throws MessagingException {

        if (rootFolder) {
            return false;
        }
        if (recursive) {
            String[] subfolders = listNames("%");
            boolean res = true;
            for (int i = 0; i < subfolders.length; i++) {
                MaildirFolderData subFolderData = store.getFolderData(subfolders[i]);
                res = subFolderData.delete(true) && res;
            }
            if (!res) {
                return false;
            }
        }

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

    /**
     * Returns folder's type. It checks for flag file to determine are subfolders are allowed.
     * In this implementation messages are always allowed.
     * @return folder's type. See {@link javax.mail.Folder#HOLDS_FOLDERS} and {@link javax.mail.Folder#HOLDS_MESSAGES}.
     * @throws MessagingException never.
     */
    public int getType() throws MessagingException {
        if (type == -1) {
            if (rootFolder) {
                type = Folder.HOLDS_FOLDERS;
            } else {
                File f = new File(getFolderFile(), NO_SUBFOLDERS_FILENAME);
                if (f.exists()) {
                    type = Folder.HOLDS_MESSAGES;
                } else {
                    type = Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES;
                }
            }
        }
        return type;
    }

    /**
     * Renames folder to given folder data
     * @param folderData folder data
     * @return <code>true</code> if folder was successfully renamed
     * @throws MessagingException thrown in case when there is a problem renaming folder
     */
    public MaildirFolderData renameTo(MaildirFolderData folderData) throws MessagingException {
        if (folderData.store != store) {
            throw new IllegalStateException("Folder belongs to wrong store; "+getFullName());
        }
        if (folderData.exists()) {
            return null;
        }
        // setFolderFile(folderData.getFolderFile());
        // rootFolder = folderData.rootFolder;

        return folderData;
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

   // ---------------------------------------------------------------------------------------

    /**
     * Returns message count. This implementation checks files on the disk - new and cur directories.
     * @return message count
     * @throws MessagingException never
     */
    public int getMessageCount() throws MessagingException {
        synchronized (this) {
            if (data != null) {
                return data.messages.size();
            }
        }

        int len = 0;
        if (getNewDir().exists()) {
            String[] fs = getNewDir().list();
            if (fs != null) {
                len = len + fs.length;
            }
        }
        if (getCurDir().exists()) {
            String[] fs = getCurDir().list();
            if (fs != null) {
                len = len + fs.length;
            }
        }

        return len;
    }

    /**
     * Returns new message count. This implementation checks files on the disk - only in new directory.
     * @return new message count
     * @throws MessagingException never
     */
    public int getNewMessageCount() throws MessagingException {
        synchronized (this) {
            if (data != null) {
                int cnt = 0;
                Iterator<MaildirMessage> it = data.messages.iterator();
                while (it.hasNext()) {
                    Message msg = it.next();
                    if (msg.isSet(Flags.Flag.RECENT)) {
                        cnt++;
                    }
                }
                return cnt;
            }
        }
        if (getNewDir().exists()) {
            return getNewDir().list().length;
        }
        return 0;
    }

    /**
     * This method is called by folder that is being opened. This implementation reads all files from the new and cur directories and
     * wraps them in appropriate message message objects (see {@link #createExistingMaildirMessage(File, int)}).
     * @param folder folder that asked opening
     * @throws MessagingException thrown if an error is encountered while creating messages
     * @throws MessagingException thrown if an error is encountered while creating messages
     */
    protected void open(MaildirFolder folder) throws MessagingException {
        openCount = openCount + 1;
        if (data == null) { // Must be same as openCount == 1
            if (closedRef != null) {
                data = closedRef.get();
            }
            if (data == null) {
                data = new Data();
                data.messages = new ArrayList<MaildirMessage>();
                data.files = new HashMap<String, MaildirMessage>();
            }
            closedRef = null;
        }

        folder.setFolderMessages(createFolderMessages());
        obtainMessages();

        // If folder is pronounced as opened here
        // it won't be filled with newly discovered messages so...
        synchronized (openedFolders) {
            openedFolders.put(folder, null);
        }
        // ... this statement here will be only one that will add messages
        folder.addMessages(data.messages, false);
    }

    /**
     * This method is called with folder that is closing.
     * @param folder folder that is closed
     */
    protected void close(MaildirFolder folder) {
        synchronized (openedFolders) {
            openedFolders.remove(folder);
        }

        openCount = openCount - 1;
        if (openCount == 0) {
            closedRef = new WeakReference<Data>(data);
            data = null;
        }
    }

    /**
     * Appends messages to the folder.
     * @param folder folder that initiated appending messages
     * @param messages array of messages
     * @throws MessagingException thrown while creating new message.
     */
    protected void appendMessages(MaildirFolder folder, Message[] messages) throws MessagingException {
        ArrayList<MaildirMessage> addedMessages = new ArrayList<MaildirMessage>();
        Exception exception = null;
        int num = 0;
        if ((folder != null) && folder.isOpen()) {
            num = folder.getMessageCount();
        }
        for (int i = 0; i < messages.length; i++) {
            try {
                MaildirMessage message = createNewMaildirMessage((MimeMessage)messages[i], num);
                if (data != null) {
                    data.files.put(message.getBaseName(), message);
                }
                num = num + 1;
                addedMessages.add(message);
            } catch (IOException e) {
                exception = e;
            }
        }
        if (addedMessages.size() > 0) {
            // folder.addMessages(addedMessages, true);
            addMessages(folder, addedMessages);
        }
        if (exception != null) {
            throw new MessagingException("Adding message failed", exception);
        }
    }

    /**
     * Appends messages to the maildir data. This method doesn't append messages to any
     * specific folder - but all opened.
     * @param messages messages to be appended
     * @throws MessagingException
     */
    public void appendMessages(Message[] messages) throws MessagingException {
        appendMessages(null, messages);
    }

    /**
     * Expunges messages for given folder.
     * @param folder folder
     * @param explicit should folder be notified of messages
     * @return list of messages that were expunged
     * @throws MessagingException thrown in <code>javax.mail.Message.getFlags()</code> method
     */
    protected List<? extends Message> expunge(MaildirFolder folder, boolean explicit) throws MessagingException {
        ArrayList<MaildirMessage> expunged = new ArrayList<MaildirMessage>();
        List<MaildirMessage> messages = getFolderMessages(folder);

        Iterator<MaildirMessage> it = messages.iterator();
        while (it.hasNext()) {
            MaildirMessage message = it.next();
            if (!message.isExpunged() && message.getFlags().contains(Flags.Flag.DELETED)) {
                expunged.add(message);
            }
        }
        if (expunged.size() > 0) {
            it = expunged.iterator();
            while (it.hasNext()) {
                MaildirMessage msg = (MaildirMessage)it.next();
                if (!expungeMessage(msg)) {
                    it.remove();
                }
                data.messages.remove(msg);
                data.files.remove(msg.getBaseName());
            }

            removeMessages(folder, expunged);
            // Remove method removes for all but folder
            return folder.removeMessages(expunged, explicit);
        } else {
            // Zero sized list
            return expunged;
        }
    }


    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @return <code>null</code>
     * @throws MessagingException
     */
    public Message[] expunge() throws MessagingException {
        // TODO Try to give this method some meaning...
        return null;
    }

    /**
     * Returns folder's messages for given folder.
     * This implementation unwraps messages from the folder.
     *
     * @param folder
     * @return list of messages
     */
    protected List<MaildirMessage> getFolderMessages(MaildirFolder folder) {
        List<? extends MessageBase> messages = folder.getFolderMessages();
        int size = messages.size();
        ArrayList<MaildirMessage> unwrapped = new ArrayList<MaildirMessage>(size);
        for (int i = 0; i < size; i++) {
            MaildirMessage wrapped = (MaildirMessage)((MessageWrapper)messages.get(i)).getMessage();
            unwrapped.add(wrapped);
        }
        return unwrapped;
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
            if ((time - lastAccess) > delay) {
                int msgno = 1;
                HashMap<String, MaildirMessage> expunged = (HashMap<String, MaildirMessage>)data.files.clone();
                ArrayList<MaildirMessage> newMessages = new ArrayList<MaildirMessage>();
                File[] oldFiles = getCurDir().listFiles();
                File[] newFiles = getNewDir().listFiles();
                if (oldFiles != null) {
                    for (int i = 0; i < oldFiles.length; i++) {
                        String baseName = MaildirMessage.baseNameFromFile(oldFiles[i]);
                        MaildirMessage message = (MaildirMessage)data.files.get(baseName);
                        if (message == null) {
                            // new file
                            message = createExistingMaildirMessage(oldFiles[i], msgno);
                            msgno++;
                            newMessages.add(message);
                            if (data != null) {
                                data.files.put(message.getBaseName(), message);
                            }
                        } else {
                            // we still have that file
                            expunged.remove(baseName);
                        }
                    }
                }
                if (newFiles != null) {
                    for (int i = 0; i < newFiles.length; i++) {
                        String baseName = MaildirMessage.baseNameFromFile(newFiles[i]);
                        MaildirMessage message = (MaildirMessage)data.files.get(baseName);
                        if (message == null) {
                            // new file
                            message = createExistingMaildirMessage(newFiles[i], msgno);
                            msgno++;
                            newMessages.add(message);
                            if (data != null) {
                                data.files.put(message.getBaseName(), message);
                            }
                        } else {
                            // we still have that file
                            expunged.remove(baseName);
                        }
                    }
                }
                if (newMessages.size() > 0) {
                    addMessages(null, newMessages);
                }
                if (expunged.size() > 0) {
                    Iterator it = expunged.values().iterator();
                    while (it.hasNext()) {
                        MaildirMessage msg = (MaildirMessage)it.next();
                        data.messages.remove(msg);
                        data.files.remove(msg.getBaseName());
                    }
                    removeMessages((MaildirFolder)null, expunged.values());
                }

                long now = System.currentTimeMillis();
                if ((now - time) > (delay / delayFactor)) {
                    delay = delay * delayFactor;
                }

                lastAccess = now;
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
    protected void addMessages(MaildirFolder folder, List<MaildirMessage> messages) throws MessagingException {
        if (data != null) {
            // There are opened folders
            Collections.sort(messages);
            renumerateMessages(data.messages.size()+1, messages);
            data.messages.addAll(messages);

            synchronized (openedFolders) {
                Iterator<Folder> it = openedFolders.keySet().iterator();
                while (it.hasNext()) {
                    MaildirFolder extFolder = (MaildirFolder)it.next();
                    extFolder.addMessages(messages, folder != extFolder);
                }
            }
        }
    }

    /**
     * This method removes folder messages. Called only for implicite removal.
     * @param folder folder that initiated call. That folder will be excluded from removal
     * and method folder.removeMessages must be called separately
     * @param messages messages to be removed
     * @throws MessagingException
     */
    protected void removeMessages(MaildirFolder folder, Collection<? extends MimeMessage> messages) throws MessagingException {
        synchronized (openedFolders) {
            Iterator<Folder> it = openedFolders.keySet().iterator();
            while (it.hasNext()) {
                MaildirFolder extFolder = (MaildirFolder)it.next();
//                extFolder.removeMessages(messages, (folder == extFolder) && explicit);
                if (extFolder != folder) {
                    extFolder.removeMessages(messages, false);
                }
            }
        }
    }

    /**
     * This method creates collection structure for storing messages in the folder.
     * This implementation returns <code>ArrayList</code>
     * @return list for messages to be stored in.
     */
    protected List<MessageBase> createFolderMessages() {
        return new ArrayList<MessageBase>();
    }

    /**
     * Expunges one message
     * @param message message to be expunged
     * @return <code>true</code> if expunge succed
     */
    protected boolean expungeMessage(MaildirMessage message) {
        return message.expunge();
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
     * This method creates new maildir message for folder data (not folder). It is expected
     * for this method to create appropriate file in proper directory based on flags in
     * supplied message (cur or new).
     * @param message message whose content will be copied to new message
     * @param num message number
     * @return new maildir message this folder will keep for its folder data
     * @throws IOException
     * @throws MessagingException
     */
    protected MaildirMessage createNewMaildirMessage(MimeMessage message, int num) throws IOException, MessagingException {
        return new MaildirMessage(this, message, num);
    }

    /**
     * This method creates new maildir message object for existing file in folder data's directory.
     * @param file file message object is going to be created
     * @param num message number
     * @return new maildir message this folder will keep for its folder data
     * @throws IOException
     * @throws MessagingException
     */
    protected MaildirMessage createExistingMaildirMessage(File file, int num) throws IOException, MessagingException {
        return new MaildirMessage(this, file, num);
    }


    /**
     * Folders data class. This class keeps list of messages and map for files, messages pair.
     * It is moved to separate class just for easier handling of moving instance of this class
     * under weak reference regime when there are no open folders. That means garbage collector
     * can more easily remove not used messages.
     */
    protected static class Data {
        /** Messages in this directory (folder(s)) */
        protected List<MaildirMessage> messages;

        /** Map from files to message objects */
        protected HashMap<String, MaildirMessage> files;
    }



    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @return <code>null</code>
     * @throws MessagingException
     */
    public Folder getParent() throws MessagingException {
        return null;
    }

    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @param arg0
     * @return <code>null</code>
     * @throws MessagingException
     */
    public Folder[] list(String arg0) throws MessagingException {
        return null;
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
     * @param arg0
     * @return <code>null</code>
     * @throws MessagingException
     */
    public Folder getFolder(String arg0) throws MessagingException {
        return null;
    }

    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @param folder
     * @return result of {@link #renameTo(MaildirFolderData)} method.
     * @throws MessagingException
     */
    public boolean renameTo(Folder folder) throws MessagingException {
        if (folder instanceof MaildirFolderData) {
            return renameTo((MaildirFolderData)folder) != null;
        }
        return false;
    }

    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @param arg0
     * @throws MessagingException
     */
    public void open(int arg0) throws MessagingException {
    }

    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @param arg0
     * @throws MessagingException
     */
    public void close(boolean arg0) throws MessagingException {
    }

    /**
     * This method is only to satisfy Folder interface. Not to be used.
     * @return <code> openCount &gt; 0</code>
     */
    public boolean isOpen() {
        return openCount > 0;
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
     * This method is only to satisfy Folder interface. Not to be used.
     * @param i index
     * @return returns message with given index
     * @throws MessagingException
     */
    public Message getMessage(int i) throws MessagingException {
        if (data != null) {
            return (MaildirMessage)data.messages.get(i);
        } else {
            return null;
        }
    }
}
