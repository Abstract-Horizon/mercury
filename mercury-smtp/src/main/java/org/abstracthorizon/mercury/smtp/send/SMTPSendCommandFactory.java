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
package org.abstracthorizon.mercury.smtp.send;

import org.abstracthorizon.mercury.smtp.command.AuthCommand;
import org.abstracthorizon.mercury.smtp.filter.SMTPFilterCommandFactory;
import org.abstracthorizon.mercury.smtp.filter.quiet.QuietFilterResetCommand;
import org.abstracthorizon.mercury.smtp.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP quiet drop spam filter command factory.
 *
 * @author Daniel Sendula
 */
public class SMTPSendCommandFactory extends SMTPFilterCommandFactory {

    protected static final Logger logger = LoggerFactory.getLogger(SMTPSendCommandFactory.class);

    /** Maximum flush speed in bytes per second. Default is 10240 ~ 10kb/s */
    protected int maxFlushSpeed = 10240;

    protected Transport transport;

    /**
     * Constructor
     */
    public SMTPSendCommandFactory() {
        super();
        commands.put(EHLO, new SendEhloCommand());
        commands.put(HELO, new SendEhloCommand());
        commands.put(MAIL, new SendMailCommand());
        commands.put(RCPT, new SendRcptCommand());
        commands.put(DATA, new SendDataCommand());
        commands.put(RSET, new QuietFilterResetCommand());
        commands.put(AUTH, new AuthCommand());
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public void setMaxFlushSpeed(int maxFlushSpeed) {
        this.maxFlushSpeed = maxFlushSpeed;
    }

    public int getMaxFlushSpeed() {
        return maxFlushSpeed;
    }
}
