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
import javax.mail.MessagingException;
import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.imap.IMAPSession;
import org.abstracthorizon.mercury.imap.NOCommandException;
import org.abstracthorizon.mercury.imap.util.ParserException;
import org.abstracthorizon.mercury.imap.util.IMAPScanner;

/**
 * Login IMAP command
 *
 * @author Daniel Sendula
 */
public class Login extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic mnemonic
     */
    public Login(String mnemonic) {
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
        IMAPScanner scanner = session.getScanner();
        StringBuffer user = new StringBuffer();
        if (!scanner.astring(user)) {
            throw new ParserException("<username>");
        }
        if (!scanner.is_char(' ')) {
            throw new ParserException("<SP>");
        }
        StringBuffer pass = new StringBuffer();
        if (!scanner.astring(pass)) {
            throw new ParserException("<password>");
        }
        checkEOL(session);

        if (user.equals("")) {
            (session).unauthorise();
            return;
        }

        if (pass.equals("")) {
            (session).unauthorise();
            throw new NOCommandException("Missing password");
        }

        if (session.isInsecureAllowed() || session.isSecure()) {
            if (!(session).authorise(user.toString(), pass.toString())) {
                throw new NOCommandException("Wrong username/password combination");
            }
        } else {
            throw new NOCommandException("Insecure login is not allowed");
        }

        sendOK(session);
    }

}
