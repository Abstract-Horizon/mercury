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
 * QUIT command
 *
 * @author Daniel Sendula
 */
public class QuitCommand extends SMTPCommand {

    /**
     * Constructor
     */
    public QuitCommand() {
        super();
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
        connection.sendResponse(SMTPResponses.QUIT_RESPONSE);
        connection.close();
    }
}
