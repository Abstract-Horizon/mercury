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
package org.abstracthorizon.mercury.smtp.send;

import java.io.IOException;

import org.abstracthorizon.mercury.smtp.SMTPResponse;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.EhloCommand;

/**
 * Filter command that adds extra processing to EHLO command.
 *
 * @author Daniel Sendula
 */
public class SendEhloCommand extends EhloCommand {

    /**
     * Constructor
     */
    public SendEhloCommand() {
        super();
    }


    /**
     * Doesn't do anything new
     *
     * @param session SMTP session
     * @throws IOException
     */
    protected void processEHLO(SMTPSession session, String domain) throws IOException {
        session.setState(SMTPSession.STATE_AUTH_NEEDED);
        SMTPResponse ehloResponse = new SMTPResponse(250, session.getConnectionHandler().getStorageManager().getMainDomain());
        ehloResponse.addLine("AUTH PLAIN");
        session.sendResponse(ehloResponse);
    }
}
