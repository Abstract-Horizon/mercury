/*
 * Copyright (c) 2004-2007 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.imap.util;

import org.abstracthorizon.mercury.imap.IMAPSession;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * An interface used in processing sequence of messages
 *
 * @author Daniel Sendula
 */
public interface MessageProcessor {

    /**
     * Method that processes mail
     * @param session imap session
     * @param m message
     * @throws IOException
     * @throws MessagingException
     */
    public void process(IMAPSession session, MimeMessage m) throws IOException, MessagingException;

}
