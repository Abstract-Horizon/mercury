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
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import org.abstracthorizon.mercury.imap.BADCommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.response.ExpungeResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * Expunge IMAP command
 *
 * @author Daniel Sendula
 */
public class Expunge extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Expunge(String mnemonic) {
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
    protected void execute(IMAPSession session) throws ParserException, MessagingException, IOException, BADCommandException {
        Folder f = session.getSelectedFolder();
        if (f != null) {
            Message[] msg = f.expunge();
            new ExpungeResponse(session, msg.length).submit();
            sendOK(session);
        } else {
            throw new BADCommandException("Folder must be selected.");
        }
    }

}
