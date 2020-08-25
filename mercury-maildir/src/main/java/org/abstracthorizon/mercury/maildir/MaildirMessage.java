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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Random;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.file.FileProvider;
import org.abstracthorizon.mercury.maildir.file.SharedInputStreamPool;
import org.abstracthorizon.mercury.maildir.util.LazyParsingMessage;


/**
 * <p>Maildir message representation.</p>
 * <p>Messages file name is defined in the following way:
 * <ul>
 * <li>time of creation in seconds</li>
 * <li>&quot;.&quot; (dot)</li>
 * <li>&quot;M&quot; followed with milliseconds of creation timestamp</li>
 * <li>&quot;P&quot; followed with threads hash code</li>
 * <li>&quot;R&quot; followed with 18bit random integer</li>
 * <li>&quot;.&quot; (dot)</li>
 * <li>host name</li>
 * <li>optional &quot;:&quot;, &quot;.&quot; or value from stores info separator attibute
 * followed by &quot;2,&quot; followed by flags</li>
 * </ul>
 * Flags are defined on the way explained in {@link org.abstracthorizon.mercury.maildir.FlagUtilities}
 * </p>
 *
 * @author Daniel Sendula
 */
public class MaildirMessage extends LazyParsingMessage implements FilenameFilter, FileProvider,  Comparable<MaildirMessage> {

    /** Number of retries when creating new file */
    public static final int CREATE_FILE_RETRIES = 6;

    /** Cached link to maildir folder message belongs to. Note - can be empty. */
    protected MaildirFolderData maildirFolder;

    /** Message's file */
    protected File file;

    /** Message's base name, name without info (flags) */
    protected String baseName;

    /** Cached info separator */
    protected char infoSeparator;

    /** Cached file size or -1 */
    protected long fileSize = -1;

    /** Flag to show is file in <i>new</i> subdirectory or not */
    protected boolean isNew;

    /** Flags (info) separator */
    public static final String FLAGS_SEPERATOR = "2,";

    /** Random number generator */
    protected static final Random randomGenerator = new Random();

    /** Host name cache */
    protected static String host;

    static {
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "localhost";
        }
    }

    /**
     * Constructor that takes new message and creates a file.
     * @param folder folder message belongs to.
     * @param message new message this message instance will be created based upon
     * @param msgnum message number
     * @throws MessagingException
     * @throws IOException
     */
    protected MaildirMessage(MaildirFolderData folder, MimeMessage message, int msgnum) throws MessagingException, IOException {
        super(message);
        this.maildirFolder = folder;
        this.folder = folder;
        this.msgnum = msgnum;
        infoSeparator = folder.getMaildirStore().getInfoSeparator();
        isNew = true;

        createFile(null);
        storeMessage(message);
        setFlags(message.getFlags(), true);
        initialise();
    }

    /**
     * Constructor that creates message object from the file.
     * @param folder folder this message belongs to
     * @param file file
     * @param msgnum message number
     * @param initialise should message be initialised or not
     * @throws MessagingException
     */
    public MaildirMessage(MaildirFolderData folder, File file, int msgnum, boolean initialise) throws MessagingException {
        super(folder, msgnum);
        this.maildirFolder = folder;
        infoSeparator = maildirFolder.getMaildirStore().getInfoSeparator();

        if (!file.exists()) {
            throw new MessagingException("File not found; "+file.getAbsolutePath());
        }

        setFile(file);

        if (initialise) {
            initialise();
        }
    }

    /**
     * Constructor that creates message object from the file. Message is always initialised
     * @param folder folder this message belongs to
     * @param file file
     * @param msgnum message number
     * @throws MessagingException
     */
    public MaildirMessage(MaildirFolderData folder, File file, int msgnum) throws MessagingException {
        this(folder, file, msgnum, true);
    }

    /**
     * Initialises message. This method really just calls parse method with appropriate
     * <code>SharedInputStream</code> implementation, which in turn (because of lazy implementation)
     * just stores that input stream.
     * @throws MessagingException
     */
    protected void initialise() throws MessagingException {
        parse(SharedInputStreamPool.getDefaultInstance().newStream(this, 0, fileSize));
    }

    /**
     * Static method that obtains base name from the given file.
     * @param file file
     * @return message's base name
     */
    public static String baseNameFromFile(File file) {
        String name = file.getName();
        int i = name.lastIndexOf(MaildirMessage.FLAGS_SEPERATOR);
        if (i > 0) {
            name = name.substring(0, i-1);
        }
        return name;
    }

    /**
     * Returns message's file. If file doesn't exist it tries to synchorise (reading all files
     * from directories and checks for same basename)
     * @return message's file.
     * @throws IOException
     */
    public File getFile() throws IOException {
        if (!file.exists()) {
            try {
                synchronise();
            } catch (MessagingException e) {
                throw new IOException(e.getMessage());
            }
        }
        return file;
    }

    /**
     * Sets message's file.
     * @param file file
     * @throws MessagingException if there is a problem setting flags
     */
    public void setFile(File file) throws MessagingException {
        this.file = file;

        File parentFile = file.getParentFile();
        isNew = parentFile.getAbsolutePath().endsWith(File.pathSeparator + "new");

        baseName = file.getName();
        int i = baseName.lastIndexOf(FLAGS_SEPERATOR);

        Flags flags = null;
        if (i > 0) {
            String flagsStr = baseName.substring(i+2);
            baseName = baseName.substring(0, i-1);
            flags = FlagUtilities.fromMaildirString(flagsStr);
        } else {
            flags = new Flags();
        }
        if (isNew) {
            flags.add(Flags.Flag.RECENT);
        }

        super.setFlags(flags, true);

        fileSize = file.length();
    }

    /**
     * Returns cached base name
     * @return cached base name
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Closes all <code>SharedInputStream</code> impementations over this file.
     */
    protected void closeFile() {
        SharedInputStreamPool.getDefaultInstance().closeWithProvider(this);
    }

    /**
     * Creates file name, a file name composed of time, milliseconds, process id and random number,
     * host name and flags (as part of file's info).
     * @param flags flags to be applied
     * @return new file name
     * @throws MessagingException
     */
    protected String createFileName(String flags) throws MessagingException {
        String time = Long.toString(System.currentTimeMillis());
        String milis = time.substring(time.length()-3);
        time = time.substring(0, time.length()-3);
        String pid = Integer.toString(Thread.currentThread().hashCode());
        String random = Integer.toString(randomGenerator.nextInt(131072));
        if ((flags == null) || (flags.length() == 0)) {
            return time+".M" + milis + "P" + pid + "R" + random + "." + host;
        } else {
            return time+".M" + milis + "P" + pid + "R" + random + "." + host + infoSeparator + FLAGS_SEPERATOR + flags;
        }
    }

    /**
     * Creates new file for the (new) message.
     * @param flags flags
     * @throws MessagingException if file cannot be created
     */
    protected void createFile(String flags) throws MessagingException {
        for (int i = 0; i < CREATE_FILE_RETRIES; i++) {
            String fileName = createFileName(flags);
            file = new File(maildirFolder.getTmpDir(), fileName);
            try {
                if (file.createNewFile()) {
                    baseName = file.getName();
                    return;
                }
            } catch (IOException e) {
                throw new MessagingException("Cannot create new file " + file.getAbsolutePath(), e);
            }
        }
        throw new MessagingException("Cannot create new file after " + CREATE_FILE_RETRIES + " retries.");
    }

    /**
     * Stores mime message to the file.
     * @param message message to be stored
     * @throws MessagingException if file cannot be written
     */
    protected void storeMessage(MimeMessage message) throws MessagingException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                message.writeTo(fos);
            } finally {
                fos.close();
            }
            File newDir = maildirFolder.getNewDir();
            String fn = file.getName();
            File newFile = new File(newDir, fn);

            if (!file.renameTo(newFile)) {
                throw new MessagingException("Cannot move file " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath());
            }
            file = newFile;
            // initialise();
            //baseName = baseNameFromFile(file);
        } catch (IOException e) {
            throw new MessagingException("Cannot write file", e);
        }
    }

    /**
     * Return's cached file's size.
     * @return cached file's size.
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Tries to find file with same base part as this file
     * @return <code>true</code> if it succeded
     * @throws MessagingException
     */
    public boolean synchronise() throws MessagingException {
        // TODO do this through folder!
        File newFile = new File(maildirFolder.getNewDir(), baseName);
        if (newFile.exists()) {
            file = newFile;
            return true;
        }
        File curDir = maildirFolder.getCurDir();
        File[] files = curDir.listFiles(this);
        if ((files != null) && (files.length == 1)) {
            setFile(files[0]);
        } else {
            expunged = true;
        }
        return false;
    }

    /**
     * Part of <code>FilenameFilter</code> interface.
     * @param file directory where filter is applied on
     * @param name file name to be checked
     * @return <code>true</code> if name start's with messages basename
     */
    public boolean accept(File file, String name) {
        return name.startsWith(baseName);
    }

    /**
     * Calls super parse method
     * @param is input stream
     * @throws MessagingException
     */
    protected void parse(InputStream is) throws MessagingException {
        super.parse(is);
    }

    /**
     * Check if message has already been parsed. If not calls
     * super method and closes files.
     * @throws MessagingException
     */
    protected synchronized void parseImpl() throws MessagingException {
        if (!parsed) {
            super.parseImpl();
            closeFile();
        }
    }

    /**
     * Expunges the message. Effectively tries to delete the file
     * @return <code>true</code> if file can be deleted
     */
    protected boolean expunge() {
        synchronized (this) {
            if (file.exists()) {
                closeFile();
                boolean res = file.delete();
                setExpunged(res);
                if (res) {
                    String name = file.getName();
                    int i = name.indexOf(':');
                    if (i >= 0) {
                        name = name.substring(0, i);
                    }
                    File delFile = new File(maildirFolder.getDelDir(), name);
                    try {
                        if (!delFile.createNewFile()) {
                            // TODO what now?
                        }
                    } catch (IOException ignore) { }
                }
                return res;
            } else {
                setExpunged(true);
                return false;
            }
        }
    }

    /**
     * Returns file's date or <code>null</code> if file doesn't exist
     * @return file's date.
     * @throws MessagingException
     */
    public Date getReceivedDate() throws MessagingException {
        if (!file.exists()) {
            synchronise();
        }
        if (file.exists()) {
            long l = file.lastModified();
            if (l != -1L) {
                return new Date(l);
            }
        }
        return null;
    }

    /**
     * Sets flags to the message. This method is updating file's name.
     * @param flags flags that are applied
     * @param set are flags set or removed
     * @throws MessagingException
     */
    public void setFlags(Flags flags, boolean set) throws MessagingException {
        super.setFlags(flags, set);
        if (!file.exists()) {
            synchronise();
        }
        if (file.exists()) {
            synchronized (this) {
                closeFile();
                File newFile = null;
                String oldFlgs = file.getName();
                int i = oldFlgs.lastIndexOf(FLAGS_SEPERATOR);
                if (i > 0) {
                    oldFlgs = oldFlgs.substring(i+2);
                } else {
                    oldFlgs = "";
                }

                Flags currentFlags = getFlags();

                String flgs = FlagUtilities.toMaildirString(currentFlags);
                if ((!flgs.equals(oldFlgs) || (isNew != currentFlags.contains(Flags.Flag.RECENT)))) {
                    if (flags.contains(Flags.Flag.RECENT) ) {
                        super.setFlags(getFlags(), false);
                        super.setFlags(new Flags(Flags.Flag.RECENT), true);
                        newFile = new File(maildirFolder.getNewDir(), baseName);
                        isNew = true;
                    } else if (flgs.length() > 0) {
                        newFile = new File(maildirFolder.getCurDir(), baseName + infoSeparator + FLAGS_SEPERATOR+flgs);
                        isNew = false;
                    } else {
                        newFile = new File(maildirFolder.getCurDir(), baseName);
                        isNew = false;
                    }
                }

                if (newFile != null) {
                    if (!file.renameTo(newFile)) {
                        throw new MessagingException("Cannot set flags; oldFile=" + file.getAbsolutePath() + ", newFile="+newFile);
                    }
                    file = newFile;
                }
            }
        }
    }


    /**
     * This method compares two messages by base name.
     * @param o message to be compared with
     * @return -1, 0, 1 depending if this message has less, equal or greater base name.
     * If supplied object is not Maildir message then -1 is returned.
     */
    public int compareTo(MaildirMessage o) {
        String s1 = baseName;
        String s2 = o.getBaseName();
        return s1.compareTo(s2);
    }
}
