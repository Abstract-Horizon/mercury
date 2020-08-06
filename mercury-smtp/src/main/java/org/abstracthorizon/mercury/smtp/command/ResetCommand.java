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
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.exception.ParserException;

/**
 * RSET (reset) command
 *
 * @author Daniel Sendula
 */
public class ResetCommand extends SMTPCommand {

    /**
     * Constructor
     */
    public ResetCommand() {
    }

    /**
     * Executed the command
     * @param connection smtp session
     * @throws CommandException
     * @throws IOException
     * @throws ParserException
     */
    protected void execute(SMTPSession connection) throws CommandException, IOException, ParserException {
        connection.getScanner().check_eol();
        resetSession(connection);
        connection.sendResponse(SMTPResponses.OK_RESPONSE);
    }

    /**
     * Resets the session. It is used from <code>ResetCommand</code> and from
     * <code>EhloCommand</code>
     */
    protected void resetSession(SMTPSession connection) {
//        if (connection.getState() == SMTPSession.STATE_MAIL) {
            connection.setState(SMTPSession.STATE_READY);
//        }
    }

}
