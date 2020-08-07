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
import org.abstracthorizon.mercury.imap.util.IMAPScanner;

/**
 * Rename IMAP command
 *
 * @author Daniel Sendula
 */
public class Rename extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Rename(String mnemonic) {
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
        IMAPScanner scanner = session.getScanner();
        if (!scanner.mailbox(mailboxName)) {
            throw new ParserException("<mailbox_name>");
        }
        if (scanner.is_char(' ')) {
            throw new ParserException("<SP>");
        }
        StringBuffer newMailboxName = new StringBuffer();
        if (!scanner.mailbox(newMailboxName)) {
            throw new ParserException("<mailbox_name>");
        }
        checkEOL(session);

        Folder from = session.getStore().getFolder(mailboxName.toString());
        Folder to = session.getStore().getFolder(newMailboxName.toString());
        if (from.renameTo(to)) {
            sendOK(session);
        } else {
            throw new NOCommandException("Problem renaming folder; "+from.getName()+" to "+to.getName());
        }
    }
}
