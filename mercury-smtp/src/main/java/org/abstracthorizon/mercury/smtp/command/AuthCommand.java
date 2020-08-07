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
package org.abstracthorizon.mercury.smtp.command;

import java.io.IOException;

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPScanner;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.exception.ParserException;
import org.abstracthorizon.mercury.smtp.util.Base64;

/**
 * EHLO command
 *
 * @author Daniel Sendula
 */
public class AuthCommand extends ResetCommand {

    /**
     * Constructor
     */
    public AuthCommand() {
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

        if (!scanner.keyword("PLAIN")) { 
            scanner.skip_line();
            connection.sendResponse(SMTPResponses.AUTHENTICATION_CREDENTIALS_INVALID);
        }

        StringBuffer initialResponse = new StringBuffer();
        if (!scanner.base64(initialResponse)) {
            scanner.skip_line();
            connection.sendResponse(SMTPResponses.AUTHENTICATION_CREDENTIALS_INVALID);
        } else {
            String s = Base64.decode(initialResponse.toString());

            int i1 = s.indexOf('\0');
            int i2 = s.indexOf('\0', i1+1);

            String username = s.substring(i1+1, i2);
            String password = s.substring(i2+1);

            processAUTH(connection, username, password);
        }
        

    }

    /**
     * Processes EHLO command
     *
     * @param connection SMTP session
     * @throws IOException
     */
    protected void processAUTH(SMTPSession connection, String username, String password) throws IOException {
        resetSession(connection);
        connection.setState(SMTPSession.STATE_READY);
//        SMTPResponse ehloResponse = new SMTPResponse(SMTPSession., connection.getConnectionHandler().getStorageManager().getMainDomain());
        SMTPResponse authResponse = SMTPResponses.AUTHENTICATION_SUCCEEDED;
        connection.sendResponse(authResponse);
    }

}
