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
package org.abstracthorizon.mercury.smtp.filter;

import java.io.IOException;

import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.MailCommand;
import org.abstracthorizon.mercury.smtp.util.Path;

/**
 * Mail command extension that invokes available filters
 *
 * @author Daniel Sendula
 */
public class FilterMailCommand extends MailCommand {

    /**
     * Constructor
     */
    public FilterMailCommand() {
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

        SMTPFilterCommandFactory factory = ((SMTPFilterCommandFactory)session.getCommandFactory());
        factory.start(data);
        String response = factory.setSourceDomain(data);
        if (!Filter.POSITIVE_RESPONSE.equals(response)) {
            SMTPResponse smtpResponse = new SMTPResponse(550, response);
            session.sendResponse(smtpResponse);
            if (session.getState() == SMTPSession.STATE_MAIL) {
                session.setState(SMTPSession.STATE_READY);
            }
        } else {
            response = factory.setSourceMailbox(session.getMailSessionData());
            if (Filter.POSITIVE_RESPONSE.equals(response)) {
                super.processMailFrom(session, path);
            } else {
                SMTPResponse smtpResponse = new SMTPResponse(550, response);
                session.sendResponse(smtpResponse);
            }
        }
    }

}
