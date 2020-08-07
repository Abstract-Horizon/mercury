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
import javax.mail.Store;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.NOCommandException;
import org.abstracthorizon.mercury.imap.response.BADResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * Create IMAP command
 *
 * @author Daniel Sendula
 */
public class Create extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Create(String mnemonic) {
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

        StringBuffer name = new StringBuffer();
        if (!session.getScanner().mailbox(name)) {
            new BADResponse(session, "Wrong mailbox name; "+name).submit();
            return;
        }
        checkEOL(session);

        Store store = session.getStore();
        Folder folder = store.getDefaultFolder();
        Folder newFolder = folder.getFolder(name.toString());
        if (newFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES)) {
            sendOK(session);
        } else {
            throw new NOCommandException("Cannot create folder "+name);
        }

    }
}
