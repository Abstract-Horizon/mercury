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
import org.abstracthorizon.mercury.imap.response.ByeResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * Logout IMAP command
 *
 * @author Daniel Sendula
 */
public class Logout extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Logout(String mnemonic) {
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
    protected void execute(IMAPSession session) throws CommandException, ParserException, IOException {
        checkEOL(session);

        new ByeResponse(session).submit();
        try {
            sendOK(session);
        } catch (IOException e) {
            // we don't care if client exited earier!
        }
        session.close();
    }
}
