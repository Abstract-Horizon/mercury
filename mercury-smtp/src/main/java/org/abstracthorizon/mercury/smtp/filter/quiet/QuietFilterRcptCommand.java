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
package org.abstracthorizon.mercury.smtp.filter.quiet;

import java.io.IOException;

import org.abstracthorizon.mercury.common.StorageManager;
import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.RcptCommand;
import org.abstracthorizon.mercury.smtp.filter.Filter;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;
import org.abstracthorizon.mercury.smtp.util.Path;

/**
 * RCPT commmand that calls filters but in case of a problem it does nothing.
 *
 * @author Daniel Sendula
 */
public class QuietFilterRcptCommand extends RcptCommand {

    /**
     * Constructor
     */
    public QuietFilterRcptCommand() {
    }


    protected void processPath(SMTPSession session, Path path) throws IOException {
        String response = ((SMTPQuietFilterCommandFactory)session.getCommandFactory()).setDestinationMailbox(session.getMailSessionData(), path);
        if (Filter.POSITIVE_RESPONSE.equals(response)) {
            session.sendResponse(SMTPResponses.OK_RESPONSE);
        } else {
            // MailSessionData data = session.getMailSessionData();
            // data.setAttribute("drop", "true");
            StorageManager manager = session.getConnectionHandler().getStorageManager();

            path.setLocalDomain(manager.hasDomain(path.getDomain()));
            if (!path.isLocalDomain()) {
                MailSessionData data = session.getMailSessionData();
                data.setAttribute("dropconnection", "true");
                SMTPResponse smtpResponse = new SMTPResponse(550, response);
                session.sendResponse(smtpResponse);
            } else {
                // session.sendResponse(SMTPResponses.OK_RESPONSE);
                SMTPResponse smtpResponse = new SMTPResponse(550, response);
                session.sendResponse(smtpResponse);
            }
        }
    }
}
