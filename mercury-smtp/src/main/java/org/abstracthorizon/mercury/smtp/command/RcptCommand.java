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
package org.abstracthorizon.mercury.smtp.command;

import java.io.IOException;

import javax.mail.MessagingException;

import org.abstracthorizon.mercury.common.StorageManager;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.common.exception.UnknownUserException;
import org.abstracthorizon.mercury.common.exception.UserRejectedException;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPScanner;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.exception.ParserException;
import org.abstracthorizon.mercury.smtp.util.Path;

/**
 * RCPT command
 *
 * @author Daniel Sendula
 */
public class RcptCommand extends SMTPCommand {

    /**
     * Constructor
     */
    public RcptCommand() {
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    protected void execute(SMTPSession connection) throws CommandException, IOException, ParserException {
        if (connection.getState() != SMTPSession.STATE_MAIL) {
            connection.sendResponse(SMTPResponses.BAD_SEQUENCE_OF_COMMANDS_RESPONSE);
            return;
        }

        SMTPScanner scanner = connection.getScanner();
        if (!scanner.is_char(' ')) { throw new ParserException("space"); }
        if (!scanner.keyword("TO:")) { throw new ParserException("MAIL:"); }
        if (scanner.is_char(' ')) {
            // Allow for incorrect clients that add space here..
        }
        Path path = new Path();
        if (scanner.keyword("<Postmaster>")) {
            path.setMailbox("postmaster");
        } else if (scanner.path(path)) {
        } else {
            throw new ParserException("reverse path");
        }
        readExtraParameters(connection, scanner);

        processPath(connection, path);
    }

    /**
     * Obtains extra parameters.
     *
     * @param session SMTP session
     * @param scanner STMP scanner
     * @throws IOException io exception
     * @throws ParserException parsing exception
     * @throws CommandException command exception
     */
    protected void readExtraParameters(SMTPSession connection, SMTPScanner scanner) throws IOException, ParserException, CommandException {
        scanner.skip_line();
    }

    /**
     * Processes path
     * @param connection smtp session
     * @param path path
     * @throws IOException
     */
    protected void processPath(SMTPSession connection, Path path) throws IOException {
        StorageManager manager = connection.getConnectionHandler().getStorageManager();

        path.setLocalDomain(manager.hasDomain(path.getDomain()));
        if (path.isLocalDomain()) {
            try {
                // Store store = manager.getLocalMailbox(path.getMailbox(), path.getDomain());
                // path.setStore(store);
                path.setFolder(manager.findInbox(path.getMailbox(), path.getDomain(), null));
                connection.getMailSessionData().getDestinationMailboxes().add(path);
                connection.sendResponse(SMTPResponses.OK_RESPONSE);
            } catch (MessagingException e) {
                connection.sendResponse(SMTPResponses.GENERIC_ERROR_RESPONSE);
            } catch (UnknownUserException e) {
                connection.sendResponse(SMTPResponses.MAILBOX_UNAVAILABLE_RESPONSE);
            } catch (UserRejectedException e) {
                connection.sendResponse(SMTPResponses.MAILBOX_UNAVAILABLE_RESPONSE);
            }
        } else {
            connection.getMailSessionData().getDestinationMailboxes().add(path);
            connection.sendResponse(SMTPResponses.MAILBOX_UNAVAILABLE_RESPONSE);
        }
    }

}
