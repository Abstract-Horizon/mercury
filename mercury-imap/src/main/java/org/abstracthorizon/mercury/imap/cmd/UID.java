/*
 * Copyright (c) 2004-2006 Creative Sphere Limited.
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
import org.abstracthorizon.mercury.imap.response.NOResponse;
import org.abstracthorizon.mercury.imap.response.Response;
import org.abstracthorizon.mercury.imap.util.ParserException;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;

/**
 * UID prefix of IMAP command. It is treated as a command - but it is actually
 * a wrapper to commands that follow UID
 *
 * @author Daniel Sendula
 */
public class UID extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic menmonic
     */
    public UID(String mnemonic) {
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
        IMAPScanner scanner = session.getScanner();
        if (scanner.keyword("COPY")) {
            session.getParent().invokeCommand(session, "COPY", true);
        } else if (scanner.keyword("FETCH")) {
            session.getParent().invokeCommand(session, "FETCH", true);
        } else if (scanner.keyword("SEARCH")) {
            session.getParent().invokeCommand(session, "SEARCH", true);
        } else if (scanner.keyword("STORE")) {
            session.getParent().invokeCommand(session, "STORE", true);
        } else {
            new NOResponse(session, Response.UNTAGGED_RESPONSE, "Error in UID command").submit();
        }
    }
}
