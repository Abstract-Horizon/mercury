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
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.EhloCommand;

/**
 * Filter command that adds extra processing to EHLO command.
 *
 * @author Daniel Sendula
 */
public class FilterEhloCommand extends EhloCommand {

    /**
     * Constructor
     */
    public FilterEhloCommand() {
        super();
    }


    /**
     * Doesn't do anything new
     *
     * @param session SMTP session
     * @throws IOException
     */
    protected void processEHLO(SMTPSession session, String domain) throws IOException {
        //MailSessionData data = session.getMailSessionData();
        //FilterHandler handler = ((SMTPFilterCommandFactory)session.getCommandFactory()).getFilterHandler();
        //handler.start(data);
        //String response = handler.setSourceDomain(data);
        //if (Filter.POSITIVE_RESPONSE.equals(response)) {
        //    super.processEHLO(session, domain);
        //} else {
        //    SMTPResponse smtpResponse = new SMTPResponse(550, response);
        //    session.sendResponse(smtpResponse);
        //    resetSession();
        //}
        super.processEHLO(session, domain);
    }
}
