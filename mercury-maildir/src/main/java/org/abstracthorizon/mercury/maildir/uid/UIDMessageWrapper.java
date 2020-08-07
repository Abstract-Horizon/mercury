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
package org.abstracthorizon.mercury.maildir.uid;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.maildir.util.MessageWrapper;

/**
 * Message wrapper for UID messages created by
 * {@link org.abstracthorizon.mercury.maildir.uid.UIDMaildirFolderData}.
 *
 * @author Daniel Sendula
 */
public class UIDMessageWrapper extends /* ReadOnlyMessageWrapper */ MessageWrapper implements UIDMessage, Comparable<UIDMessage> {

    /**
     * Constructor.
     * @param folder folder
     * @param message message this message is based on
     * @param msgnum message number
     * @throws MessagingException
     */
    protected UIDMessageWrapper(Folder folder, UIDMaildirMessage message, int msgnum) throws MessagingException {
        super(folder, message, msgnum);
    }

    /**
     * Returns wrapped message
     * @return wrapped message
     */
    public MimeMessage getMessage() {
        return message;
    }

    /**
     * Returns supplied message's UID.
     * @return supplied message's UID.
     * @throws MessagingException never
     * @throws MessagingException if supplied message's getUID method throws it
     * @throws IllegalStateException if supplied message is <code>null</code> or not <code>UIDMessage</code>
     */
    public UID getUID() throws MessagingException {
        if (message == null) {
            throw new IllegalStateException("Supplied message is empty");
        }
        if (message instanceof UIDMaildirMessage) {
            return ((UIDMaildirMessage)message).getUID();
        }
        throw new IllegalStateException("Supplied message is not UIDMessage");
    }


    /**
     * Checks supplied message's uid against wrapped message's uid
     * @param o message to be compared with
     * @return <code>true</code> if two messages have same uid and belong to folder with same full names
     */
    public boolean equals(Object o) {
        if (o instanceof UIDMessageWrapper) {
            try {
                long u1 = getUID().getUID();
                long u2 = ((UIDMessageWrapper)o).getUID().getUID();
                if ((u1 == u2) && (getFolder().getFullName().equals(((UIDMessageWrapper)o).getFolder().getFullName()))) {
                    return true;
                }
            } catch (MessagingException ignore) {
            }
        }
        return false;
    }

    /**
     * Compares wrapped message's uid with supplied message's uid.
     *
     * @param o message to be compared with
     * @return -1, 0 or 1. If supplied object is not of <code>UIDMessage</code> type it will return -1.
     */
    public int compareTo(UIDMessage o) {
        if (o instanceof UIDMessage) {
            try {
                long u1 = getUID().getUID();
                long u2 = o.getUID().getUID();
                if (u1 == u2) {
                    return 0;
                } else if (u1 < u2) {
                    return -1;
                } else {
                    return 1;
                }
            } catch (MessagingException ignore) {
            }
        }
        return -1;
    }
}
