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
import org.abstracthorizon.mercury.smtp.command.EhloCommand;

/**
 * EHLO command
 *
 * @author Daniel Sendula
 */
public class QuietFilterEhloCommand extends EhloCommand {

    /**
     * Constructor
     */
    public QuietFilterEhloCommand() {
    }


    /**
     * Processes EHLO command
     *
     * @param session SMTP session
     * @throws IOException
     */
    protected void processEHLO(SMTPSession session, String domain) throws IOException {
        //MailSessionData data = session.getMailSessionData();
        //FilterHandler handler = ((SMTPQuietFilterCommandFactory)session.getCommandFactory()).getFilterHandler();
        //handler.start(data);
        //String response = handler.setSourceDomain(data);
        //if (Filter.POSITIVE_RESPONSE.equals(response)) {
        //    super.processEHLO(session, domain);
        //} else {
        //    data.setAttribute("drop", "true");
        //    super.processEHLO(session, domain);
        //}
        super.processEHLO(session, domain);
    }

}
