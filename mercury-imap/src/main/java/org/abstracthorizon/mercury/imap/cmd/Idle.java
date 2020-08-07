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
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.response.ContinuationResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;


/**
 * Check IMAP Command
 *
 * @author Daniel Sendula
 */
public class Idle extends NOOP {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Idle(String mnemonic) {
        super(mnemonic);
        unilateral = IMAPCommand.SEND_WHEN_HAVE_NEW;
    }

    /**
     * Executes the command
     * @param session
     * @throws ParserException
     * @throws MessagingException
     * @throws CommandException
     * @throws IOException
     */
    protected void execute(IMAPSession session) throws CommandException, ParserException, IOException {
        checkEOL(session);
        try {

            session.setIdling(true);
            new ContinuationResponse(session, "Idling").submit();

            session.getScanner().keyword("DONE");
            checkEOL(session);
            sendOK(session);
        } finally {
            session.setIdling(false);
        }
   }
}
