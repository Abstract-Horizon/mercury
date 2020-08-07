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
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * NOOP IMAP command
 *
 * @author Daniel Sendula
 */
public class NOOP extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public NOOP(String mnemonic) {
        super(mnemonic);
        unilateral = IMAPCommand.ALWAYS_SEND_UNILATERAL_DATA;
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

        // This should happen as normal part of execution in selected state
        //Folder f = session.getSelectedFolder();
        //try {
        //    if (f != null) {
        //        new RecentResponse(session, f).submit();
        //        new ExistsResponse(session, f).submit();
        //    }
        //} catch (MessagingException e) {
        //    // don't care
        //}

        sendOK(session);
   }
}
