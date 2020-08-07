/*
 * Copyright (c) 2006-2020 Creative Sphere Limited.
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
package org.abstracthorizon.mercury.smtp.logging;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.support.logging.AccessLogConnectionHandler;
import org.abstracthorizon.mercury.smtp.SMTPSession;
import org.abstracthorizon.mercury.smtp.filter.MailSessionData;

import java.util.List;

/**
 * <p>
 * Utility class that adds new pattern codes to existing in {@link AccessLogConnectionHandler}
 * </p>
 * <ul>
 * <li><code>%A</code> - local IP address</li>
 * </ul>
 * <p>
 * Those are added through {@link SMTPSPAMPatternProcessor}
 * </p>
 * <p>
 * Also it sets default pattern to &quot;%y %S %R %h localhost SMTP - %r %b&quot;
 * </p>
 * @author Daniel Sendula
 */
public class SMTPSPAMAccessLogConnectionHandler extends AccessLogConnectionHandler {

    /**
     * <p>Adds lists of predefined processors to the lists of provider classes.</p>
     * <p>This method adds following:</p>
     * <ul>
     * <li>{@link SMTPSPAMPatternProcessor}</li>
     * </ul>
     * <p>Also it calls super method {@link AccessLogConnectionHandler#addPredefinedProcessors(List)}</p>
     *
     * @param providerClasses list of provider classes
     */
    protected void addPredefinedProcessors(List<String> providerClasses) {
        super.addPredefinedProcessors(providerClasses);
        if (!providerClasses.contains(SMTPSPAMPatternProcessor.class.getName())) {
            providerClasses.add(SMTPSPAMPatternProcessor.class.getName());
        }
    }


    /**
     * Returns default log pattern
     * @return default log pattern
     */
    protected String getDefaultLogPattern() {
        return "%y %S %R %h localhost SMTP - %r %b";
    }

    protected String createLogLine(Connection connection, long start) {
        SMTPSession smtpSession = (SMTPSession)connection.adapt(SMTPSession.class);
        MailSessionData data = smtpSession.getMailSessionData();
        if (data.getSourceMailbox() == null) {
            return null;
        }
        return super.createLogLine(connection, start);
    }
}
