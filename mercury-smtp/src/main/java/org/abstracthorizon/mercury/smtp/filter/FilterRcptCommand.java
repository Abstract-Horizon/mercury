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
package org.abstracthorizon.mercury.smtp.filter;

import java.io.IOException;

import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.RcptCommand;
import org.abstracthorizon.mercury.smtp.util.Path;

/**
 * RCPT command that invokes filters
 *
 * @author Daniel Sendula
 */
public class FilterRcptCommand extends RcptCommand {

    /**
     * Constructor
     */
    public FilterRcptCommand() {
    }


    protected void processPath(SMTPSession session, Path path) throws IOException {
        MailSessionData data = session.getMailSessionData();
        String response = ((SMTPFilterCommandFactory)session.getCommandFactory()).setDestinationMailbox(data, path);
        if (Filter.POSITIVE_RESPONSE.equals(response)) {
            session.sendResponse(SMTPResponses.OK_RESPONSE);
        } else {
            SMTPResponse smtpResponse = new SMTPResponse(550, response);
            session.sendResponse(smtpResponse);
        }
    }

}
