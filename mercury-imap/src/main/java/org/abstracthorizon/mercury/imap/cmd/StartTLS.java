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
package org.abstracthorizon.mercury.imap.cmd;

import java.io.IOException;

import javax.mail.MessagingException;

import org.abstracthorizon.mercury.common.command.CommandException;

import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.response.OKResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * Login IMAP command
 *
 * @author Daniel Sendula
 */
public class StartTLS extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public StartTLS(String mnemonic) {
        super(mnemonic);
    }

    /**
     * Executes the command
     * @param session
     * @throws ParserException
     * @throws MessagingException
     * @throws CommandException
     * @throws IOException
     */
    protected void execute(IMAPSession session) throws ParserException, MessagingException, CommandException, IOException {
        new OKResponse(session, "Begin TLS negotiation now").submit();
        session.getScanner().skip_line();
        session.switchToTLS();
        session.setCleanInput(true);
    }
}
