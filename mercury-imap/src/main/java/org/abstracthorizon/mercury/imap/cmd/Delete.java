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
import javax.mail.MessagingException;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.NOCommandException;
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * Delete IMAP command
 *
 * @author Daniel Sendula
 */
public class Delete extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Delete(String mnemonic) {
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
        StringBuffer mailboxName = new StringBuffer();
        if (!session.getScanner().mailbox(mailboxName)) {
            throw new ParserException("<mailbox_name>");
        }
        checkEOL(session);

        Folder f = session.getStore().getFolder(mailboxName.toString());
        if (f.delete(false)) {
            sendOK(session);
        } else {
            throw new NOCommandException("Problem deleting folder; "+f.getName());
        }
    }
}
