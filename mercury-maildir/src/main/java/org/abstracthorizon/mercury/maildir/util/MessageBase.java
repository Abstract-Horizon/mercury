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
package org.abstracthorizon.mercury.maildir.util;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * This class extends mime message from javax.mail exposing <code>setMessageNumber</code> method.
 */
public class MessageBase extends MimeMessage {

    /**
     * Constructor
     * @param folder folder
     * @param msgnum message number
     * @throws MessagingException
     */
    protected MessageBase(Folder folder, int msgnum) throws MessagingException {
        super(folder, msgnum);
    }

    /**
     * Constructor
     * @param session session
     * @throws MessagingException
     */
    public MessageBase(Session session) throws MessagingException {
        super(session);
    }

    /**
     * Constructor
     * @param message message
     * @throws MessagingException
     */
    public MessageBase(MimeMessage message) throws MessagingException {
        super(message);
    }

    /**
     * Sets message number
     * @param num message number
     */
    public void setMessageNumber(int num) {
        super.setMessageNumber(num);
    }
}
