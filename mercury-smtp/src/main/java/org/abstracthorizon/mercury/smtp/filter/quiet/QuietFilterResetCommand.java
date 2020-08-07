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

import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.ResetCommand;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;

/**
 * RSET commmand that calls filters but in case of a problem it does nothing.
 *
 * @author Daniel Sendula
 */
public class QuietFilterResetCommand extends ResetCommand {

    /**
     * Constructor
     */
    public QuietFilterResetCommand() {
    }

    /**
     * Resets the session. It is used from <code>ResetCommand</code> and from
     * <code>EhloCommand</code>
     */
    protected void resetSession(SMTPSession connection) {
        super.resetSession(connection);
        MailSessionData mailSessionData = connection.getMailSessionData();
        ((SMTPQuietFilterCommandFactory)connection.getCommandFactory()).finish(mailSessionData);
    }
}
