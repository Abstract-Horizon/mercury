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

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPScanner;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.exception.ParserException;

/**
 * EHLO command
 *
 * @author Daniel Sendula
 */
public class EhloCommand extends ResetCommand {

    /**
     * Constructor
     */
    public EhloCommand() {
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    protected void execute(SMTPSession connection) throws CommandException, IOException, ParserException {
//        if (connection.getState() != SMTPSession.STATE_READY) {
//            connection.sendResponse(SMTPResponses.BAD_SEQUENCE_OF_COMMANDS_RESPONSE);
//            return;
//        }

        SMTPScanner scanner = connection.getScanner();
        if (!scanner.is_char(' ')) { throw new ParserException("space"); }

        
        boolean bracked = scanner.is_char('[');
        StringBuffer domain = new StringBuffer();
        if (!scanner.domain(domain)) { throw new ParserException("domain"); }
        if (bracked) {
            if (!scanner.is_char(']')) { throw new ParserException("]"); }
        }

        readExtraParameters(connection, scanner);

        String domainStr = domain.toString();
        connection.getMailSessionData().setSourceDomain(domainStr);
        processEHLO(connection, domainStr);
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
        scanner.check_eol();
    }

    /**
     * Processes EHLO command
     *
     * @param connection SMTP session
     * @throws IOException
     */
    protected void processEHLO(SMTPSession connection, String domain) throws IOException {
        resetSession(connection);
        connection.setState(SMTPSession.STATE_READY);
        SMTPResponse ehloResponse = new SMTPResponse(250, connection.getConnectionHandler().getStorageManager().getMainDomain());
        connection.sendResponse(ehloResponse);
    }

}
