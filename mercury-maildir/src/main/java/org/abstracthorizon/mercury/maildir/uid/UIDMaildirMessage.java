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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.MaildirFolderData;
import org.abstracthorizon.mercury.maildir.MaildirMessage;


/**
 * <p>UID Maildir message representation.</p>
 * <p>Messages file name is defined in the following way:
 * <ul>
 * <li>time of creation in seconds</li>
 * <li>&quot;.&quot; (dot)</li>
 * <li>&quot;V&quot; followed with last 9 bits of folder's hash code as an integer</li>
 * <li>&quot;U&quot; message's uid</li>
 * <li>&quot;.&quot; (dot)</li>
 * <li>host name</li>
 * <li>optional &quot;:&quot;, &quot;.&quot; or value from stores info separator attibute
 * followed by &quot;2,&quot; followed by flags</li>
 * </ul>
 * Flags are defined on the way explained in {@link org.abstracthorizon.mercury.maildir.FlagUtilities}
 * </p>
 * <p>Note: If file supplied in one of the constructors doesn't match this file name pattern file is then
 * renamed to match it. Also, if file name matches that pattern but folder's hash code is different
 * (folder's hash code represents UID validity) file is again renamed to proper one.
 * </p>
 *
 * @author Daniel Sendula
 */
public class UIDMaildirMessage extends MaildirMessage implements UIDMessage {

    /** Messages uid object */
    private UID uid;

    /**
     * Creates new message based on another message. It will create new file for this message in supplied folder.
     * @param folder folder this message belongs to
     * @param message message
     * @param msgnum message number
     * @throws MessagingException
     * @throws IOException
     */
    protected UIDMaildirMessage(MaildirFolderData folder, MimeMessage message, int msgnum) throws MessagingException, IOException {
        super(folder, message, msgnum);
    }

    /**
     * This constructor creates new message from supplied file. If supplied file doesn't
     * match appropriate format than it is renamed.
     * @param folder folder this message belongs to
     * @param file file
     * @param msgnum message number
     * @throws MessagingException
     */
    public UIDMaildirMessage(MaildirFolderData folder, File file, int msgnum) throws MessagingException {
        super(folder, file, msgnum, false);

        String name = file.getName();

        int i = name.indexOf('.');
        if (i > 0) {
            int j = name.indexOf('.', i + 1);
            if (name.charAt(i + 1) == 'V') {
                int k = name.indexOf('U', i + 1);
                if ((k > 0) && (k < j)) {
                    try {
                        /*int folderHash = */Integer.parseInt(name.substring(i + 2, k));
                        long uidx = Long.parseLong(name.substring(k + 1, j));
                        uid = new UID(uidx);
//                        if ((getFolder().hashCode() & 511) == folderHash) {
//                            uid = new UID(uidx);
//                        }
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }

        if (uid == null) {
            File oldFile = file;
            String filename = file.getName();
            String flags = null;

            int j = filename.lastIndexOf(FLAGS_SEPERATOR);
            if (j >= 0) {
                flags = filename.substring(j + 2);
            }

            this.file = new File(file.getParentFile(), createFileName(flags));
            long oldDate = oldFile.lastModified();
            if (!oldFile.renameTo(this.file)) {
                throw new MessagingException("Cannot rename " + oldFile.getAbsolutePath() + " to " + this.file.getAbsolutePath());
            } else {
                this.file.setLastModified(oldDate);
            }
        }
        initialise();
    }

    /**
     * Creates file name using supplied flags.
     * @param flags flags
     * @return file name for this message
     * @throws MessagingException
     */
    protected String createFileName(String flags) throws MessagingException {
        String time = Long.toString(System.currentTimeMillis());

        uid = ((UIDMaildirFolderData)folder).getNextUID();

         String folderHash = Integer.toString(getFolder().hashCode() & 511);

        baseName = time + ".V" + folderHash + 'U' + uid.getUID() + '.' + host;

        if ((flags == null) || (flags.length() == 0)) {
            return baseName;
        } else {
            return baseName + infoSeparator + FLAGS_SEPERATOR + flags;
        }
    }

    /**
     * Returns message's UID object.
     * @return message's UID object.
     */
    public UID getUID() {
        return uid;
    }

    /**
     * This object compares two messages based on UID value.
     * @param o message to be compared
     * @return <code>true</code> if both messages have same UID. <code>False</code> is returned if
     * UIDs are different or supplied object is not UID message
     */
    public boolean equals(Object o) {
        if (o instanceof UIDMaildirMessage) {
            long u1 = uid.getUID();
            long u2 = ((UIDMaildirMessage)o).getUID().getUID();
            if ((u1 == u2) && (getFolder().getFullName().equals(((UIDMaildirMessage)o).getFolder().getFullName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method compares two messages' uids.
     * @param o message to be compared with
     * @return -1, 0, 1 depending if this message has less, equal or greater UID then supplied.
     * If supplied object is not UID message then -1 is returned.
     */
    public int compareTo(MaildirMessage o) {
        if (o instanceof UIDMaildirMessage) {
            long u1 = uid.getUID();
            long u2 = ((UIDMaildirMessage)o).getUID().getUID();
            if (u1 == u2) {
                return 0;
            } else if (u1 < u2) {
                return -1;
            } else {
                return 1;
            }
        }
        return -1;
    }
}
