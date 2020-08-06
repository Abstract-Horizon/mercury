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

import org.abstracthorizon.mercury.common.command.CommandException;
import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPResponses;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.DataCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP DATA command.
 *
 * @author Daniel Sendula
 */
public class FilterDataCommand extends DataCommand {

    /** Logger */
    protected static final Logger logger = LoggerFactory.getLogger(FilterDataCommand.class);

    /**
     * Constructor
     */
    public FilterDataCommand() {
        super();
    }


    /**
     * Returns <code>true</code> in case it is ok with proceeding with the
     * reading input stream. This method is responsible of sending response back
     * to the client
     *
     * <br>
     * Method to be overriden for filtering purposes.
     *
     * @param session SMTP session
     * @return <code>true</code> in case it is ok with proceeding with the
     *         reading input stream.
     * @throws IOException
     */
    protected boolean precheck(SMTPSession session) throws CommandException, IOException {
        String response = ((SMTPFilterCommandFactory)session.getCommandFactory()).doPreLoadCheck(session.getMailSessionData());
        if (Filter.POSITIVE_RESPONSE.equals(response)) {
            session.sendResponse(SMTPResponses.START_DATA_RESPONSE);
            return true;
        } else {
            SMTPResponse smtpResponse = new SMTPResponse(550, response);
            session.sendResponse(smtpResponse);
            return false;
        }
    }

    protected boolean postcheck(SMTPSession connection) throws IOException {
        String response = ((SMTPFilterCommandFactory)connection.getCommandFactory()).doPostLoadCheck(connection.getMailSessionData());
        if (Filter.POSITIVE_RESPONSE.equals(response)) {
            // Message is going to be delivered - so we can reset inactivity counter
            connection.resetLastAccessed();
            return super.postcheck(connection);
        } else {
            SMTPResponse smtpResponse = new SMTPResponse(550, response);
            connection.sendResponse(smtpResponse);
            return false;
        }
    }

    protected void postProcessing(SMTPSession session, boolean hasSuccessfuls) throws IOException {
        super.postProcessing(session, hasSuccessfuls);
        MailSessionData mailSessionData = session.getMailSessionData();

        ((SMTPFilterCommandFactory)session.getCommandFactory()).finish(mailSessionData);
    }


}
