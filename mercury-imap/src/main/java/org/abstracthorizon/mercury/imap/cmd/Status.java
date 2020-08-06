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
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.NOCommandException;
import org.abstracthorizon.mercury.imap.response.StatusResponse;
import org.abstracthorizon.mercury.imap.util.ParserException;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;

/**
 * Status IMAP command
 *
 * @author Daniel Sendula
 */
public class Status extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic menmonic
     */
    public Status(String mnemonic) {
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
        StringBuffer mailbox = new StringBuffer();
        IMAPScanner scanner = session.getScanner();
        if (!scanner.mailbox(mailbox)) {
            throw new ParserException("<mailbox_name>");
        }
        if (!scanner.is_char(' ')) {
            throw new ParserException("<SP>");
        }

        Folder folder = session.getStore().getFolder(mailbox.toString());
        if ((folder == null) || !folder.exists()) {
            throw new NOCommandException("Folder doesn't exists; "+mailbox);
        } else {
            folder.open(Folder.READ_ONLY);
            try {
                StatusResponse response = new StatusResponse(session);
                response.append(' ').append(mailbox).append(" (");

                if (!scanner.is_char('(')) {
                    throw new ParserException("'('");
                }

                if (!status_att(scanner, response, folder)) {
                    throw new ParserException("<status_att>");
                }
                while (scanner.is_char(' ')) {
                    response.append(' ');
                    if (!status_att(scanner, response, folder)) {
                        throw new ParserException("<status_att>");
                    }
                } // while
                if (!scanner.is_char(')')) {
                    throw new ParserException("')'");
                }

                response.append(')');
                checkEOL(session);

                response.submit();
                sendOK(session);
            } finally {
                folder.close(false);
            }
        }
    }

    /**
     * This method scans for status attributes
     * @param scanner scanner
     * @param response response
     * @param folder folder
     * @return <code>true</code> if successful
     * @throws IOException
     * @throws ParserException
     * @throws MessagingException
     */
    protected boolean status_att(IMAPScanner scanner, StatusResponse response, Folder folder) throws IOException, ParserException, MessagingException {
        if (scanner.keyword("MESSAGES")) {
            response.append("MESSAGES ").append(folder.getMessageCount());
            return true;
        } else if (scanner.keyword("RECENT")) {
            response.append("RECENT ").append(folder.getNewMessageCount());
            return true;
        } else if (scanner.keyword("UIDNEXT")) {
            int newUID = 0;
            int msgCount = folder.getMessageCount();
            if ((msgCount > 0) && (folder instanceof UIDFolder)) {
                UIDFolder ff = (UIDFolder) folder;
                Message m = folder.getMessage(msgCount);
                newUID = (int) (ff.getUID(m) + 1);
            } else {
                System.err.println("MSG Count " + msgCount);
            }
            response.append("UIDNEXT ").append(newUID);
            return true;
        } else if (scanner.keyword("UIDVALIDITY")) {
            long uidValidity = 0;
            if (folder instanceof UIDFolder) {
                uidValidity = ((UIDFolder) folder).getUIDValidity();
            }
            response.append("UIDVALIDITY ").append(uidValidity);
            return true;
        } else if (scanner.keyword("UNSEEN")) {
            response.append("UNSEEN ").append(folder.getUnreadMessageCount());
            return true;
        }
        return false;
    }
}
