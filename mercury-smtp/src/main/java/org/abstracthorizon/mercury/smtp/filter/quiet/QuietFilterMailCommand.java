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
package org.abstracthorizon.mercury.smtp.filter.quiet;

import java.io.IOException;

import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.MailCommand;
import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.util.Path;

/**
 * MAIL commmand that calls filters but in case of a problem it does nothing.
 *
 * @author Daniel Sendula
 */
public class QuietFilterMailCommand extends MailCommand {

    /**
     * Constructor
     */
    public QuietFilterMailCommand() {
    }

    /**
     * Sets from path to the session. This method can test if and make an
     * different action (sending different response).
     *
     * @param path path to be stored in the session
     * @param session SMTP session
     * @throws IOException
     */
    protected void processMailFrom(SMTPSession session, Path path) throws IOException {
        MailSessionData data = session.getMailSessionData();

        SMTPQuietFilterCommandFactory factory = ((SMTPQuietFilterCommandFactory)session.getCommandFactory());
        factory.start(data);
        String response = factory.setSourceDomain(data);
        if (!Filter.POSITIVE_RESPONSE.equals(response)) {
            session.getMailSessionData().setAttribute("drop", "true");
        }

        response = factory.setSourceMailbox(session.getMailSessionData());
        if (Filter.POSITIVE_RESPONSE.equals(response)) {
            super.processMailFrom(session, path);
        } else {
            session.getMailSessionData().setAttribute("drop", "true");
            super.processMailFrom(session, path);
        }
    }
}
