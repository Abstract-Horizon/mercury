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
import org.abstracthorizon.mercury.imap.response.ContinuationResponse;
import org.abstracthorizon.mercury.imap.response.NOResponse;
import org.abstracthorizon.mercury.imap.util.Base64;
import org.abstracthorizon.mercury.imap.util.ParserException;

/**
 * Authenticate IMAP command
 *
 * @author Daniel Sendula
 */
public class Authenticate extends IMAPCommand {

    /**
     * Constructor
     * @param mnemonic
     */
    public Authenticate(String mnemonic) {
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
    protected void execute(IMAPSession session) throws MessagingException, ParserException, CommandException, IOException {
        StringBuffer type = new StringBuffer();
        session.getScanner().atom(type);
        checkEOL(session);
        if ("PLAIN".equalsIgnoreCase(type.toString())) {
            new ContinuationResponse(session, "Please enter your password:").submit();
            StringBuffer line = new StringBuffer();
            session.getScanner().readBase64Line(line);

            String s = Base64.decode(line.toString());

            int i1 = s.indexOf('\0');
            int i2 = s.indexOf('\0', i1+1);

            String username = s.substring(i1+1, i2);
            String password = s.substring(i2+1);


            if (session.isInsecureAllowed() || session.isSecure()) {
                if (!(session).authorise(username, password)) {
                    throw new NOCommandException("Wrong username/password combination");
                }
            } else {
                throw new NOCommandException("Insecure login is not allowed");
            }

            sendOK(session);
        } else if ("LOGIN".equalsIgnoreCase(type.toString())) {
            new ContinuationResponse(session, "").submit();


        } else {
            new NOResponse(session, getMnemonic()+" unimplemented").submit();
        }
    }
}
