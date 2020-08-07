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

import org.abstracthorizon.mercury.smtp.filter.SMTPFilterCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP quiet drop spam filter command factory.
 *
 * @author Daniel Sendula
 */
public class SMTPQuietFilterCommandFactory extends SMTPFilterCommandFactory {

    protected static final Logger logger = LoggerFactory.getLogger(SMTPQuietFilterCommandFactory.class);

    /** Maximum flush speed in bytes per second. Default is 10240 ~ 10kb/s */
    protected int maxFlushSpeed = 10240;

    /**
     * Constructor
     */
    public SMTPQuietFilterCommandFactory() {
        super();
        commands.put(EHLO, new QuietFilterEhloCommand());
        commands.put(HELO, new QuietFilterEhloCommand());
        commands.put(MAIL, new QuietFilterMailCommand());
        commands.put(RCPT, new QuietFilterRcptCommand());
        commands.put(DATA, new QuietFilterDataCommand());
        commands.put(RSET, new QuietFilterResetCommand());
    }

    /**
     * Sets maximum flush speed in bytes per second
     * @param flushSpeed bytes per second
     */
    public void setMaxFlushSpeed(int flushSpeed) {
        this.maxFlushSpeed = flushSpeed ;
    }

    /**
     * Returns maximum flush speed in bytes per second
     * @return maximum flush speed in bytes per second
     */
    public int getMaxFlushSpeed() {
        return maxFlushSpeed;
    }
}
