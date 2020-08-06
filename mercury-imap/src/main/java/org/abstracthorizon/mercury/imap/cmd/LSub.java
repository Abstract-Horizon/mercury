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
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.response.LsubResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;

/**
 * LSUB IMAP command
 *
 * @author Daniel Sendula
 */
public class LSub extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public LSub(String mnemonic) {
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
        Store store = session.getStore();
        StringBuffer name = new StringBuffer();
        StringBuffer expr = new StringBuffer();
        IMAPScanner scanner = session.getScanner();


        if (!scanner.mailbox(name)) {
            throw new ParserException("<mailbox_name>");
        }
        if (!scanner.is_char(' ')) {
            throw new ParserException("<SP>");
        }
        if (!scanner.list_mailbox(expr)) {
            throw new ParserException("<expr>");
        }
        checkEOL(session);

        Folder root = store.getFolder(name.toString());

        Folder[] res = root.list(expr.toString());
        if (res.length > 0) {
            for (int i=0; i<res.length; i++) {
                new LsubResponse(session, res[i]).submit();
            }
        }
        sendOK(session);
    }
}
