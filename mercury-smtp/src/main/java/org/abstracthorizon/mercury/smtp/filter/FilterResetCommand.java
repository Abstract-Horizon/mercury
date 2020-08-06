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

import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.command.ResetCommand;

/**
 * Reset command that invokes filters to finish mail session
 *
 * @author Daniel Sendula
 */
public class FilterResetCommand extends ResetCommand {

    /**
     * Constructor
     */
    public FilterResetCommand() {
    }

    /**
     * Resets the session. It is used from <code>ResetCommand</code> and from
     * <code>EhloCommand</code>
     */
    protected void resetSession(SMTPSession connection) {
        super.resetSession(connection);
        MailSessionData mailSessionData = connection.getMailSessionData();
        ((SMTPFilterCommandFactory)connection.getCommandFactory()).finish(mailSessionData);
    }

}
