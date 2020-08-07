/*
 * Copyright (c) 2004-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.imap.cmd;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.util.MessageProcessor;

/**
 * A super class for all commands that can be invoked by UID prefix
 * (Copy, Fetch, Search and Store)
 *
 * @author Daniel Sendula
 */

public abstract class UIDCommand extends IMAPCommand implements MessageProcessor {

    /** Run as UID command */
    protected boolean asuid = false;

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public UIDCommand(String mnemonic) {
        super(mnemonic);
    }

    /**
     * Marks to run command as UID command
     */
    public void setAsUID() {
        asuid = true;
    }

    /**
     * Template method to be implementd to process each individial message
     * @param session session
     * @param m message
     * @throws IOException
     * @throws MessagingException
     */
    public abstract void process(IMAPSession session, MimeMessage m) throws IOException, MessagingException;

}
