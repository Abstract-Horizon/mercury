/*
 * Copyright (c) 2005-2020 Creative  Sphere Limited.
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
package org.abstracthorizon.mercury.maildir.uid;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.MaildirFolderData;
import org.abstracthorizon.mercury.maildir.MaildirMessage;
import org.abstracthorizon.mercury.maildir.MaildirStore;

/**
 * UID implementation of maildir folder model.
 *
 * @author Daniel Sendula
 */
public class UIDMaildirFolderData extends MaildirFolderData /* implements UIDFolder TODO: nice to have */{


    /** Number of retries for .nextuid file to be read */
    public static final int MAX_RETRIES = 30;


    /** Current maxUID (last read) */
    protected long maxUid = -1;

    /** UID validity of this folder */
    protected long uidValidity = -1;

    /**
     * Constructor.
     * @param store store
     * @param file file
     */
    protected UIDMaildirFolderData(MaildirStore store, File file) {
        super(store, file);
    }

    /**
     * Returns max uid value
     * @return max uid value
     */
    public long getMaxUID() {
        return maxUid;
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
    protected synchronized MaildirMessage createNewMaildirMessage(MimeMessage message, int num) throws IOException, MessagingException {
        UIDMaildirMessage msg = new UIDMaildirMessage(this, message, num);
        return msg;
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
        UIDMaildirMessage msg = new UIDMaildirMessage(this, file, num);
        return msg;
    }


    /**
     * This method obtains next UID
     * @return next UID number
     * @throws MessagingException
     */
    protected synchronized UID getNextUID() throws MessagingException {
        RandomAccessFile uidFile = null;
        File file = new File(getFolderFile(), UIDMaildirStore.NEXT_UID_FILE);
        int retry = 0;
        while ((uidFile == null) && (retry < MAX_RETRIES)) {
            try {
                uidFile = new RandomAccessFile(file, "rw");
            } catch (IOException ignore) {
            }
            if (uidFile == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
                retry = retry + 1;
            }
        }
        try {
            String old = uidFile.readLine();
            long uid = 0;
            try {
                uid = Long.parseLong(old);
            } catch (NumberFormatException ignore) {
            }
            uid = uid + 1;
            uidFile.seek(0);
            uidFile.writeBytes(Long.toString(uid));
            uidFile.close();
            maxUid = uid;
            return new UID(uid);
        } catch (IOException e) {
            throw new MessagingException("Cannot obtain next uid", e);
        } finally {
            try {
                uidFile.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * This method returns folder's uid validity. It is read from &quot;.uidvalidity&quot;
     * @return uid validity
     */
    public long getUIDValidity() {
        if (uidValidity != -1) {
            return uidValidity;
        }
        File file = new File(getFolderFile(), UIDMaildirStore.UID_VALIDITY_FILE);
        if (file.exists()) {
            try {
                RandomAccessFile uidFile = new RandomAccessFile(file, "r");
                try {
                    String line = uidFile.readLine();
                    try {
                        uidValidity = Long.parseLong(line);
                    } catch (NumberFormatException ignore) {
                    }
                } finally {
                    uidFile.close();
                }
            } catch (IOException ignore) {
            }
        }
        if (uidValidity == -1) {
            uidValidity = System.currentTimeMillis();
            try {
                RandomAccessFile uidFile = new RandomAccessFile(file, "rw");
                try {
                    uidFile.writeBytes(Long.toString(uidValidity));
                } finally {
                    uidFile.close();
                }
            } catch (IOException ignore) {
            }
        }
        return uidValidity;
    }

    /**
     * Returns folder's uid validity as integer - hash code
     * @return folder's uid validity
     */
    public int hashCode() {
        return (int)getUIDValidity();
    }
}
